package com.sapphire.charting.temperature;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link TemperatureTrendService}.
 *
 * <p>All I/O is mocked. Line coverage target: 100%.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TemperatureTrendService")
class TemperatureTrendServiceTest {

    @Mock
    private TemperatureRollupRepository rollupRepository;

    @Mock
    private TemperatureReportingMetrics metrics;

    @InjectMocks
    private TemperatureTrendService service;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final LocalDate FROM = LocalDate.of(2026, 8, 1);
    private static final LocalDate TO   = LocalDate.of(2026, 8, 31);

    private TemperatureRollup sampleRollup() {
        return new TemperatureRollup(
            USER_ID, PeriodType.DAY,
            LocalDate.of(2026, 8, 15), LocalDate.of(2026, 8, 15),
            new BigDecimal("36.5"), new BigDecimal("37.8"), new BigDecimal("37.1"),
            5, java.time.OffsetDateTime.now()
        );
    }

    @Nested
    @DisplayName("getTrend")
    class GetTrend {

        @Test
        @DisplayName("should return populated dataPoints when rollups exist")
        void shouldReturnPopulatedDataPoints() {
            // Arrange
            when(rollupRepository.findByUserAndPeriod(USER_ID, PeriodType.DAY, FROM, TO))
                .thenReturn(List.of(sampleRollup()));

            // Act
            TemperatureTrendResponse result = service.getTrend(USER_ID, PeriodType.DAY, FROM, TO, null);

            // Assert
            assertThat(result.dataPoints()).hasSize(1);
            assertThat(result.dataPoints().get(0).avgCelsius()).isEqualByComparingTo("37.1");
            assertThat(result.range()).isEqualTo("DAY");
            verify(metrics).recordQuery(PeriodType.DAY);
        }

        @Test
        @DisplayName("should return empty dataPoints list when no rollups exist")
        void shouldReturnEmptyDataPointsWhenNoRollupsExist() {
            // Arrange
            when(rollupRepository.findByUserAndPeriod(any(), any(), any(), any()))
                .thenReturn(List.of());

            // Act
            TemperatureTrendResponse result = service.getTrend(USER_ID, PeriodType.WEEK, FROM, TO, null);

            // Assert
            assertThat(result.dataPoints()).isEmpty();
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("should apply date-range filter by passing from/to to repository")
        void shouldApplyDateRangeFilter() {
            // Arrange
            LocalDate customFrom = LocalDate.of(2026, 7, 1);
            LocalDate customTo   = LocalDate.of(2026, 7, 31);
            when(rollupRepository.findByUserAndPeriod(USER_ID, PeriodType.MONTH, customFrom, customTo))
                .thenReturn(List.of(sampleRollup()));

            // Act
            TemperatureTrendResponse result = service.getTrend(USER_ID, PeriodType.MONTH, customFrom, customTo, null);

            // Assert
            assertThat(result.from()).isEqualTo(customFrom);
            assertThat(result.to()).isEqualTo(customTo);
        }

        @Test
        @DisplayName("should emit OTEL metric for each query")
        void shouldEmitOtelMetric() {
            // Arrange
            when(rollupRepository.findByUserAndPeriod(any(), any(), any(), any())).thenReturn(List.of());

            // Act
            service.getTrend(USER_ID, PeriodType.DAY, FROM, TO, "device-xyz");

            // Assert
            verify(metrics).recordQuery(PeriodType.DAY);
        }
    }
}
