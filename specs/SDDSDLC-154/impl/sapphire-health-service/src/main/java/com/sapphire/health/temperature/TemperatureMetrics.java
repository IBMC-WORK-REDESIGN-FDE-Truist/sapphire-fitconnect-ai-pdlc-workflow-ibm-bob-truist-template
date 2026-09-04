package com.sapphire.health.temperature;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.metrics.LongCounter;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * OTEL metrics component for body temperature ingestion.
 *
 * <p>Records {@code temperature.ingestion.accepted} and {@code temperature.ingestion.rejected}
 * counters, labelled by {@code outcome}, on every ingestion result. Health data values are
 * never included in metric attributes.
 */
@Component
public class TemperatureMetrics {

    private static final String METER_NAME = "sapphire.health.temperature";

    private final LongCounter acceptedCounter;
    private final LongCounter rejectedCounter;

    public TemperatureMetrics(OpenTelemetry openTelemetry) {
        var meter = openTelemetry.getMeter(METER_NAME);

        this.acceptedCounter = meter.counterBuilder("temperature.ingestion.accepted")
            .setDescription("Number of temperature readings successfully ingested")
            .setUnit("readings")
            .build();

        this.rejectedCounter = meter.counterBuilder("temperature.ingestion.rejected")
            .setDescription("Number of temperature readings rejected during ingestion")
            .setUnit("readings")
            .build();
    }

    /**
     * Increments the accepted counter by the given count.
     *
     * @param count number of readings accepted in this batch
     */
    public void recordAccepted(int count) {
        if (count > 0) {
            acceptedCounter.add(count,
                io.opentelemetry.api.common.Attributes.of(
                    io.opentelemetry.api.common.AttributeKey.stringKey("outcome"), "accepted"
                ));
        }
    }

    /**
     * Increments the rejected counter for each rejection code found in {@code errors}.
     *
     * @param errors list of ingestion errors from the current request
     */
    public void recordRejected(List<IngestionError> errors) {
        for (IngestionError error : errors) {
            String outcome = switch (error.code()) {
                case "OUT_OF_RANGE"  -> "rejected-out-of-range";
                case "RATE_LIMITED"  -> "rejected-rate-limited";
                case "INVALID_UNIT"  -> "rejected-invalid-unit";
                case "DUPLICATE"     -> "deduplicated";
                default              -> "rejected-unknown";
            };
            rejectedCounter.add(1,
                io.opentelemetry.api.common.Attributes.of(
                    io.opentelemetry.api.common.AttributeKey.stringKey("outcome"), outcome
                ));
        }
    }
}
