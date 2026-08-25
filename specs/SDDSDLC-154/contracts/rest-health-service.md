# REST API Contract: sapphire-health-service — Temperature Ingestion

**Feature**: SDDSDLC-154
**Service**: `sapphire-health-service`
**Version**: additive extension to existing metrics API

---

## POST /temperature-readings

Ingest a single body temperature reading.

### Request

**Headers**
```
Authorization: Bearer <JWT>
Content-Type: application/json
```

**Body**
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

| Field | Type | Required | Constraints |
|---|---|---|---|
| `value` | number | Yes | 30.0–43.0 °C or 86.0–109.4 °F |
| `unit` | string | Yes | `"C"` or `"F"` |
| `recorded_at` | ISO-8601 string | Yes | UTC; must not be > 5 min in the future |
| `device_source_id` | string | Yes | max 255 chars |
| `ingestion_source` | string | Yes | `"DEVICE"` or `"API"` |
| `measurement_method` | string | No | oral, axillary, tympanic; nullable |

### Responses

| Status | Meaning |
|---|---|
| `200 OK` | Record accepted (including idempotent re-submission) |
| `401 Unauthorized` | Missing or invalid JWT |
| `422 Unprocessable Entity` | Validation failure — body contains structured error |
| `429 Too Many Requests` | Rate limit exceeded (10 req/min per device) |

**422 Error body**:
```json
{
  "errors": [
    { "field": "value", "reason": "VALUE_OUT_OF_RANGE", "acceptable_range": "30.0–43.0 °C" }
  ]
}
```

---

## POST /temperature-readings/batch

Ingest multiple temperature readings in one request.

### Request Body
```json
{
  "readings": [
    { "value": 37.1, "unit": "C", "recorded_at": "2026-08-20T08:30:00Z", "device_source_id": "device-abc-123", "ingestion_source": "DEVICE" },
    { "value": 60.0, "unit": "C", "recorded_at": "2026-08-20T09:00:00Z", "device_source_id": "device-abc-123", "ingestion_source": "DEVICE" }
  ]
}
```

Max batch size: 100 records (configurable).

### Responses

| Status | Meaning |
|---|---|
| `200 OK` | All records accepted |
| `207 Multi-Status` | Partial success — some records accepted, some rejected |
| `401 Unauthorized` | Missing or invalid JWT |
| `429 Too Many Requests` | Rate limit exceeded |

**207 body**:
```json
{
  "accepted": 1,
  "rejected": 1,
  "errors": [
    { "index": 1, "value": 60.0, "unit": "C", "reason": "VALUE_OUT_OF_RANGE", "acceptable_range": "30.0–43.0 °C" }
  ]
}
```
