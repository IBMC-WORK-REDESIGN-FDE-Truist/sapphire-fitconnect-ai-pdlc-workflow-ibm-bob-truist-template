package com.sapphire.charting.temperature;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link TemperatureRollup} entities.
 *
 * <p>Provides standard CRUD operations plus an idempotent upsert that updates an
 * existing rollup row when the job re-processes the same period.
 */
@Repository
public interface TemperatureRollupRepository extends JpaRepository<TemperatureRollup, UUID> {

    /**
     * Returns all rollup rows for a user within the given period type and date range,
     * ordered by {@code period_start} ascending.
     *
     * @param userId      the user whose rollups to query
     * @param periodType  aggregation granularity (DAY, WEEK, MONTH)
     * @param from        inclusive start date
     * @param to          inclusive end date
     * @return ordered list of matching rollup rows; never null
     */
    @Query("SELECT r FROM TemperatureRollup r " +
           "WHERE r.userId = :userId " +
           "  AND r.periodType = :periodType " +
           "  AND r.periodStart >= :from " +
           "  AND r.periodEnd   <= :to " +
           "ORDER BY r.periodStart ASC")
    List<TemperatureRollup> findByUserAndPeriod(
        @Param("userId")     UUID userId,
        @Param("periodType") PeriodType periodType,
        @Param("from")       LocalDate from,
        @Param("to")         LocalDate to
    );

    /**
     * Idempotent upsert for a single rollup row. Inserts if the period does not exist;
     * updates all aggregate columns if it does.
     *
     * @param userId      user the rollup belongs to
     * @param periodType  aggregation granularity string
     * @param periodStart first day of the period
     * @param periodEnd   last day of the period
     * @param minCelsius  minimum temperature in Celsius over the period
     * @param maxCelsius  maximum temperature in Celsius over the period
     * @param avgCelsius  average temperature in Celsius over the period
     * @param recordCount number of raw records included in the aggregation
     * @param computedAt  timestamp when the rollup was last computed
     */
    @Modifying
    @Query(value = """
        INSERT INTO temperature_rollups
            (id, user_id, period_type, period_start, period_end,
             min_celsius, max_celsius, avg_celsius, record_count, computed_at)
        VALUES
            (gen_random_uuid(), :userId, :periodType, :periodStart, :periodEnd,
             :minCelsius, :maxCelsius, :avgCelsius, :recordCount, :computedAt)
        ON CONFLICT (user_id, period_type, period_start) DO UPDATE SET
            period_end   = EXCLUDED.period_end,
            min_celsius  = EXCLUDED.min_celsius,
            max_celsius  = EXCLUDED.max_celsius,
            avg_celsius  = EXCLUDED.avg_celsius,
            record_count = EXCLUDED.record_count,
            computed_at  = EXCLUDED.computed_at
        """,
        nativeQuery = true)
    void upsertRollup(
        @Param("userId")      UUID userId,
        @Param("periodType")  String periodType,
        @Param("periodStart") LocalDate periodStart,
        @Param("periodEnd")   LocalDate periodEnd,
        @Param("minCelsius")  BigDecimal minCelsius,
        @Param("maxCelsius")  BigDecimal maxCelsius,
        @Param("avgCelsius")  BigDecimal avgCelsius,
        @Param("recordCount") int recordCount,
        @Param("computedAt")  OffsetDateTime computedAt
    );
}
