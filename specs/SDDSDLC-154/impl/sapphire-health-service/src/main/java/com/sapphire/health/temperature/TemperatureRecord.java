package com.sapphire.health.temperature;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * JPA entity representing a single body temperature reading recorded by a user device or API.
 *
 * <p>Unique constraint on {@code (user_id, device_source_id, recorded_at)} enforces idempotent
 * deduplication: re-submitting the same reading from the same device at the same timestamp is a
 * no-op rather than an error.
 */
@Entity
@Table(
    name = "temperature_records",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_temperature_records_dedup",
        columnNames = {"user_id", "device_source_id", "recorded_at"}
    )
)
public class TemperatureRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "device_source_id", nullable = false, length = 255)
    private String deviceSourceId;

    @Column(name = "recorded_at", nullable = false)
    private OffsetDateTime recordedAt;

    @Column(name = "ingested_at", nullable = false)
    private OffsetDateTime ingestedAt;

    @Column(name = "value", nullable = false, precision = 5, scale = 2)
    private BigDecimal value;

    @Enumerated(EnumType.STRING)
    @Column(name = "unit", nullable = false, length = 10)
    private TemperatureUnit unit;

    @Enumerated(EnumType.STRING)
    @Column(name = "ingestion_source", nullable = false, length = 10)
    private IngestionSource ingestionSource;

    @Column(name = "measurement_method", length = 50)
    private String measurementMethod;

    protected TemperatureRecord() {
        // JPA no-arg constructor
    }

    public TemperatureRecord(
            UUID userId,
            String deviceSourceId,
            OffsetDateTime recordedAt,
            OffsetDateTime ingestedAt,
            BigDecimal value,
            TemperatureUnit unit,
            IngestionSource ingestionSource,
            String measurementMethod) {
        this.userId = userId;
        this.deviceSourceId = deviceSourceId;
        this.recordedAt = recordedAt;
        this.ingestedAt = ingestedAt;
        this.value = value;
        this.unit = unit;
        this.ingestionSource = ingestionSource;
        this.measurementMethod = measurementMethod;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getDeviceSourceId() { return deviceSourceId; }
    public OffsetDateTime getRecordedAt() { return recordedAt; }
    public OffsetDateTime getIngestedAt() { return ingestedAt; }
    public BigDecimal getValue() { return value; }
    public TemperatureUnit getUnit() { return unit; }
    public IngestionSource getIngestionSource() { return ingestionSource; }
    public String getMeasurementMethod() { return measurementMethod; }
}
