package com.sapphire.health.temperature;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.http.ProblemDetail;

import java.net.URI;
import java.util.UUID;

/**
 * REST controller for body temperature ingestion endpoints.
 *
 * <p>Exposes two endpoints:
 * <ul>
 *   <li>{@code POST /temperature-readings} — ingest a single reading</li>
 *   <li>{@code POST /temperature-readings/batch} — ingest up to 100 readings</li>
 * </ul>
 *
 * <p>JWT subject ({@code sub} claim) is extracted from the authenticated principal and
 * treated as the {@code userId}. All business logic is delegated to {@link TemperatureService};
 * this controller contains no business logic.
 */
@RestController
@RequestMapping("/temperature-readings")
public class TemperatureController {

    private final TemperatureService temperatureService;

    public TemperatureController(TemperatureService temperatureService) {
        this.temperatureService = temperatureService;
    }

    /**
     * Ingests a single body temperature reading.
     *
     * @param jwt     the authenticated JWT principal (resolved by Spring Security)
     * @param request the validated reading request body
     * @return 200 OK with {@link IngestionResponse} on success
     */
    @PostMapping
    public ResponseEntity<IngestionResponse> ingestSingle(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody TemperatureReadingRequest request) {
        UUID userId = UUID.fromString(jwt.getSubject());
        IngestionResponse response = temperatureService.ingestSingle(userId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Ingests a batch of up to 100 body temperature readings.
     *
     * <p>Returns 200 when all readings are accepted, or 207 Multi-Status when the batch
     * contains a mix of accepted and rejected readings.
     *
     * @param jwt     the authenticated JWT principal
     * @param request the validated batch request body
     * @return 200 or 207 with {@link IngestionResponse}
     */
    @PostMapping("/batch")
    public ResponseEntity<IngestionResponse> ingestBatch(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody BatchIngestionRequest request) {
        UUID userId = UUID.fromString(jwt.getSubject());
        IngestionResponse response = temperatureService.ingestBatch(userId, request.readings());
        int status = response.rejected() > 0 && response.accepted() > 0
            ? 207
            : HttpStatus.OK.value();
        return ResponseEntity.status(status).body(response);
    }

    /**
     * Global exception handler for temperature ingestion domain exceptions.
     */
    @ControllerAdvice
    static class TemperatureExceptionHandler {

        @ExceptionHandler(RateLimitExceededException.class)
        public ResponseEntity<ProblemDetail> handleRateLimit(RateLimitExceededException ex) {
            ProblemDetail detail = ProblemDetail.forStatusAndDetail(
                HttpStatus.TOO_MANY_REQUESTS, ex.getMessage());
            detail.setType(URI.create("https://sapphire.example.com/errors/rate-limited"));
            detail.setTitle("Rate Limit Exceeded");
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(detail);
        }

        @ExceptionHandler(OutOfRangeException.class)
        public ResponseEntity<ProblemDetail> handleOutOfRange(OutOfRangeException ex) {
            ProblemDetail detail = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
            detail.setType(URI.create("https://sapphire.example.com/errors/out-of-range"));
            detail.setTitle("Temperature Out of Physiological Range");
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(detail);
        }
    }
}
