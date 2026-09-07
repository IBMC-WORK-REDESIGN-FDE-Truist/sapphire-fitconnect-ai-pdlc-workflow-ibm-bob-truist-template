package com.sapphire.charting.temperature;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A single pre-aggregated data point in a temperature trend response.
 *
 * <p>All values are stored and returned in degrees Celsius. Client-side unit conversion
 * is performed by the UI using {@code temperatureUtils}.
 *
 * @param periodStart start date of the aggregation period
 * @param periodEnd   end date of the aggregation period
 * @param minCelsius  minimum temperature recorded during the period
 * @param maxCelsius  maximum temperature recorded during the period
 * @param avgCelsius  average temperature over the period
 * @param recordCount number of raw readings included in the aggregation
 */
public record TemperatureDataPoint(
    LocalDate periodStart,
    LocalDate periodEnd,
    BigDecimal minCelsius,
    BigDecimal maxCelsius,
    BigDecimal avgCelsius,
    int recordCount
) {}
