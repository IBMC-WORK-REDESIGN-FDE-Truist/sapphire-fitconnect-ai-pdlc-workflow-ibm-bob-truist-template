package com.sapphire.health.temperature;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Inbound DTO for a batch of body temperature readings submitted in a single request.
 *
 * <p>Batch size is capped at 100 readings per request to prevent resource exhaustion.
 * Each individual reading is validated via {@link TemperatureReadingRequest}'s own
 * Bean Validation constraints.
 *
 * @param readings list of individual temperature readings; maximum 100 entries
 */
public record BatchIngestionRequest(

    @Size(min = 1, max = 100, message = "batch must contain between 1 and 100 readings")
    List<@Valid TemperatureReadingRequest> readings
) {}
