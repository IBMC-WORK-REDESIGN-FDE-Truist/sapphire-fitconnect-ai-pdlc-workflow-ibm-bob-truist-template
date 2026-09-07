package com.sapphire.health.temperature;

/**
 * Units of measurement for body temperature readings.
 *
 * <p>All ingested values are stored in the unit provided by the source device.
 * Conversion to the user's preferred display unit happens at the reporting layer.
 */
public enum TemperatureUnit {

    /** Degrees Celsius. Physiological range: 30.0–43.0 °C. */
    CELSIUS,

    /** Degrees Fahrenheit. Physiological range: 86.0–109.4 °F. */
    FAHRENHEIT
}
