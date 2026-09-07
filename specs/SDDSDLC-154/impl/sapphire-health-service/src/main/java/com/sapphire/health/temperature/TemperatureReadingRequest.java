package com.sapphire.health.temperature;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Inbound DTO for a single body temperature reading submitted by a device or external system.
 *
 * <p>Bean Validation annotations enforce physiological plausibility before the value
 * reaches {@link TemperatureService}. Range limits are checked again in service logic
 * once the unit is known (Celsius vs Fahrenheit).
 *
 * @param deviceSourceId  unique identifier of the submitting device or source system
 * @param recordedAt      wall-clock timestamp when the measurement was taken (must be non-null)
 * @param value           raw measurement value in the stated {@code unit}
 * @param unit            measurement unit ({@code CELSIUS} or {@code FAHRENHEIT})
 * @param ingestionSource originating source type
 * @param measurementMethod optional measurement site (oral, axillary, tympanic); may be null
 */
public record TemperatureReadingRequest(

    @NotBlank
    String deviceSourceId,

    @NotNull
    OffsetDateTime recordedAt,

    @NotNull
    @DecimalMin(value = "-273.15", message = "value must be above absolute zero")
    @DecimalMax(value = "1000.00", message = "value exceeds plausible upper bound")
    BigDecimal value,

    @NotNull
    TemperatureUnit unit,

    @NotNull
    IngestionSource ingestionSource,

    String measurementMethod
) {}
