package com.sapphire.health.temperature;

/**
 * The source that submitted a temperature reading.
 *
 * <p>{@code DEVICE} readings arrive from a registered smart device via the
 * ingestion endpoint. {@code API} readings are submitted programmatically
 * by an external system (e.g., a health-import service).
 */
public enum IngestionSource {

    /** Reading originated from a registered patient-owned smart device. */
    DEVICE,

    /** Reading submitted via the REST API by an external system. */
    API
}
