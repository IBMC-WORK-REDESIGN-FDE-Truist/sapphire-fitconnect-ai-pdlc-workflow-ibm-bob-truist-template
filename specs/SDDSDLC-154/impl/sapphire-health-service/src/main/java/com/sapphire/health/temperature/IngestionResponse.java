package com.sapphire.health.temperature;

import java.util.List;

/**
 * Structured response returned after a single or batch ingestion request.
 *
 * <p>A single-reading request will always produce {@code accepted=1, rejected=0} on success
 * or {@code accepted=0, rejected=1} on failure. Batch requests may return partial success
 * (207 Multi-Status) when some readings are accepted and others are rejected.
 *
 * @param accepted number of readings successfully stored
 * @param rejected number of readings that were rejected (validation failure, rate limit, etc.)
 * @param errors   per-reading error details for each rejected reading; empty list on full success
 */
public record IngestionResponse(
    int accepted,
    int rejected,
    List<IngestionError> errors
) {

    /**
     * Convenience factory for a fully successful ingestion of {@code count} readings.
     *
     * @param count number of accepted readings
     * @return response with zero errors
     */
    public static IngestionResponse allAccepted(int count) {
        return new IngestionResponse(count, 0, List.of());
    }
}
