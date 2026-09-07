package com.sapphire.health.temperature;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * In-process, per-device rate limiter backed by a Caffeine cache.
 *
 * <p>Each entry in the cache tracks the number of ingestion requests received from a
 * given {@code deviceSourceId} within the current 60-second window. Once a device
 * exceeds {@value #LIMIT} accepted readings in the window, further requests are rejected
 * with HTTP 429 until the TTL expires.
 *
 * <p>The Caffeine {@code expireAfterWrite} policy automatically resets the counter after
 * 60 seconds of inactivity for that device, providing a sliding-window approximation.
 */
@Component
public class DeviceRateLimiter {

    /** Maximum number of accepted readings per device per 60-second window. */
    static final int LIMIT = 10;

    private final Cache<String, AtomicInteger> cache;

    public DeviceRateLimiter() {
        this.cache = Caffeine.newBuilder()
            .expireAfterWrite(60, TimeUnit.SECONDS)
            .maximumSize(10_000)
            .build();
    }

    /**
     * Checks whether the device is within its rate limit and, if so, increments the counter.
     *
     * <p>This method is thread-safe: the underlying {@link AtomicInteger} increment is atomic,
     * and Caffeine provides safe concurrent cache access.
     *
     * @param deviceId the {@code deviceSourceId} of the submitting device
     * @return {@code true} if the request is within the limit (counter incremented);
     *         {@code false} if the limit is already reached (counter not incremented)
     */
    public boolean checkAndIncrement(String deviceId) {
        AtomicInteger counter = cache.get(deviceId, k -> new AtomicInteger(0));
        int current = counter.get();
        if (current >= LIMIT) {
            return false;
        }
        // CAS loop to avoid over-counting under concurrent load
        while (true) {
            int witness = counter.compareAndExchange(current, current + 1);
            if (witness == current) {
                return true;
            }
            current = witness;
            if (current >= LIMIT) {
                return false;
            }
        }
    }
}
