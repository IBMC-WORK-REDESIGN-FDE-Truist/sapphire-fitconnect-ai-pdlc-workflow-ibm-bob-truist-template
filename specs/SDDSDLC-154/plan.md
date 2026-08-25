# Implementation Plan: SDDSDLC-154 — Body Temperature Metric Ingestion, Storage, and Reporting

**Branch**: `SDDSDLC-154` | **Date**: 2026-08-20 | **Spec**: [spec.md](spec.md)

---

## Summary

Add body temperature as a first-class health metric across four services. The `sapphire-health-service` receives and stores individual and batch temperature readings with physiological validation, idempotent deduplication, and device-level rate limiting. The `sapphire-charting-api` pre-aggregates records into daily/weekly/monthly rollups via a scheduled job and exposes a trend reporting endpoint and analytics export extension. The `sapphire-bff-api` surfaces both via additive GraphQL queries and mutations with JWT-validated resolvers. The `sapphire-ui` adds a co-located React feature directory with a temperature metric card, trend chart, unit toggle (URL state), and all three required UI states (loading skeleton, error boundary, empty state). Platform-standard OTEL instrumentation applied across all four services; health data excluded from logs.

---

## Technical Context

**Language/Version**: Java 21 / Spring Boot 3.x (`sapphire-health-service`, `sapphire-charting-api`); Node.js 20 / GraphQL (`sapphire-bff-api`); TypeScript / React 18 (`sapphire-ui`)
**Primary Dependencies**: Spring Data JPA, Hibernate, Caffeine (rate limiting), `opentelemetry-spring-boot-starter`; Apollo Server, DataLoader; Apollo Client 3, React Testing Library
**Storage**: PostgreSQL — existing instance; new tables `temperature_records`, `temperature_rollups`
**Testing**: JUnit 5 + Mockito (Java); Jest + React Testing Library (UI); contract snapshot tests (BFF)
**Target Platform**: Linux container (all services); browser (UI)
**Project Type**: Multi-service web application
**Performance Goals**: Dashboard chart load < 3 s (SC-001); unit switch < 500 ms (SC-007); ingestion responds within normal HTTP latency (no synchronous rollup)
**Constraints**: Additive-only GraphQL changes; no breaking changes to existing REST contracts; health data must not appear in logs; rate limit 10 req/min per device
**Scale/Scope**: 4 repos; 2 new DB tables; 3 new REST endpoints; 2 new GraphQL operations; 1 React feature directory

---

## Constitution Check

| # | Gate | Principle | Status |
|---|------|-----------|--------|
| 1 | All public functions/methods/classes have docstrings or Javadoc (intent, not implementation) | I. Code Quality | ✅ Planned — Javadoc on all Java service/controller/repo methods; JSDoc on TS hooks |
| 2 | No magic numbers or strings — named constants or enums used | I. Code Quality | ✅ Planned — `TemperatureUnit`, `TemperatureRange`, `IngestionSource` enums; threshold constants in config |
| 3 | Cyclomatic complexity ≤ 10 per function/method confirmed via static analysis | I. Code Quality | ✅ Planned — CI static analysis gate; batch validation loop extracted to named method |
| 4 | No commented-out code committed; feature flags or deletion used instead | I. Code Quality | ✅ Enforced by PR review |
| 5 | Stack-specific rules applied (Spring layering / PEP 8 + ruff / strict TS / BFF DataLoader) | I. Code Quality | ✅ Controller→Service→Repository in Java; strict TS in UI; DataLoader in BFF resolver |
| 6 | Coverage gates planned: Java 80%/100% domain, Python 80%, TS/React 70%, BFF resolvers 100% | II. Testing Standards | ✅ `TemperatureService` = 100%; controller/repo = 80% min; UI = 70% min |
| 7 | Contract tests planned for all GraphQL schema changes and Kafka event schema changes | II. Testing Standards | ✅ BFF schema snapshot tests added for all new types and fields |
| 8 | Test pyramid respected: unit (mocked I/O), integration (Docker Compose, pre-merge only), E2E | II. Testing Standards | ✅ Unit tests mock all I/O; integration tests tagged `@IntegrationTest`; E2E in `sapphire-playwright` |
| 9 | All data-fetching components handle loading skeleton / error boundary / empty state | III. UX Consistency | ✅ `TemperatureChart` has all three states; skeleton matches chart dimensions |
| 10 | Auth path is exclusively Keycloak OIDC/PKCE; no bypass routes in any environment | III. UX Consistency | ✅ JWT from Keycloak validated in BFF before any backend call; 401 returned on failure |
| 11 | URL state is source of truth for filters, pagination, and selections | III. UX Consistency | ✅ `range` and `unit` stored in URL query params; deep-linkable |
| 12 | Apollo cache policies explicit; no implicit cache-first for mutable health data | I. Code Quality | ✅ `fetchPolicy: 'cache-and-network'` on `useTemperatureTrend` hook |
| 13 | All services emit structured JSON logs with trace_id and span_id | IV. Observability | ✅ Logback+logstash-logback-encoder (Java); pino (Node.js); no health data in log fields |
| 14 | OTEL metrics: request count, duration, error rate, in-flight + custom business metrics | IV. Observability | ✅ Auto-instrumented via starters; FR-025/FR-026 custom counters added |
| 15 | Distributed traces via OTEL SDK; W3C traceparent propagation | IV. Observability | ✅ Auto-instrumented; BFF forwards `traceparent` header to REST backends |
| 16 | OTEL env vars set in every container | IV. Observability | ✅ `OTEL_EXPORTER_OTLP_ENDPOINT`, `OTEL_SERVICE_NAME`, `OTEL_DEPLOYMENT_ENVIRONMENT` in all container configs |
| 17 | *(LangGraph only)* — not applicable | I. Code Quality | N/A |

---

## Project Structure

### Documentation (this feature)

```text
specs/SDDSDLC-154/
├── plan.md              ← this file
├── research.md          ← Phase 0 output
├── data-model.md        ← Phase 1 output
├── quickstart.md        ← Phase 1 output
├── contracts/
│   ├── rest-health-service.md    ← REST ingestion API contract
│   ├── rest-charting-api.md      ← REST reporting/export API contract
│   └── graphql-bff-api.md        ← GraphQL schema extension contract
└── tasks.md             ← Phase 2 output (/speckit.tasks — not yet created)
```

### Source Code Layout (per affected repo)

**sapphire-health-service**
```text
src/main/java/com/sapphire/health/temperature/
├── TemperatureController.java
├── TemperatureService.java
├── TemperatureRepository.java
├── TemperatureRecord.java         # @Entity
├── TemperatureReadingRequest.java # record DTO
├── BatchIngestionRequest.java     # record DTO
├── IngestionResponse.java         # record DTO
└── TemperatureMetrics.java        # OTEL counters

src/test/java/com/sapphire/health/temperature/
├── TemperatureServiceTest.java    # unit
├── TemperatureControllerTest.java # @WebMvcTest slice
└── TemperatureIngestionIT.java    # @IntegrationTest
```

**sapphire-charting-api**
```text
src/main/java/com/sapphire/charting/temperature/
├── TemperatureTrendController.java
├── TemperatureTrendService.java
├── TemperatureRollupRepository.java
├── TemperatureRollup.java         # @Entity
├── TemperatureTrendResponse.java  # record DTO
├── TemperatureRollupJob.java      # @Scheduled
└── TemperatureReportingMetrics.java # OTEL counters
```

**sapphire-bff-api**
```text
src/schema/temperature.graphql
src/resolvers/temperature/
├── temperatureTrend.resolver.ts
├── ingestTemperatureReadings.resolver.ts
└── temperatureDataLoader.ts
src/schema/__tests__/temperature.contract.test.ts
```

**sapphire-ui**
```text
src/features/temperature/
├── TemperatureMetricCard.tsx
├── TemperatureChart.tsx
├── TemperatureChart.test.tsx
├── TemperatureChartSkeleton.tsx
├── TemperatureChartError.tsx
├── TemperatureChartEmpty.tsx
├── useTemperatureTrend.ts
├── useTemperatureTrend.test.ts
├── temperature.types.ts
└── temperature.graphql
```

---

## Complexity Tracking

No constitution violations requiring justification. All gates pass as planned.
