package com.sapphire.health.temperature;

/**
 * Catalog of supported health metric types in sapphire-health-service.
 *
 * <p>Add new metric types here to register them in the platform metric catalog.
 * Existing entries must not be removed or renamed — doing so is a breaking change.
 */
public enum MetricType {

    BLOOD_PRESSURE,
    SPO2,
    HEART_RATE,
    STEPS,

    /** Body temperature metric. Added by SDDSDLC-154. */
    BODY_TEMPERATURE
}
