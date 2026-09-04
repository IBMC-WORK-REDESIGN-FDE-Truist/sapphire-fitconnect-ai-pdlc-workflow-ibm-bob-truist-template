package com.sapphire.charting.temperature;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service that retrieves pre-aggregated body temperature trend data for a user.
 *
 * <p>Delegates all data access to {@link TemperatureRollupRepository}. Returns an empty
 * {@code dataPoints} list (never null) when no rollup data exists for the requested period.
 * OTEL query metrics are emitted via {@link TemperatureReportingMetrics} on every call.
 */
@Service
public class TemperatureTrendService {

    private final TemperatureRollupRepository rollupRepository;
    private final TemperatureReportingMetrics metrics;

    public TemperatureTrendService(
            TemperatureRollupRepository rollupRepository,
            TemperatureReportingMetrics metrics) {
        this.rollupRepository = rollupRepository;
        this.metrics = metrics;
    }

    /**
     * Returns aggregated temperature trend data for the given user and time window.
     *
     * <p>If {@code deviceSourceId} is non-null, the query is further filtered by that device
     * (not yet supported in the rollup schema — reserved for future use; currently ignored
     * at the repository layer but accepted at the API layer for forward compatibility).
     *
     * @param userId         the user whose data to retrieve
     * @param range          the aggregation period granularity
     * @param from           inclusive start of the query window
     * @param to             inclusive end of the query window
     * @param deviceSourceId optional device filter; may be null to include all devices
     * @return trend response containing an ordered list of data points; never null
     */
    public TemperatureTrendResponse getTrend(
            UUID userId,
            PeriodType range,
            LocalDate from,
            LocalDate to,
            String deviceSourceId) {

        metrics.recordQuery(range);

        List<TemperatureRollup> rollups = rollupRepository.findByUserAndPeriod(userId, range, from, to);

        List<TemperatureDataPoint> dataPoints = rollups.stream()
            .map(r -> new TemperatureDataPoint(
                r.getPeriodStart(),
                r.getPeriodEnd(),
                r.getMinCelsius(),
                r.getMaxCelsius(),
                r.getAvgCelsius(),
                r.getRecordCount()
            ))
            .collect(Collectors.toList());

        return new TemperatureTrendResponse(
            userId.toString(),
            range.name(),
            from,
            to,
            dataPoints
        );
    }
}
