package com.sapphire.charting.temperature;

import java.time.LocalDate;
import java.util.List;

/**
 * Response DTO returned by {@code GET /metrics/temperature/trend}.
 *
 * @param userId   the user whose trend data is returned
 * @param range    the requested aggregation range (DAY, WEEK, MONTH)
 * @param from     inclusive start date of the returned data window
 * @param to       inclusive end date of the returned data window
 * @param dataPoints ordered list of aggregated data points; empty list when no data exists
 */
public record TemperatureTrendResponse(
    String userId,
    String range,
    LocalDate from,
    LocalDate to,
    List<TemperatureDataPoint> dataPoints
) {}
