# Quickstart: SDDSDLC-154 — Body Temperature Feature

**Date**: 2026-08-20 | **Branch**: `SDDSDLC-154`

This guide describes the key integration points for each affected service and how to verify the feature end-to-end in a local development environment.

---

## Affected Services Overview

| Service | Language | Change Summary |
|---|---|---|
| `sapphire-health-service` | Java / Spring Boot | New `TemperatureRecord` entity, ingestion controller, validation, rate limit, OTEL counters |
| `sapphire-charting-api` | Java / Spring Boot | New `TemperatureRollup` entity, trend reporting endpoint, scheduled rollup job, OTEL counter |
| `sapphire-bff-api` | Node.js / GraphQL | Additive schema: `temperatureTrend` query, `ingestTemperatureReadings` mutation, DataLoader |
| `sapphire-ui` | React / TypeScript | New `TemperatureMetricCard` + `TemperatureChart` components, unit toggle, co-located Apollo queries |

---

## sapphire-health-service — Key Integration Points

### New endpoint paths (resource-based, plural nouns per constitution)
```
POST /temperature-readings          # single ingestion
POST /temperature-readings/batch    # batch ingestion
```

### Package structure (feature-by-domain)
```
com.sapphire.health.temperature/
├── TemperatureController.java      # @RestController — delegates only, no business logic
├── TemperatureService.java         # @Service — validation, rate limit, upsert logic
├── TemperatureRepository.java      # @Repository — extends JpaRepository<TemperatureRecord, UUID>
├── TemperatureRecord.java          # @Entity — JPA entity (NOT a record type — mutable for JPA)
├── TemperatureReadingRequest.java  # record DTO — inbound single
├── BatchIngestionRequest.java      # record DTO — inbound batch
├── IngestionResponse.java          # record DTO — outbound
└── TemperatureMetrics.java         # OTEL counter wiring (FR-025)
```

### Constitution compliance checklist for this service
- [x] Controller → Service → Repository layering; no business logic in controller
- [x] Constructor injection only; `@Autowired` field injection forbidden
- [x] `record` types for all DTOs
- [x] `Optional<T>` returned from repository lookups
- [x] `@ControllerAdvice` for global exception handler (422, 429, 401)
- [x] Logback + logstash-logback-encoder for structured JSON logs; no health data in log fields
- [x] `opentelemetry-spring-boot-starter` for auto-instrumentation
- [x] Custom `LongCounter` beans for FR-025 ingestion outcome counters
- [x] Unit tests: JUnit 5 + Mockito; 80% line coverage; domain service = 100% coverage
- [x] Integration tests: `@IntegrationTest` tag; Docker Compose; excluded from validation stage CI

---

## sapphire-charting-api — Key Integration Points

### New endpoint paths
```
GET /metrics/temperature/trend      # trend data (min/max/avg per period)
GET /metrics/export                 # extended — includes BODY_TEMPERATURE
```

### Package structure
```
com.sapphire.charting.temperature/
├── TemperatureTrendController.java
├── TemperatureTrendService.java
├── TemperatureRollupRepository.java
├── TemperatureRollup.java          # @Entity
├── TemperatureTrendResponse.java   # record DTO
├── TemperatureRollupJob.java       # @Scheduled — 15-min cron rollup computation
└── TemperatureReportingMetrics.java # OTEL counter wiring (FR-026)
```

### Rollup job
- Cron: `@Scheduled(cron = "0 */15 * * * *")`
- Reads `TemperatureRecord` rows with `recorded_at > last_watermark`
- Upserts `TemperatureRollup` rows for affected `(user_id, period_type, period_start)` buckets
- Updates watermark on completion
- Idempotent — safe to re-run

---

## sapphire-bff-api — Key Integration Points

### Schema files to add/modify
```
src/schema/temperature.graphql      # new — all temperature types defined here
src/resolvers/temperature/
├── temperatureTrend.resolver.ts    # Query resolver
├── ingestTemperatureReadings.resolver.ts  # Mutation resolver
└── temperatureDataLoader.ts        # DataLoader for batching trend lookups
```

### Resolver pattern
```typescript
// temperatureTrend.resolver.ts
export const temperatureTrendResolver = {
  Query: {
    temperatureTrend: async (_parent, args, context: AppContext) => {
      const userId = context.jwt.sub;          // JWT claim, not caller-supplied
      return context.loaders.temperatureTrend.load({ userId, ...args });
    },
  },
};
```

### Contract tests
- Required: schema snapshot tests must be added for all new types and fields
- Run as part of pre-merge stage, not validation stage

---

## sapphire-ui — Key Integration Points

### Feature directory (co-located per constitution)
```
src/features/temperature/
├── TemperatureMetricCard.tsx       # Dashboard list entry
├── TemperatureChart.tsx            # Main chart component
├── TemperatureChart.test.tsx       # RTL tests
├── useTemperatureTrend.ts          # Custom Apollo query hook
├── useTemperatureTrend.test.ts     # Hook unit tests
├── temperature.types.ts            # TypeScript types
└── temperature.graphql             # Co-located GraphQL operations
```

### Apollo cache policy (explicit, per constitution — no implicit cache-first for health data)
```typescript
// useTemperatureTrend.ts
const { data, loading, error } = useQuery(GetTemperatureTrendDocument, {
  variables: { range, deviceSourceId, from, to },
  fetchPolicy: 'cache-and-network',   // show cached, then refresh
});
```

### Unit/URL state
```typescript
// URL: /dashboard/temperature?range=WEEK&unit=F
const [searchParams, setSearchParams] = useSearchParams();
const unit = (searchParams.get('unit') ?? 'C') as TemperatureUnit;
```

### Three required UI states (constitution §III)
```tsx
if (loading && !data) return <TemperatureChartSkeleton />;
if (error)            return <TemperatureChartError onRetry={refetch} />;
if (!data?.temperatureTrend.dataPoints.length) return <TemperatureChartEmpty />;
return <TemperatureChart data={data.temperatureTrend} unit={unit} />;
```

---

## Local End-to-End Verification Steps

1. **Start services**: `docker compose up sapphire-health-service sapphire-charting-api sapphire-bff-api sapphire-ui`
2. **Authenticate**: obtain JWT from Keycloak local instance.
3. **Ingest test data**: `POST /temperature-readings` with valid payload → expect 200.
4. **Ingest invalid data**: `POST /temperature-readings` with value 60 °C → expect 422 with range error.
5. **Ingest duplicate**: repeat step 3 with same payload → expect 200 (no duplicate row created).
6. **Trigger rollup**: wait for scheduler (or invoke job endpoint directly in dev mode) → rollup rows created.
7. **Query BFF**: GraphQL `temperatureTrend(range: WEEK)` → expect data points returned.
8. **Open UI**: navigate to temperature metric → chart loads; select WEEK/MONTH; toggle unit → verify < 500 ms update.
9. **Export**: `GET /metrics/export?metric_type=BODY_TEMPERATURE&from=...&to=...` → verify CSV/JSON contains correct records.
10. **OTEL**: verify spans and counters appear in Jaeger/Grafana dashboard.
