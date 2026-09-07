package com.sapphire.health.temperature;

/**
 * Per-reading error detail included in {@link IngestionResponse#errors()} when a reading is rejected.
 *
 * @param index   zero-based index of the rejected reading within the submitted batch (0 for single)
 * @param code    machine-readable rejection code (e.g. {@code OUT_OF_RANGE}, {@code RATE_LIMITED})
 * @param message human-readable description of why the reading was rejected
 */
public record IngestionError(
    int index,
    String code,
    String message
) {}
