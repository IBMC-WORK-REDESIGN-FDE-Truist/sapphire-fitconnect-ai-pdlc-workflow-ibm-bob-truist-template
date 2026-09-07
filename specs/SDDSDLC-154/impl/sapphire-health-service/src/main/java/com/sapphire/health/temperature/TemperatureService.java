package com.sapphire.health.temperature;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Core business logic for body temperature ingestion.
 *
 * <p>Enforces physiological range validation, per-device rate limiting, and idempotent
 * deduplication before delegating persistence to {@link TemperatureRepository}. OTEL
 * counters are emitted via {@link TemperatureMetrics} on every outcome.
 *
 * <p>Validation order per reading:
 * <ol>
 *   <li>Rate limit check — reject with 429 code if device has exceeded 10 req/min</li>
 *   <li>Physiological range check — reject with 422 code for out-of-range values</li>
 *   <li>Repository upsert — accepted or silently deduplicated</li>
 * </ol>
 */
@Service
public class TemperatureService {

    /** Minimum accepted temperature in degrees Celsius. */
    private static final BigDecimal MIN_CELSIUS = new BigDecimal("30.0");

    /** Maximum accepted temperature in degrees Celsius. */
    private static final BigDecimal MAX_CELSIUS = new BigDecimal("43.0");

    /** Minimum accepted temperature in degrees Fahrenheit. */
    private static final BigDecimal MIN_FAHRENHEIT = new BigDecimal("86.0");

    /** Maximum accepted temperature in degrees Fahrenheit. */
    private static final BigDecimal MAX_FAHRENHEIT = new BigDecimal("109.4");

    private final TemperatureRepository repository;
    private final DeviceRateLimiter rateLimiter;
    private final TemperatureMetrics metrics;

    public TemperatureService(
            TemperatureRepository repository,
            DeviceRateLimiter rateLimiter,
            TemperatureMetrics metrics) {
        this.repository = repository;
        this.rateLimiter = rateLimiter;
        this.metrics = metrics;
    }

    /**
     * Ingests a single body temperature reading for the given user.
     *
     * @param userId  the authenticated user who owns the reading
     * @param request the validated reading request
     * @return ingestion response indicating accepted or rejected with error detail
     * @throws RateLimitExceededException  if the device has exceeded its per-minute quota
     * @throws OutOfRangeException         if the temperature value falls outside physiological bounds
     */
    @Transactional
    public IngestionResponse ingestSingle(UUID userId, TemperatureReadingRequest request) {
        List<IngestionError> errors = processReading(0, userId, request);
        if (errors.isEmpty()) {
            metrics.recordAccepted(1);
            return IngestionResponse.allAccepted(1);
        }
        String code = errors.get(0).code();
        if ("RATE_LIMITED".equals(code)) {
            throw new RateLimitExceededException(errors.get(0).message());
        }
        if ("OUT_OF_RANGE".equals(code)) {
            throw new OutOfRangeException(errors.get(0).message());
        }
        return new IngestionResponse(0, 1, errors);
    }

    /**
     * Ingests a batch of body temperature readings for the given user.
     *
     * <p>Processes each reading independently. Partial success (207) is possible when some
     * readings are accepted and others are rejected.
     *
     * @param userId   the authenticated user who owns the readings
     * @param requests list of validated reading requests
     * @return ingestion response summarising accepted and rejected counts with per-reading errors
     */
    @Transactional
    public IngestionResponse ingestBatch(UUID userId, List<TemperatureReadingRequest> requests) {
        List<IngestionError> errors = new ArrayList<>();
        int accepted = 0;

        for (int i = 0; i < requests.size(); i++) {
            List<IngestionError> readingErrors = processReading(i, userId, requests.get(i));
            if (readingErrors.isEmpty()) {
                accepted++;
            } else {
                errors.addAll(readingErrors);
            }
        }

        metrics.recordAccepted(accepted);
        metrics.recordRejected(errors);
        return new IngestionResponse(accepted, errors.size(), errors);
    }

    /**
     * Processes a single reading: rate limit → range check → upsert.
     *
     * @param index   zero-based position in the batch (0 for single-reading requests)
     * @param userId  the user who owns the reading
     * @param request the reading to process
     * @return empty list on success; single-element list with error detail on rejection
     */
    private List<IngestionError> processReading(
            int index, UUID userId, TemperatureReadingRequest request) {
        // 1. Rate limit
        if (!rateLimiter.checkAndIncrement(request.deviceSourceId())) {
            String msg = "Rate limit exceeded for device " + request.deviceSourceId()
                + ": max " + DeviceRateLimiter.LIMIT + " requests per minute";
            return List.of(new IngestionError(index, "RATE_LIMITED", msg));
        }

        // 2. Physiological range
        if (isOutOfRange(request.value(), request.unit())) {
            String msg = "Temperature value " + request.value() + " " + request.unit()
                + " is outside physiological range";
            return List.of(new IngestionError(index, "OUT_OF_RANGE", msg));
        }

        // 3. Upsert (idempotent — 0 rows affected means duplicate, still treated as accepted)
        repository.upsert(
            userId,
            request.deviceSourceId(),
            request.recordedAt(),
            OffsetDateTime.now(),
            request.value(),
            request.unit().name(),
            request.ingestionSource().name(),
            request.measurementMethod()
        );

        return List.of();
    }

    /**
     * Returns {@code true} if {@code value} is outside the physiological range for {@code unit}.
     *
     * @param value the measurement value to validate
     * @param unit  the unit of measurement
     * @return {@code true} if the value is out of range
     */
    private boolean isOutOfRange(BigDecimal value, TemperatureUnit unit) {
        return switch (unit) {
            case CELSIUS     -> value.compareTo(MIN_CELSIUS) < 0 || value.compareTo(MAX_CELSIUS) > 0;
            case FAHRENHEIT  -> value.compareTo(MIN_FAHRENHEIT) < 0 || value.compareTo(MAX_FAHRENHEIT) > 0;
        };
    }
}
