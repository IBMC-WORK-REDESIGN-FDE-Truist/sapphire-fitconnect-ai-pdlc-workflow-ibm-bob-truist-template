package com.sapphire.charting.temperature;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.metrics.LongCounter;
import org.springframework.stereotype.Component;

/**
 * OTEL metrics component for temperature trend query reporting.
 *
 * <p>Records {@code temperature.trend.queries} labelled by {@code range} (DAY/WEEK/MONTH)
 * every time {@link TemperatureTrendService#getTrend} is called. Health data values are
 * never included in metric attributes.
 */
@Component
public class TemperatureReportingMetrics {

    private static final String METER_NAME = "sapphire.charting.temperature";

    private final LongCounter trendQueryCounter;

    public TemperatureReportingMetrics(OpenTelemetry openTelemetry) {
        var meter = openTelemetry.getMeter(METER_NAME);
        this.trendQueryCounter = meter.counterBuilder("temperature.trend.queries")
            .setDescription("Number of temperature trend queries served")
            .setUnit("queries")
            .build();
    }

    /**
     * Increments the trend query counter for the given aggregation range.
     *
     * @param range the {@link PeriodType} requested (DAY, WEEK, MONTH)
     */
    public void recordQuery(PeriodType range) {
        trendQueryCounter.add(1,
            io.opentelemetry.api.common.Attributes.of(
                io.opentelemetry.api.common.AttributeKey.stringKey("range"), range.name()
            ));
    }
}
