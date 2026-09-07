package com.sapphire.health.temperature;

/**
 * Thrown when a temperature reading value falls outside the accepted physiological range.
 *
 * <p>Maps to HTTP 422 Unprocessable Entity in the controller advice.
 */
public class OutOfRangeException extends RuntimeException {
    public OutOfRangeException(String message) {
        super(message);
    }
}
