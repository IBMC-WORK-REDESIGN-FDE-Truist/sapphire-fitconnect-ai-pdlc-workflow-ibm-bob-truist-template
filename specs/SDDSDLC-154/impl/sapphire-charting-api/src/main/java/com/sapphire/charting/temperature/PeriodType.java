package com.sapphire.charting.temperature;

/**
 * Aggregation period granularity for temperature rollup records.
 *
 * <p>Used in {@link TemperatureRollup} to distinguish daily, weekly, and monthly
 * pre-aggregated summaries. Each combination of {@code (user_id, period_type, period_start)}
 * is unique in the {@code temperature_rollups} table.
 */
public enum PeriodType {

    /** One calendar day (midnight-to-midnight, user's local timezone at storage time). */
    DAY,

    /** ISO week (Monday–Sunday). */
    WEEK,

    /** Calendar month (1st to last day of month). */
    MONTH
}
