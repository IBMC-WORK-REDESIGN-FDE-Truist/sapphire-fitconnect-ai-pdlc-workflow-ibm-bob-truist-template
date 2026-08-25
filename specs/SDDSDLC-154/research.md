# Research: SDDSDLC-154 — Body Temperature Metric Ingestion, Storage, and Reporting

**Phase**: 0 — Research
**Date**: 2026-08-20
**Branch**: `SDDSDLC-154`

---

## Decision 1 — Time-Series Storage Strategy for Temperature Records

**Decision**: Store raw `TemperatureRecord` rows in the existing health metrics relational datastore (PostgreSQL assumed), indexed on `(user_id, timestamp DESC)`, plus a separate `TemperatureRollup` table for pre-aggregated daily/weekly/monthly buckets. Rollups are computed asynchronously via a scheduled job, not on ingestion.

**Rationale**: The platform already stores blood pressure and SpO2 in a relational store with time-series indexing. Extending the same table-per-metric-type (or partitioned unified metrics table) pattern avoids introducing a new storage technology, keeps schema changes additive, and aligns with FR-012 (same retention rules). Pre-aggregated rollup rows satisfy SC-001 (< 3 s dashboard load) without expensive aggregation at query time.

**Alternatives considered**:
- TimescaleDB hypertable: higher operational complexity; justified only if query volume grows beyond existing infrastructure capacity — deferrable.
- On-demand aggregation: simpler schema, but violates SC-001 latency requirement for users with large record counts.
- Separate NoSQL store: introduces a new dependency class; out of scope.

---

## Decision 2 — Idempotency Key for Duplicate Detection

**Decision**: Use a composite unique constraint on `(user_id, device_source_id, recorded_at)` in the `temperature_records` table. Ingestion logic performs an upsert (`INSERT … ON CONFLICT DO NOTHING`) — conflict returns HTTP 200 as if accepted, no new row created.

**Rationale**: Devices re-sync historical data after reconnection; idempotent upsert prevents phantom duplicates without requiring callers to track state. The `(user, device, timestamp)` triple is the natural business key, matching FR-001a.

**Alternatives considered**:
- Client-provided idempotency token (UUID): requires device SDK changes; higher coordination cost.
- `409 Conflict` on duplicate: breaks re-sync patterns where devices cannot distinguish "already sent" from "failed".

---

## Decision 3 — Rate Limiting Mechanism

**Decision**: Implement rate limiting in the ingestion service using a token-bucket counter keyed on `device_source_id`, stored in an in-process cache (Caffeine for Spring Boot) with a 60-second sliding window, limit = 10. If a distributed rate limit is needed later, replace with Redis; this is a configuration change only.

**Rationale**: 10 req/min per device (FR-001b) is well within in-process counter capability for expected device counts. Avoids a new Redis dependency for the initial delivery. Spring Boot + Caffeine provides TTL-based eviction out of the box.

**Alternatives considered**:
- API Gateway rate limiting: correct long-term direction but requires infrastructure changes outside this story's scope.
- Redis sliding window: correct for multi-instance deployments; deferrable until horizontal scaling is required.

---

## Decision 4 — Rollup Computation Scheduling

**Decision**: Use a Spring `@Scheduled` task (cron expression, e.g., every 15 minutes) in `sapphire-charting-api` to compute or refresh `TemperatureRollup` rows for all users who received new records since the last run. Tracks last-processed watermark via a `rollup_watermark` configuration row.

**Rationale**: Decouples ingestion latency from rollup computation (FR-013). 15-minute freshness is acceptable for daily/weekly/monthly dashboard views. Rollup job is idempotent — re-running over the same window produces the same result.

**Alternatives considered**:
- Event-driven (Kafka): correct at scale, but introduces Kafka dependency not justified by current scope.
- On-write trigger: creates coupling between ingestion and charting services; violates service boundary.
- Nightly batch only: insufficient freshness for same-day dashboard view.

---

## Decision 5 — GraphQL Schema Extension Strategy (BFF)

**Decision**: Add new additive types and query/mutation fields to the existing BFF GraphQL schema. Use input-object pattern for mutations; return payload objects. No existing fields are renamed or removed. Apply `@deprecated` to any future field replacements. New query: `temperatureTrend(userId: ID!, range: TemperatureRange!, deviceSource: String): TemperatureTrendPayload!`. New mutation: `ingestTemperatureReadings(input: IngestTemperatureInput!): IngestTemperaturePayload!`.

**Rationale**: Constitution mandates additive-only GraphQL changes. Relay-style connection types used for list results. DataLoader applied to resolver that fetches rollup data per user to prevent N+1.

**Alternatives considered**:
- REST passthrough in BFF: breaks the single-GraphQL-surface contract.
- New standalone GraphQL endpoint: adds routing complexity without benefit.

---

## Decision 6 — Unit Conversion Strategy (UI)

**Decision**: Store and transmit temperature values in the original submitted unit. The BFF returns both the raw value and its unit in the `TemperatureRecord` type. The UI performs in-memory Celsius↔Fahrenheit conversion client-side using a pure utility function, with the selected unit stored in URL query state (e.g., `?unit=F`). No server round-trip required for unit switching.

**Rationale**: Satisfies SC-007 (< 500 ms unit switch, no reload). URL state as source of truth satisfies the constitution's navigation requirement. Client-side conversion is stateless and trivially testable.

**Alternatives considered**:
- Server-side conversion on query: adds latency, violates SC-007.
- LocalStorage for unit preference: breaks URL deep-linking, violates constitution URL-state rule.

---

## Decision 7 — OTEL Instrumentation Approach per Service

| Service | Language | Instrumentation approach |
|---|---|---|
| `sapphire-health-service` | Java/Spring Boot | `opentelemetry-spring-boot-starter` auto-instruments HTTP, DB; custom `LongCounter` for FR-025 business metrics via `OpenTelemetry` bean |
| `sapphire-charting-api` | Java/Spring Boot | Same starter; custom `LongCounter` for FR-026 trend query counter |
| `sapphire-bff-api` | Node.js | `@opentelemetry/sdk-node` with auto-instrumentation; trace context forwarded to downstream REST calls via W3C `traceparent` header |
| `sapphire-ui` | React/TypeScript | Browser OTEL via `@opentelemetry/sdk-trace-web` for UI spans; no health data in span attributes |

All services export to `OTEL_EXPORTER_OTLP_ENDPOINT`; `OTEL_SERVICE_NAME` and `OTEL_DEPLOYMENT_ENVIRONMENT` set per container.
