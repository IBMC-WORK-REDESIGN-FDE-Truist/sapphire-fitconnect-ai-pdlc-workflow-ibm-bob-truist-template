package com.sapphire.charting.temperature;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Raw temperature record as read from the health-service data source by the rollup job.
 *
 * <p>Values are pre-converted to Celsius before this record is populated so the rollup
 * job only deals with a single unit.
 *
 * @param userId       the user who owns the reading
 * @param recordedAt   when the measurement was taken
 * @param valueCelsius value in degrees Celsius (converted from original unit if needed)
 */
public record RawTemperatureRecord(
    UUID userId,
    OffsetDateTime recordedAt,
    BigDecimal valueCelsius
) {}
