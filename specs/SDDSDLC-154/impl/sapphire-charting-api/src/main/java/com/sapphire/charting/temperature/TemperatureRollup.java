package com.sapphire.charting.temperature;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Pre-aggregated body temperature rollup for a user over a specific time period.
 *
 * <p>Rollups are computed by {@link TemperatureRollupJob} every 15 minutes by reading
 * new {@code temperature_records} rows since the last watermark. Each rollup stores the
 * minimum, maximum, and average temperature in Celsius for a given period, alongside the
 * source record count used to compute the aggregates.
 *
 * <p>Unique constraint on {@code (user_id, period_type, period_start)} enables idempotent
 * upserts: re-running the rollup job for the same period updates rather than duplicates.
 */
@Entity
@Table(
    name = "temperature_rollups",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_temperature_rollups_period",
        columnNames = {"user_id", "period_type", "period_start"}
    )
)
public class TemperatureRollup {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "period_type", nullable = false, length = 10)
    private PeriodType periodType;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    @Column(name = "min_celsius", nullable = false, precision = 5, scale = 2)
    private BigDecimal minCelsius;

    @Column(name = "max_celsius", nullable = false, precision = 5, scale = 2)
    private BigDecimal maxCelsius;

    @Column(name = "avg_celsius", nullable = false, precision = 5, scale = 2)
    private BigDecimal avgCelsius;

    @Column(name = "record_count", nullable = false)
    private int recordCount;

    @Column(name = "computed_at", nullable = false)
    private OffsetDateTime computedAt;

    protected TemperatureRollup() {
        // JPA no-arg constructor
    }

    public TemperatureRollup(
            UUID userId,
            PeriodType periodType,
            LocalDate periodStart,
            LocalDate periodEnd,
            BigDecimal minCelsius,
            BigDecimal maxCelsius,
            BigDecimal avgCelsius,
            int recordCount,
            OffsetDateTime computedAt) {
        this.userId = userId;
        this.periodType = periodType;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.minCelsius = minCelsius;
        this.maxCelsius = maxCelsius;
        this.avgCelsius = avgCelsius;
        this.recordCount = recordCount;
        this.computedAt = computedAt;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public PeriodType getPeriodType() { return periodType; }
    public LocalDate getPeriodStart() { return periodStart; }
    public LocalDate getPeriodEnd() { return periodEnd; }
    public BigDecimal getMinCelsius() { return minCelsius; }
    public BigDecimal getMaxCelsius() { return maxCelsius; }
    public BigDecimal getAvgCelsius() { return avgCelsius; }
    public int getRecordCount() { return recordCount; }
    public OffsetDateTime getComputedAt() { return computedAt; }
}
