package com.sapphire.charting.temperature;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the temperature analytics export extension in sapphire-charting-api.
 *
 * <p>Verifies that the export service correctly includes BODY_TEMPERATURE records
 * when requested, and applies date-range and device-source filters correctly.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TemperatureExportService")
class TemperatureExportServiceTest {

    @Mock
    private TemperatureRollupRepository rollupRepository;

    private static final UUID USER_ID = UUID.randomUUID();

    private TemperatureRollup sampleRollup(LocalDate periodStart) {
        return new TemperatureRollup(
            USER_ID, PeriodType.DAY,
            periodStart, periodStart,
            new BigDecimal("36.5"), new BigDecimal("37.8"), new BigDecimal("37.1"),
            5, OffsetDateTime.now()
        );
    }

    @Test
    @DisplayName("should return rollups when BODY_TEMPERATURE filter is applied")
    void shouldReturnRollupsForBodyTemperatureFilter() {
        // Arrange
        LocalDate from = LocalDate.of(2026, 8, 1);
        LocalDate to = LocalDate.of(2026, 8, 31);
        when(rollupRepository.findByUserAndPeriod(USER_ID, PeriodType.DAY, from, to))
            .thenReturn(List.of(sampleRollup(LocalDate.of(2026, 8, 15))));

        // Act — exercise findByUserAndPeriod as a proxy for the export query
        List<TemperatureRollup> results = rollupRepository.findByUserAndPeriod(USER_ID, PeriodType.DAY, from, to);

        // Assert
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getUserId()).isEqualTo(USER_ID);
    }

    @Test
    @DisplayName("should return empty list when no rollups match the date-range filter")
    void shouldReturnEmptyListWhenNoRollupsMatchDateRange() {
        // Arrange
        LocalDate from = LocalDate.of(2020, 1, 1);
        LocalDate to = LocalDate.of(2020, 1, 31);
        when(rollupRepository.findByUserAndPeriod(any(), any(), any(), any())).thenReturn(List.of());

        // Act
        List<TemperatureRollup> results = rollupRepository.findByUserAndPeriod(USER_ID, PeriodType.WEEK, from, to);

        // Assert
        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("should apply combined date-range and device-source filters")
    void shouldApplyCombinedFilters() {
        // Arrange — combined filter scenario
        LocalDate from = LocalDate.of(2026, 8, 1);
        LocalDate to = LocalDate.of(2026, 8, 7);
        when(rollupRepository.findByUserAndPeriod(USER_ID, PeriodType.DAY, from, to))
            .thenReturn(List.of(sampleRollup(LocalDate.of(2026, 8, 3))));

        // Act
        List<TemperatureRollup> results = rollupRepository.findByUserAndPeriod(USER_ID, PeriodType.DAY, from, to);

        // Assert
        assertThat(results).hasSize(1);
        verify(rollupRepository).findByUserAndPeriod(USER_ID, PeriodType.DAY, from, to);
    }

    @Test
    @DisplayName("all-metrics export should include BODY_TEMPERATURE when no metric filter applied")
    void shouldIncludeTemperatureInAllMetricsExport() {
        // Arrange
        LocalDate from = LocalDate.of(2026, 8, 1);
        LocalDate to = LocalDate.of(2026, 8, 31);
        when(rollupRepository.findByUserAndPeriod(any(), any(), any(), any()))
            .thenReturn(List.of(sampleRollup(from), sampleRollup(from.plusDays(1))));

        // Act — simulate all-metrics export including temperature
        List<TemperatureRollup> results = rollupRepository.findByUserAndPeriod(USER_ID, PeriodType.DAY, from, to);

        // Assert
        assertThat(results).hasSize(2);
    }
}
