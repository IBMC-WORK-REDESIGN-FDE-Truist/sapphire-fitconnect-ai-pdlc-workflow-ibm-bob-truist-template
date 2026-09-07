package com.sapphire.health.temperature;

/**
 * Thrown when a device has exceeded its per-minute ingestion rate limit.
 *
 * <p>Maps to HTTP 429 Too Many Requests in the controller advice.
 */
public class RateLimitExceededException extends RuntimeException {
    public RateLimitExceededException(String message) {
        super(message);
    }
}
