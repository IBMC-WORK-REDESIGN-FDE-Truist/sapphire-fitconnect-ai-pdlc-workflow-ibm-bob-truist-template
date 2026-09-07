package com.sapphire.health.temperature;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link TemperatureService}.
 *
 * <p>All I/O is mocked. Tests follow the AAA (Arrange–Act–Assert) pattern.
 * Line coverage target: 100%.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TemperatureService")
class TemperatureServiceTest {

    @Mock
    private TemperatureRepository repository;

    @Mock
    private DeviceRateLimiter rateLimiter;

    @Mock
    private TemperatureMetrics metrics;

    @InjectMocks
    private TemperatureService service;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final String DEVICE_ID = "device-abc-123";

    private TemperatureReadingRequest validCelsiusRequest() {
        return new TemperatureReadingRequest(
            DEVICE_ID,
            OffsetDateTime.now(),
            new BigDecimal("37.0"),
            TemperatureUnit.CELSIUS,
            IngestionSource.DEVICE,
            "oral"
        );
    }

    @BeforeEach
    void allowRateLimit() {
        when(rateLimiter.checkAndIncrement(anyString())).thenReturn(true);
    }

    // -----------------------------------------------------------------------
    // Single ingestion — happy path
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("ingestSingle")
    class IngestSingle {

        @Test
        @DisplayName("should accept a valid Celsius reading and return accepted=1")
        void shouldAcceptValidCelsiusReading() {
            // Arrange
            when(repository.upsert(any(), anyString(), any(), any(), any(), anyString(), anyString(), any()))
                .thenReturn(1);

            // Act
            IngestionResponse response = service.ingestSingle(USER_ID, validCelsiusRequest());

            // Assert
            assertThat(response.accepted()).isEqualTo(1);
            assertThat(response.rejected()).isEqualTo(0);
            assertThat(response.errors()).isEmpty();
            verify(metrics).recordAccepted(1);
        }

        @Test
        @DisplayName("should accept a valid Fahrenheit reading (98.6 F)")
        void shouldAcceptValidFahrenheitReading() {
            // Arrange
            TemperatureReadingRequest request = new TemperatureReadingRequest(
                DEVICE_ID, OffsetDateTime.now(), new BigDecimal("98.6"),
                TemperatureUnit.FAHRENHEIT, IngestionSource.API, null
            );
            when(repository.upsert(any(), anyString(), any(), any(), any(), anyString(), anyString(), any()))
                .thenReturn(1);

            // Act
            IngestionResponse response = service.ingestSingle(USER_ID, request);

            // Assert
            assertThat(response.accepted()).isEqualTo(1);
        }

        @Test
        @DisplayName("should throw OutOfRangeException when Celsius value is below 30.0")
        void shouldRejectOutOfRangeCelsiusLow() {
            // Arrange
            TemperatureReadingRequest request = new TemperatureReadingRequest(
                DEVICE_ID, OffsetDateTime.now(), new BigDecimal("29.9"),
                TemperatureUnit.CELSIUS, IngestionSource.DEVICE, null
            );

            // Act + Assert
            assertThatThrownBy(() -> service.ingestSingle(USER_ID, request))
                .isInstanceOf(OutOfRangeException.class)
                .hasMessageContaining("outside physiological range");
        }

        @Test
        @DisplayName("should throw OutOfRangeException when Celsius value is above 43.0")
        void shouldRejectOutOfRangeCelsiusHigh() {
            // Arrange
            TemperatureReadingRequest request = new TemperatureReadingRequest(
                DEVICE_ID, OffsetDateTime.now(), new BigDecimal("43.1"),
                TemperatureUnit.CELSIUS, IngestionSource.DEVICE, null
            );

            // Act + Assert
            assertThatThrownBy(() -> service.ingestSingle(USER_ID, request))
                .isInstanceOf(OutOfRangeException.class);
        }

        @Test
        @DisplayName("should throw RateLimitExceededException when device rate limit is reached")
        void shouldThrowRateLimitExceptionWhenLimitReached() {
            // Arrange
            when(rateLimiter.checkAndIncrement(DEVICE_ID)).thenReturn(false);

            // Act + Assert
            assertThatThrownBy(() -> service.ingestSingle(USER_ID, validCelsiusRequest()))
                .isInstanceOf(RateLimitExceededException.class)
                .hasMessageContaining("Rate limit exceeded");
        }

        @Test
        @DisplayName("should return accepted=1 when repository upsert returns 0 (idempotent duplicate)")
        void shouldTreatDuplicateAsAccepted() {
            // Arrange
            when(repository.upsert(any(), anyString(), any(), any(), any(), anyString(), anyString(), any()))
                .thenReturn(0);

            // Act
            IngestionResponse response = service.ingestSingle(USER_ID, validCelsiusRequest());

            // Assert
            assertThat(response.accepted()).isEqualTo(1);
            assertThat(response.rejected()).isEqualTo(0);
        }
    }

    // -----------------------------------------------------------------------
    // Batch ingestion
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("ingestBatch")
    class IngestBatch {

        @Test
        @DisplayName("should accept all valid readings in a batch")
        void shouldAcceptAllValidReadings() {
            // Arrange
            when(repository.upsert(any(), anyString(), any(), any(), any(), anyString(), anyString(), any()))
                .thenReturn(1);
            List<TemperatureReadingRequest> requests = List.of(
                validCelsiusRequest(), validCelsiusRequest(), validCelsiusRequest()
            );

            // Act
            IngestionResponse response = service.ingestBatch(USER_ID, requests);

            // Assert
            assertThat(response.accepted()).isEqualTo(3);
            assertThat(response.rejected()).isEqualTo(0);
            assertThat(response.errors()).isEmpty();
        }

        @Test
        @DisplayName("should return partial success (207-style) when some readings are out of range")
        void shouldReturnPartialSuccessForMixedBatch() {
            // Arrange
            when(repository.upsert(any(), anyString(), any(), any(), any(), anyString(), anyString(), any()))
                .thenReturn(1);
            TemperatureReadingRequest outOfRange = new TemperatureReadingRequest(
                DEVICE_ID, OffsetDateTime.now(), new BigDecimal("50.0"),
                TemperatureUnit.CELSIUS, IngestionSource.DEVICE, null
            );
            List<TemperatureReadingRequest> requests = List.of(validCelsiusRequest(), outOfRange);

            // Act
            IngestionResponse response = service.ingestBatch(USER_ID, requests);

            // Assert
            assertThat(response.accepted()).isEqualTo(1);
            assertThat(response.rejected()).isEqualTo(1);
            assertThat(response.errors()).hasSize(1);
            assertThat(response.errors().get(0).code()).isEqualTo("OUT_OF_RANGE");
            assertThat(response.errors().get(0).index()).isEqualTo(1);
        }

        @Test
        @DisplayName("should include rate-limited error in batch response")
        void shouldIncludeRateLimitedErrorInBatch() {
            // Arrange
            when(rateLimiter.checkAndIncrement(DEVICE_ID))
                .thenReturn(true)  // first reading accepted
                .thenReturn(false); // second reading rate-limited
            when(repository.upsert(any(), anyString(), any(), any(), any(), anyString(), anyString(), any()))
                .thenReturn(1);
            List<TemperatureReadingRequest> requests = List.of(
                validCelsiusRequest(), validCelsiusRequest()
            );

            // Act
            IngestionResponse response = service.ingestBatch(USER_ID, requests);

            // Assert
            assertThat(response.accepted()).isEqualTo(1);
            assertThat(response.rejected()).isEqualTo(1);
            assertThat(response.errors().get(0).code()).isEqualTo("RATE_LIMITED");
        }
    }
}
