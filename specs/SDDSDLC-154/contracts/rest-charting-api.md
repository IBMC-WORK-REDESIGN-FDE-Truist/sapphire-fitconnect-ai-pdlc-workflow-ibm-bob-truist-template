# REST API Contract: sapphire-charting-api — Temperature Reporting

**Feature**: SDDSDLC-154
**Service**: `sapphire-charting-api`
**Version**: additive extension to existing charting API

---

## GET /metrics/temperature/trend

Returns pre-aggregated temperature trend data for the authenticated user.

### Request

**Headers**
```
Authorization: Bearer <JWT>
```

**Query Parameters**

| Parameter | Type | Required | Values / Format |
|---|---|---|---|
| `range` | string | Yes | `DAY`, `WEEK`, `MONTH` |
| `device_source_id` | string | No | Filter to a specific device |
| `from` | ISO-8601 date | No | Start of date range (inclusive); defaults to range start |
| `to` | ISO-8601 date | No | End of date range (inclusive); defaults to today |

### Response

**200 OK**
```json
{
  "user_id": "user-uuid",
  "range": "WEEK",
  "unit": "C",
  "data_points": [
    {
      "period_start": "2026-08-14",
      "period_end": "2026-08-14",
      "min": 36.5,
      "max": 37.4,
      "avg": 36.9,
      "count": 3
    }
  ]
}
```

**Empty state** (no records for range):
```json
{
  "user_id": "user-uuid",
  "range": "WEEK",
  "unit": "C",
  "data_points": []
}
```

| Status | Meaning |
|---|---|
| `200 OK` | Trend data returned (may be empty) |
| `400 Bad Request` | Invalid `range` value or malformed date |
| `401 Unauthorized` | Missing or invalid JWT |

---

## GET /metrics/export

Returns filtered health metrics data for export. Temperature is included as metric type `BODY_TEMPERATURE`.

### Query Parameters

| Parameter | Type | Required |
|---|---|---|
| `metric_type` | string | No — omit for all metrics; `BODY_TEMPERATURE` for temperature only |
| `from` | ISO-8601 date | Yes |
| `to` | ISO-8601 date | Yes |
| `device_source_id` | string | No |

### Response

**200 OK** — returns a list of metric records in the existing export format:
```json
{
  "records": [
    {
      "metric_type": "BODY_TEMPERATURE",
      "value": 37.1,
      "unit": "C",
      "recorded_at": "2026-08-20T08:30:00Z",
      "device_source_id": "device-abc-123",
      "measurement_method": "oral"
    }
  ]
}
```
