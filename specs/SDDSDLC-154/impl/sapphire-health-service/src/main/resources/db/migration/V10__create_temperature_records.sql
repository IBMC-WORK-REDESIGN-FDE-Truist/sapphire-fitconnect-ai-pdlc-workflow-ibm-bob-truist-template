-- Migration: Create temperature_records table
-- Feature: SDDSDLC-154 — Body Temperature Metric Ingestion
-- Story:   SDDSDLC-166 (sapphire-health-service)

CREATE TABLE temperature_records (
    id                UUID        NOT NULL DEFAULT gen_random_uuid(),
    user_id           UUID        NOT NULL,
    device_source_id  VARCHAR(255) NOT NULL,
    recorded_at       TIMESTAMPTZ NOT NULL,
    ingested_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    value             NUMERIC(5, 2) NOT NULL,
    unit              VARCHAR(10) NOT NULL,                -- 'CELSIUS' | 'FAHRENHEIT'
    ingestion_source  VARCHAR(10) NOT NULL,                -- 'DEVICE' | 'API'
    measurement_method VARCHAR(50),                        -- oral | axillary | tympanic; nullable

    CONSTRAINT pk_temperature_records PRIMARY KEY (id),
    CONSTRAINT uq_temperature_records_dedup
        UNIQUE (user_id, device_source_id, recorded_at)
);

CREATE INDEX idx_temperature_records_user_time
    ON temperature_records (user_id, recorded_at DESC);

CREATE INDEX idx_temperature_records_device
    ON temperature_records (device_source_id, recorded_at DESC);
