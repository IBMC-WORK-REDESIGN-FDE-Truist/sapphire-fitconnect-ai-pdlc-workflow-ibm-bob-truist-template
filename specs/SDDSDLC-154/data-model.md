# Data Model: SDDSDLC-154 — Body Temperature Metric

**Phase**: 1 — Design
**Date**: 2026-08-20
**Branch**: `SDDSDLC-154`

---

## Entities

### TemperatureRecord

Primary storage entity for a single body temperature measurement.

| Field | Type | Nullable | Constraints | Notes |
|---|---|---|---|---|
| `id` | UUID | No | PK, auto-generated | Surrogate key |
| `user_id` | UUID | No | FK → users.id | Authenticated user |
| `device_source_id` | VARCHAR(255) | No | | Device identifier submitted by caller |
| `recorded_at` | TIMESTAMPTZ | No | | ISO-8601 UTC — submitted by device |
| `ingested_at` | TIMESTAMPTZ | No | DEFAULT now() | Server-side ingestion timestamp |
| `value` | DECIMAL(5,2) | No | | Temperature value |
| `unit` | VARCHAR(1) | No | CHECK IN ('C','F') | 'C' = Celsius, 'F' = Fahrenheit |
| `ingestion_source` | VARCHAR(20) | No | CHECK IN ('DEVICE','API') | How it arrived |
| `measurement_method` | VARCHAR(50) | Yes | | oral, axillary, tympanic, etc. |

**Unique constraint**: `UNIQUE (user_id, device_source_id, recorded_at)` — enforces idempotent upsert (FR-001a).

**Indexes**:
- `idx_temperature_records_user_time`: `(user_id, recorded_at DESC)` — primary time-series query index (FR-011)
- `idx_temperature_records_device`: `(device_source_id, recorded_at DESC)` — supports device-source filter (FR-015)

**Validation rules** (enforced at service layer, not DB):
- `value` MUST be within the configured physiological range [30.0, 43.0] °C or [86.0, 109.4] °F (FR-003)
- `unit` MUST be 'C' or 'F'; missing or unrecognised unit → reject with 422
- `recorded_at` future timestamps beyond +5 minutes of server time → reject with 422 (clock drift guard)

---

### TemperatureRollup

Pre-aggregated summary per user, per period type. Populated by scheduled job.

| Field | Type | Nullable | Constraints | Notes |
|---|---|---|---|---|
| `id` | UUID | No | PK | |
| `user_id` | UUID | No | FK → users.id | |
| `period_type` | VARCHAR(10) | No | CHECK IN ('DAY','WEEK','MONTH') | Granularity |
| `period_start` | DATE | No | | Inclusive start of period (UTC date) |
| `period_end` | DATE | No | | Inclusive end of period (UTC date) |
| `min_celsius` | DECIMAL(5,2) | No | | Minimum reading, normalised to °C |
| `max_celsius` | DECIMAL(5,2) | No | | Maximum reading, normalised to °C |
| `avg_celsius` | DECIMAL(5,2) | No | | Average reading, normalised to °C |
| `record_count` | INTEGER | No | | Number of raw records in period |
| `computed_at` | TIMESTAMPTZ | No | DEFAULT now() | When rollup was last computed |

**Unique constraint**: `UNIQUE (user_id, period_type, period_start)` — one rollup row per user per period bucket.

**Notes**:
- Values stored in Celsius; UI converts to Fahrenheit client-side (Decision 6).
- Rollup job overwrites existing row on recompute (upsert on conflict).

---

### MetricType (extended)

Existing catalog table / enum extended with new entry. No schema change to the table structure.

| Metric Type Key | Display Name | Unit Options | Added By |
|---|---|---|---|
| `BLOOD_PRESSURE` | Blood Pressure | mmHg | existing |
| `SPO2` | Blood Oxygen | % | existing |
| `ACTIVITY` | Activity | steps/kcal | existing |
| **`BODY_TEMPERATURE`** | **Body Temperature** | **°C / °F** | **this story** |

---

## State Transitions

### TemperatureRecord — Ingestion Flow

```
[Device / API request]
        │
        ▼
[Rate limit check]──(exceeded)──▶ 429 Too Many Requests
        │
        ▼
[Auth check]──────(401)──────────▶ 401 Unauthorized
        │
        ▼
[Field validation]──(invalid)───▶ 422 Unprocessable Entity
        │
        ▼
[Range validation]──(out of range)▶ 422 with range error
        │
        ▼
[Upsert on (user, device, ts)]──(duplicate)──▶ 200 Accepted (no-op)
        │
        ▼
[Persist record]
        │
        ▼
  200 / 207 Accepted
```

For batch requests: each record independently traverses validation. Valid records persisted; invalid listed in response. Returns 207 Multi-Status if partial failure.

---

## API Record Shapes (internal, pre-contract)

### Ingestion Request (single)
```json
{
  "value": 37.1,
  "unit": "C",
  "recorded_at": "2026-08-20T08:30:00Z",
  "device_source_id": "device-abc-123",
  "ingestion_source": "DEVICE",
  "measurement_method": "oral"
}
```

### Ingestion Request (batch)
```json
{
  "readings": [
    { "value": 37.1, "unit": "C", "recorded_at": "2026-08-20T08:30:00Z", "device_source_id": "device-abc-123", "ingestion_source": "DEVICE" },
    { "value": 36.8, "unit": "C", "recorded_at": "2026-08-20T20:00:00Z", "device_source_id": "device-abc-123", "ingestion_source": "DEVICE" }
  ]
}
```

### Ingestion Response (partial failure)
```json
{
  "accepted": 1,
  "rejected": 1,
  "errors": [
    { "index": 1, "value": 60.0, "unit": "C", "reason": "VALUE_OUT_OF_RANGE", "acceptable_range": "30.0–43.0 °C" }
  ]
}
```

### Temperature Trend Response
```json
{
  "user_id": "user-uuid",
  "range": "WEEK",
  "unit": "C",
  "data_points": [
    { "period_start": "2026-08-14", "period_end": "2026-08-14", "min": 36.5, "max": 37.4, "avg": 36.9, "count": 3 }
  ]
}
```
