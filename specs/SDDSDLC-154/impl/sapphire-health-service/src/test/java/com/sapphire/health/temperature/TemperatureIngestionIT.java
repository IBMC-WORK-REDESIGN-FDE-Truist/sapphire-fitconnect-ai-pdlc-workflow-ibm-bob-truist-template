package com.sapphire.health.temperature;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the full temperature ingestion round-trip.
 *
 * <p>Spins up a real PostgreSQL container via Testcontainers. Tests verify:
 * <ul>
 *   <li>DB persistence of accepted readings</li>
 *   <li>Idempotency constraint (re-submitting the same reading is a no-op)</li>
 *   <li>Rate limit counter is enforced across consecutive requests</li>
 * </ul>
 *
 * <p>Tagged {@code @IntegrationTest} — excluded from standard CI; run in Docker Compose
 * pre-merge stage only.
 */
@Tag("IntegrationTest")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@DisplayName("TemperatureIngestionIT")
class TemperatureIngestionIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("sapphire_test")
        .withUsername("test")
        .withPassword("test");

    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private TemperatureRepository repository;

    private TemperatureReadingRequest validRequest(String deviceId, OffsetDateTime recordedAt) {
        return new TemperatureReadingRequest(
            deviceId, recordedAt,
            new BigDecimal("37.2"), TemperatureUnit.CELSIUS,
            IngestionSource.DEVICE, "oral"
        );
    }

    @Test
    @DisplayName("should persist a valid reading to the database")
    void shouldPersistValidReading() {
        // Arrange
        OffsetDateTime ts = OffsetDateTime.now();
        TemperatureReadingRequest request = validRequest("integration-device-1", ts);
        HttpHeaders headers = bearerHeaders();

        // Act
        ResponseEntity<IngestionResponse> response = restTemplate.postForEntity(
            "/temperature-readings",
            new HttpEntity<>(request, headers),
            IngestionResponse.class
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().accepted()).isEqualTo(1);
        assertThat(repository.count()).isGreaterThan(0L);
    }

    @Test
    @DisplayName("should treat duplicate submission as idempotent (no extra DB row)")
    void shouldHandleDuplicateIdempotently() {
        // Arrange
        OffsetDateTime ts = OffsetDateTime.parse("2026-07-01T08:00:00Z");
        TemperatureReadingRequest request = validRequest("idempotent-device", ts);
        HttpHeaders headers = bearerHeaders();
        HttpEntity<TemperatureReadingRequest> entity = new HttpEntity<>(request, headers);
        long countBefore = repository.count();

        // Act — submit twice
        restTemplate.postForEntity("/temperature-readings", entity, IngestionResponse.class);
        restTemplate.postForEntity("/temperature-readings", entity, IngestionResponse.class);

        // Assert — only one extra row added
        assertThat(repository.count()).isEqualTo(countBefore + 1);
    }

    @Test
    @DisplayName("should return 429 after exceeding rate limit (11th request in the same window)")
    void shouldReturn429AfterRateLimitBreached() {
        // Arrange
        HttpHeaders headers = bearerHeaders();
        String deviceId = "rate-limit-device-it";

        // Act — submit 11 readings (limit is 10 per minute)
        ResponseEntity<IngestionResponse> lastResponse = null;
        for (int i = 0; i < 11; i++) {
            TemperatureReadingRequest req = validRequest(deviceId, OffsetDateTime.now().plusSeconds(i));
            lastResponse = restTemplate.postForEntity(
                "/temperature-readings",
                new HttpEntity<>(req, headers),
                IngestionResponse.class
            );
        }

        // Assert
        assertThat(lastResponse).isNotNull();
        assertThat(lastResponse.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    /** Returns Authorization headers with a test JWT for user sub=00000000-0000-0000-0000-000000000001. */
    private HttpHeaders bearerHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        // In a real integration test, use a test Keycloak or a signed test JWT
        headers.setBearerAuth("test-jwt-token");
        return headers;
    }
}
