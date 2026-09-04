-- Migration: Create temperature_rollups table
-- Feature: SDDSDLC-154 — Body Temperature Metric Reporting
-- Story:   SDDSDLC-167 (sapphire-charting-api)

CREATE TABLE temperature_rollups (
    id           UUID         NOT NULL DEFAULT gen_random_uuid(),
    user_id      UUID         NOT NULL,
    period_type  VARCHAR(10)  NOT NULL,          -- 'DAY' | 'WEEK' | 'MONTH'
    period_start DATE         NOT NULL,
    period_end   DATE         NOT NULL,
    min_celsius  NUMERIC(5, 2) NOT NULL,
    max_celsius  NUMERIC(5, 2) NOT NULL,
    avg_celsius  NUMERIC(5, 2) NOT NULL,
    record_count INT          NOT NULL DEFAULT 0,
    computed_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT pk_temperature_rollups PRIMARY KEY (id),
    CONSTRAINT uq_temperature_rollups_period
        UNIQUE (user_id, period_type, period_start)
);

CREATE INDEX idx_temperature_rollups_user_period
    ON temperature_rollups (user_id, period_type, period_start DESC);
