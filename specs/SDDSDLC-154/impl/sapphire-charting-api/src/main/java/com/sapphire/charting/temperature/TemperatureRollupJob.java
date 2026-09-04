package com.sapphire.charting.temperature;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.UUID;

/**
 * Scheduled job that computes daily, weekly, and monthly temperature rollups.
 *
 * <p>Runs every 15 minutes. Reads {@code temperature_records} rows where
 * {@code recorded_at > watermark}, groups them by user and period, and upserts
 * the resulting rollup rows via {@link TemperatureRollupRepository}. The job is
 * idempotent: re-running it for the same window produces the same rollup values.
 *
 * <p>The watermark is stored in-memory for the current process lifecycle. For production
 * use, persist the watermark to a config table or distributed cache to survive restarts.
 */
@Component
public class TemperatureRollupJob {

    private static final Logger log = LoggerFactory.getLogger(TemperatureRollupJob.class);

    private final TemperatureRollupRepository rollupRepository;
    private final RawTemperatureQueryPort rawTemperaturePort;

    /** In-memory watermark; initialised to 7 days ago on startup. */
    private volatile OffsetDateTime watermark = OffsetDateTime.now().minusDays(7);

    public TemperatureRollupJob(
            TemperatureRollupRepository rollupRepository,
            RawTemperatureQueryPort rawTemperaturePort) {
        this.rollupRepository = rollupRepository;
        this.rawTemperaturePort = rawTemperaturePort;
    }

    /**
     * Executes the rollup computation on a 15-minute schedule.
     *
     * <p>Queries raw temperature records since the last watermark, computes DAY/WEEK/MONTH
     * aggregates per user, upserts rollup rows, and advances the watermark.
     */
    @Scheduled(cron = "0 */15 * * * *")
    @Transactional
    public void run() {
        OffsetDateTime since = this.watermark;
        OffsetDateTime now = OffsetDateTime.now();
        log.info("TemperatureRollupJob starting: watermark={}", since);

        List<RawTemperatureRecord> newRecords = rawTemperaturePort.findSince(since);
        if (newRecords.isEmpty()) {
            log.info("TemperatureRollupJob: no new records since {}", since);
            this.watermark = now;
            return;
        }

        // Group by user, then compute rollups for each period type
        newRecords.stream()
            .collect(java.util.stream.Collectors.groupingBy(RawTemperatureRecord::userId))
            .forEach((userId, records) -> {
                for (PeriodType periodType : PeriodType.values()) {
                    computeAndUpsertRollups(userId, periodType, records);
                }
            });

        this.watermark = now;
        log.info("TemperatureRollupJob complete: processed {} records", newRecords.size());
    }

    /**
     * Computes rollup aggregates for the given user, period type, and raw records,
     * then upserts each resulting bucket via the repository.
     */
    private void computeAndUpsertRollups(
            UUID userId, PeriodType periodType, List<RawTemperatureRecord> records) {

        records.stream()
            .collect(java.util.stream.Collectors.groupingBy(
                r -> bucketKey(r.recordedAt().toLocalDate(), periodType)
            ))
            .forEach((bucket, bucketRecords) -> {
                BigDecimal min = bucketRecords.stream()
                    .map(RawTemperatureRecord::valueCelsius)
                    .min(BigDecimal::compareTo)
                    .orElse(BigDecimal.ZERO);
                BigDecimal max = bucketRecords.stream()
                    .map(RawTemperatureRecord::valueCelsius)
                    .max(BigDecimal::compareTo)
                    .orElse(BigDecimal.ZERO);
                BigDecimal sum = bucketRecords.stream()
                    .map(RawTemperatureRecord::valueCelsius)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal avg = sum.divide(
                    BigDecimal.valueOf(bucketRecords.size()), 2, RoundingMode.HALF_UP);

                LocalDate[] range = periodBounds(bucket, periodType);

                rollupRepository.upsertRollup(
                    userId,
                    periodType.name(),
                    range[0],
                    range[1],
                    min,
                    max,
                    avg,
                    bucketRecords.size(),
                    OffsetDateTime.now()
                );
            });
    }

    /** Returns the canonical first-day-of-period for the given date. */
    private LocalDate bucketKey(LocalDate date, PeriodType periodType) {
        return switch (periodType) {
            case DAY   -> date;
            case WEEK  -> date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            case MONTH -> date.with(TemporalAdjusters.firstDayOfMonth());
        };
    }

    /** Returns [periodStart, periodEnd] for the given bucket start and period type. */
    private LocalDate[] periodBounds(LocalDate bucketStart, PeriodType periodType) {
        LocalDate end = switch (periodType) {
            case DAY   -> bucketStart;
            case WEEK  -> bucketStart.plusDays(6);
            case MONTH -> bucketStart.with(TemporalAdjusters.lastDayOfMonth());
        };
        return new LocalDate[]{ bucketStart, end };
    }
}
