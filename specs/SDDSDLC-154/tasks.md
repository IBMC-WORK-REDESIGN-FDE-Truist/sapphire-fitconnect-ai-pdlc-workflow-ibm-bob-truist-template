# Tasks: SDDSDLC-154 — Body Temperature Metric Ingestion, Storage, and Reporting

**Input**: Design documents from `specs/SDDSDLC-154/`
**Prerequisites**: plan.md ✅ | spec.md ✅ | research.md ✅ | data-model.md ✅ | contracts/ ✅ | quickstart.md ✅

**Affected repos**: `sapphire-health-service` · `sapphire-charting-api` · `sapphire-bff-api` · `sapphire-ui`
**Child stories**: SDDSDLC-166 (health-service) · SDDSDLC-167 (charting-api) · SDDSDLC-168 (bff-api) · SDDSDLC-169 (ui)

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: DB migrations, shared config, and OTEL env vars — one-time cross-cutting setup before any story work begins.

- [ ] T001 Add `temperature_records` table migration in `sapphire-health-service/src/main/resources/db/migration/V<next>__create_temperature_records.sql` with all fields, unique constraint `(user_id, device_source_id, recorded_at)`, and indexes `idx_temperature_records_user_time`, `idx_temperature_records_device`
- [ ] T002 Add `temperature_rollups` table migration in `sapphire-charting-api/src/main/resources/db/migration/V<next>__create_temperature_rollups.sql` with all fields and unique constraint `(user_id, period_type, period_start)`
- [ ] T003 [P] Add `BODY_TEMPERATURE` entry to the metric type catalog/enum in `sapphire-health-service` (location matching existing `BloodPressure`/`SpO2` enum definition)
- [ ] T004 [P] Add `OTEL_EXPORTER_OTLP_ENDPOINT`, `OTEL_SERVICE_NAME`, `OTEL_DEPLOYMENT_ENVIRONMENT` environment variable entries to container config for all four services (`docker-compose.yml` or equivalent)
- [ ] T005 [P] Add `com.github.ben-manes.caffeine:caffeine` dependency to `sapphire-health-service/pom.xml` (or `build.gradle`) for in-process rate limiting

**Checkpoint**: DB schemas created; metric catalog updated; OTEL env vars configured — ready for story implementation.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core Java entities, DTOs, and the rate-limiter bean that every ingestion task depends on. Must be complete before Phase 3.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete.

- [ ] T006 Create `TemperatureRecord.java` JPA entity in `sapphire-health-service/src/main/java/com/sapphire/health/temperature/TemperatureRecord.java` — fields: `id`, `userId`, `deviceSourceId`, `recordedAt`, `ingestedAt`, `value`, `unit` (enum `TemperatureUnit`), `ingestionSource` (enum `IngestionSource`), `measurementMethod`; unique constraint annotation matching migration
- [ ] T007 [P] Create `TemperatureUnit.java` enum (`CELSIUS`, `FAHRENHEIT`) in `sapphire-health-service/src/main/java/com/sapphire/health/temperature/TemperatureUnit.java`
- [ ] T008 [P] Create `IngestionSource.java` enum (`DEVICE`, `API`) in `sapphire-health-service/src/main/java/com/sapphire/health/temperature/IngestionSource.java`
- [ ] T009 Create `TemperatureReadingRequest.java` record DTO in `sapphire-health-service/src/main/java/com/sapphire/health/temperature/TemperatureReadingRequest.java` — fields matching ingestion contract; Bean Validation annotations (`@NotNull`, `@DecimalMin`, `@DecimalMax`, `@NotBlank`)
- [ ] T010 [P] Create `BatchIngestionRequest.java` record DTO in `sapphire-health-service/src/main/java/com/sapphire/health/temperature/BatchIngestionRequest.java` — wraps `List<TemperatureReadingRequest>` with `@Size(max=100)`
- [ ] T011 [P] Create `IngestionResponse.java` record DTO in `sapphire-health-service/src/main/java/com/sapphire/health/temperature/IngestionResponse.java` — fields: `accepted`, `rejected`, `errors` (`List<IngestionError>`)
- [ ] T012 Create `TemperatureRepository.java` in `sapphire-health-service/src/main/java/com/sapphire/health/temperature/TemperatureRepository.java` — extends `JpaRepository<TemperatureRecord, UUID>`; add custom upsert query using `@Query` with `INSERT … ON CONFLICT DO NOTHING`
- [ ] T013 Create `DeviceRateLimiter.java` `@Component` in `sapphire-health-service/src/main/java/com/sapphire/health/temperature/DeviceRateLimiter.java` — Caffeine `Cache<String, AtomicInteger>` keyed on `deviceSourceId`, 60-second TTL, limit=10; method `checkAndIncrement(deviceId): boolean`
- [ ] T014 [P] Create `TemperatureRollup.java` JPA entity in `sapphire-charting-api/src/main/java/com/sapphire/charting/temperature/TemperatureRollup.java` — fields: `id`, `userId`, `periodType` (enum `PeriodType`), `periodStart`, `periodEnd`, `minCelsius`, `maxCelsius`, `avgCelsius`, `recordCount`, `computedAt`; unique constraint annotation
- [ ] T015 [P] Create `PeriodType.java` enum (`DAY`, `WEEK`, `MONTH`) in `sapphire-charting-api/src/main/java/com/sapphire/charting/temperature/PeriodType.java`
- [ ] T016 [P] Create `TemperatureRollupRepository.java` in `sapphire-charting-api/src/main/java/com/sapphire/charting/temperature/TemperatureRollupRepository.java` — extends `JpaRepository<TemperatureRollup, UUID>`; custom upsert query for rollup computation

**Checkpoint**: All shared entities, DTOs, enums, and infrastructure beans ready — user story phases can proceed.

---

## Phase 3: User Story 1 — Ingest Body Temperature from a Device or API (Priority: P1) 🎯 MVP

**Goal**: Accept single and batch temperature readings with validation, idempotent deduplication, and rate limiting. Returns structured errors for invalid/out-of-range values.

**Independent Test**: POST valid single and batch payloads to `POST /temperature-readings` and `POST /temperature-readings/batch`; verify 200/207 responses, correct DB rows, idempotent re-submission, 422 for out-of-range, 429 for rate limit breach.

### sapphire-health-service

- [ ] T017 [US1] Implement `TemperatureService.java` in `sapphire-health-service/src/main/java/com/sapphire/health/temperature/TemperatureService.java` — constructor-injected `TemperatureRepository`, `DeviceRateLimiter`; methods: `ingestSingle(userId, request): IngestionResponse`, `ingestBatch(userId, requests): IngestionResponse`; enforce rate limit → 429; validate range [30.0–43.0 °C / 86.0–109.4 °F] → 422; call repository upsert; return accept/reject summary
- [ ] T018 [P] [US1] Create `TemperatureMetrics.java` `@Component` in `sapphire-health-service/src/main/java/com/sapphire/health/temperature/TemperatureMetrics.java` — wire `LongCounter` beans for `temperature.ingestion.accepted`, `temperature.ingestion.rejected` (labels: outcome = `accepted`/`rejected-out-of-range`/`rejected-invalid-unit`/`rejected-rate-limited`/`deduplicated`) via injected `OpenTelemetry` bean; call from `TemperatureService`
- [ ] T019 [US1] Implement `TemperatureController.java` `@RestController` in `sapphire-health-service/src/main/java/com/sapphire/health/temperature/TemperatureController.java` — `POST /temperature-readings` (single) and `POST /temperature-readings/batch`; extract userId from JWT principal; delegate to `TemperatureService`; `@ControllerAdvice` maps domain exceptions to 401/422/429 responses; no business logic in controller
- [ ] T020 [P] [US1] Add `TemperatureServiceTest.java` unit test in `sapphire-health-service/src/test/java/com/sapphire/health/temperature/TemperatureServiceTest.java` — JUnit 5 + Mockito; mock `TemperatureRepository` and `DeviceRateLimiter`; AAA structure; cover: valid single, valid batch, out-of-range rejection, rate-limit rejection, idempotent duplicate, partial-batch (207); 100% line coverage on `TemperatureService`
- [ ] T021 [P] [US1] Add `TemperatureControllerTest.java` `@WebMvcTest` slice in `sapphire-health-service/src/test/java/com/sapphire/health/temperature/TemperatureControllerTest.java` — cover: 200, 207, 401, 422, 429 response shapes; mock `TemperatureService`
- [ ] T022 [P] [US1] Add `TemperatureIngestionIT.java` `@IntegrationTest` in `sapphire-health-service/src/test/java/com/sapphire/health/temperature/TemperatureIngestionIT.java` — Docker Compose; test full ingestion round-trip including DB persistence, idempotency constraint, and rate limit counter reset

**Checkpoint (US1)**: `POST /temperature-readings[/batch]` fully functional — valid records stored, invalid rejected with structured errors, duplicates idempotent, rate limit enforced, OTEL counters emitting.

---

## Phase 4: User Story 2 — View Temperature History and Trends in the Dashboard (Priority: P2)

**Goal**: Expose pre-aggregated trend data via REST (charting-api), surface it via GraphQL (bff-api), and render a React chart with loading/empty/error states and unit toggle in the UI.

**Independent Test**: Seed temperature records for a test user; verify `GET /metrics/temperature/trend?range=WEEK` returns correct aggregated data points; query BFF `temperatureTrend(range: WEEK)` and verify GraphQL response; open UI dashboard, select temperature metric, switch range and unit — all without live device ingestion.

### sapphire-charting-api

- [ ] T023 [US2] Implement `TemperatureTrendService.java` in `sapphire-charting-api/src/main/java/com/sapphire/charting/temperature/TemperatureTrendService.java` — constructor-injected `TemperatureRollupRepository`; method `getTrend(userId, range, from, to, deviceSourceId): TemperatureTrendResponse`; returns rollup data points; handles empty result (returns empty list, not null)
- [ ] T024 [P] [US2] Implement `TemperatureRollupJob.java` `@Component` in `sapphire-charting-api/src/main/java/com/sapphire/charting/temperature/TemperatureRollupJob.java` — `@Scheduled(cron = "0 */15 * * * *")`; reads `TemperatureRecord` rows from `sapphire-health-service` DB (or shared DB) where `recorded_at > watermark`; upserts `TemperatureRollup` rows for DAY/WEEK/MONTH buckets; updates watermark; idempotent
- [ ] T025 [P] [US2] Create `TemperatureTrendResponse.java` record DTO and `TemperatureDataPoint.java` record in `sapphire-charting-api/src/main/java/com/sapphire/charting/temperature/` — matching REST contract shape in `contracts/rest-charting-api.md`
- [ ] T026 [P] [US2] Create `TemperatureReportingMetrics.java` `@Component` in `sapphire-charting-api/src/main/java/com/sapphire/charting/temperature/TemperatureReportingMetrics.java` — `LongCounter` for `temperature.trend.queries` labelled by `range` (DAY/WEEK/MONTH); call from `TemperatureTrendService`
- [ ] T027 [US2] Implement `TemperatureTrendController.java` `@RestController` in `sapphire-charting-api/src/main/java/com/sapphire/charting/temperature/TemperatureTrendController.java` — `GET /metrics/temperature/trend` with query params `range`, `deviceSourceId`, `from`, `to`; validate `range` enum; delegate to `TemperatureTrendService`; return 200 with data or 200 with empty `dataPoints` list
- [ ] T028 [P] [US2] Add `TemperatureTrendServiceTest.java` unit test in `sapphire-charting-api/src/test/java/com/sapphire/charting/temperature/TemperatureTrendServiceTest.java` — mock `TemperatureRollupRepository`; cover: populated result, empty result, date-range filter, deviceSource filter; 100% coverage on `TemperatureTrendService`

### sapphire-bff-api

- [ ] T029 [US2] Create `src/schema/temperature.graphql` in `sapphire-bff-api` — add all new types from `contracts/graphql-bff-api.md`: `TemperatureUnit`, `TemperatureRange`, `IngestionSource`, `TemperatureRecord`, `TemperatureDataPoint`, `TemperatureTrendPayload`, input types, payload types; extend `Query` and `Mutation`
- [ ] T030 [P] [US2] Create `src/resolvers/temperature/temperatureDataLoader.ts` in `sapphire-bff-api` — `DataLoader<{userId, range, from, to, deviceSourceId}, TemperatureTrendPayload>` batching calls to `GET /metrics/temperature/trend`
- [ ] T031 [P] [US2] Implement `src/resolvers/temperature/temperatureTrend.resolver.ts` in `sapphire-bff-api` — `Query.temperatureTrend`: extract `userId` from `context.jwt.sub`; load via `context.loaders.temperatureTrend`; validate JWT before delegation; type `context` as `AppContext` (no `any`)
- [ ] T032 [P] [US2] Add `src/schema/__tests__/temperature.contract.test.ts` in `sapphire-bff-api` — schema snapshot test for all new types and fields; run in pre-merge stage

### sapphire-ui

- [ ] T033 [US2] Create `src/features/temperature/temperature.types.ts` in `sapphire-ui` — TypeScript interfaces for `TemperatureUnit`, `TemperatureRange`, `TemperatureDataPoint`, `TemperatureTrendPayload`; no `any`
- [ ] T034 [P] [US2] Create `src/features/temperature/temperature.graphql` in `sapphire-ui` — named `GetTemperatureTrend` query using `temperatureTrend` with variables `$range`, `$deviceSourceId`, `$from`, `$to`; include fragment for `TemperatureDataPoint`
- [ ] T035 [P] [US2] Create `src/features/temperature/useTemperatureTrend.ts` custom hook in `sapphire-ui` — wraps `useQuery(GetTemperatureTrendDocument)` with `fetchPolicy: 'cache-and-network'`; reads `range` and `unit` from `useSearchParams()`; returns `{ data, loading, error, refetch }`
- [ ] T036 [P] [US2] Create temperature conversion utility `src/features/temperature/temperatureUtils.ts` in `sapphire-ui` — pure functions `celsiusToFahrenheit(c: number): number` and `fahrenheitToCelsius(f: number): number`; exported for unit tests
- [ ] T037 [US2] Create `src/features/temperature/TemperatureChartSkeleton.tsx`, `TemperatureChartEmpty.tsx`, `TemperatureChartError.tsx` components in `sapphire-ui` — skeleton uses design token dimensions matching chart; empty renders "No temperature data yet. Connect a device to get started."; error renders "Try again" button calling `onRetry` prop
- [ ] T038 [US2] Create `src/features/temperature/TemperatureChart.tsx` in `sapphire-ui` — functional component; props: `data: TemperatureTrendPayload`, `unit: TemperatureUnit`; renders sapphire-charting-api chart instance with min/max/avg series; converts values client-side using `temperatureUtils`; uses design tokens and shared colour palette; no hardcoded hex values
- [ ] T039 [US2] Create `src/features/temperature/TemperatureMetricCard.tsx` in `sapphire-ui` — dashboard list entry component; renders loading skeleton / error / empty / chart states driven by `useTemperatureTrend`; range selector (Day/Week/Month) and unit toggle update URL query params via `setSearchParams`; all three UI states required by constitution
- [ ] T040 [P] [US2] Add `src/features/temperature/useTemperatureTrend.test.ts` unit test in `sapphire-ui` — React Testing Library; mock Apollo client; cover: loading state, data state, error state, URL param read for range and unit
- [ ] T041 [P] [US2] Add `src/features/temperature/TemperatureChart.test.tsx` unit test in `sapphire-ui` — RTL; cover: renders data points, Celsius/Fahrenheit conversion, empty state, skeleton, error with retry button

**Checkpoint (US2)**: Dashboard shows temperature metric card with trend chart; range selector and unit toggle work client-side; loading/error/empty states all render; BFF GraphQL query returns correct data.

---

## Phase 5: User Story 3 — Filter and Export Temperature Data for Healthcare Sharing (Priority: P3)

**Goal**: Extend the analytics export endpoint in `sapphire-charting-api` to include `BODY_TEMPERATURE` records, supporting date-range and device-source filters.

**Independent Test**: Call `GET /metrics/export?metric_type=BODY_TEMPERATURE&from=...&to=...` with and without `device_source_id` filter; verify the response contains correct records matching the intersection of all active filters.

### sapphire-charting-api

- [ ] T042 [US3] Extend the existing analytics export service in `sapphire-charting-api` (file path matching existing export service class) to include `TemperatureRecord` data when `metric_type=BODY_TEMPERATURE` or when no metric type filter is applied; apply `from`, `to`, and `deviceSourceId` filters; map to existing export response format
- [ ] T043 [P] [US3] Extend the existing export controller in `sapphire-charting-api` to accept `metric_type=BODY_TEMPERATURE` as a valid query parameter value; no new endpoint — additive only
- [ ] T044 [P] [US3] Add export filter unit test in `sapphire-charting-api/src/test/java/com/sapphire/charting/temperature/TemperatureExportServiceTest.java` — cover: date-range filter only, device-source filter only, combined filters, empty result, all-metrics export includes temperature

**Checkpoint (US3)**: `GET /metrics/export` returns temperature records correctly filtered; all combinations of date-range and device-source filters produce the correct intersection.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Documentation, integration wiring, quickstart validation, and ensuring all Javadoc/JSDoc is in place.

- [ ] T045 [P] Add Javadoc comments to all public methods in `TemperatureService`, `TemperatureController`, `DeviceRateLimiter`, `TemperatureTrendService`, `TemperatureTrendController`, `TemperatureRollupJob` — intent-focused, not implementation-focused
- [ ] T046 [P] Add JSDoc to `useTemperatureTrend.ts`, `temperatureUtils.ts`, and `TemperatureChart.tsx` exported functions and props interfaces in `sapphire-ui`
- [ ] T047 [P] Verify `mvn -q -DskipTests compile` (or `./gradlew compileJava`) passes with no warnings in `sapphire-health-service` and `sapphire-charting-api`
- [ ] T048 [P] Verify `tsc --noEmit` passes with no errors in `sapphire-ui`
- [ ] T049 [P] Run unit test coverage gates: `TemperatureService` = 100% line coverage; all other Java classes ≥ 80%; `sapphire-ui` features ≥ 70%
- [ ] T050 [P] Update `specs/SDDSDLC-154/contracts/graphql-bff-api.md` if any schema fields were adjusted during implementation; confirm schema snapshot test passes
- [ ] T051 Run end-to-end quickstart validation per `specs/SDDSDLC-154/quickstart.md` steps 1–10; confirm all services respond correctly and OTEL spans appear in collector

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 (Setup)**: No dependencies — start immediately
- **Phase 2 (Foundational)**: Depends on Phase 1 — **BLOCKS** Phases 3, 4, 5
- **Phase 3 (US1)**: Depends on Phase 2 — can run independently once Phase 2 complete
- **Phase 4 (US2)**: Depends on Phase 2; integrates with Phase 3 output (rollup job reads `temperature_records`) — start Phase 4 charting-api tasks after T022 (DB rows verifiable)
- **Phase 5 (US3)**: Depends on Phase 2; extends Phase 4 charting-api export — start after T027
- **Phase 6 (Polish)**: Depends on Phases 3–5 complete

### User Story Dependencies

- **US1 (P1)**: Only depends on Foundational. All tasks within `sapphire-health-service`.
- **US2 (P2)**: Charting-api tasks (T023–T028) can start after Phase 2. BFF tasks (T029–T032) can start in parallel once `temperature.graphql` contract is stable. UI tasks (T033–T041) can start after T029 (schema defined). Rollup job (T024) requires `temperature_records` rows to be present for testing.
- **US3 (P3)**: Extends existing export service; only needs `TemperatureRecord` readable by charting-api (after T012, T022).

### Parallel Opportunities

- T007, T008, T009, T010, T011 — all DTOs/enums in Phase 2 are independent files, run in parallel
- T014, T015, T016 — charting-api entities, parallel
- T018, T020, T021, T022 — OTEL wiring and tests for US1 are parallel after T017
- T024, T025, T026, T028 — charting-api US2 support components parallel after T023
- T029 blocks T030, T031 (schema must exist first); T030 and T031 are then parallel
- T033–T037 UI utility/state components are all parallel; T038–T039 depend on T037
- T040, T041 UI tests are parallel with or after their respective implementations
- T045–T050 polish tasks are all independent, fully parallel

---

## Parallel Example: User Story 1 (sapphire-health-service)

```
# After Phase 2 complete:
Parallel group A (independent files):
  T017 → TemperatureService.java
  T018 → TemperatureMetrics.java

# After T017:
  T019 → TemperatureController.java

# After T017 (parallel):
  T020 → TemperatureServiceTest.java
  T021 → TemperatureControllerTest.java
  T022 → TemperatureIngestionIT.java
```

## Parallel Example: User Story 2 (multi-repo)

```
# After Phase 2 (T012, T016 complete):
Parallel group — three repos simultaneously:
  sapphire-charting-api: T023 → T024 → T027
  sapphire-bff-api:      T029 → T030 + T031 (parallel) → T032
  sapphire-ui:           T033 + T034 + T035 + T036 + T037 (parallel) → T038 → T039
```

---

## Implementation Strategy

### MVP First (User Story 1 Only — SDDSDLC-166)

1. Complete Phase 1: Setup (T001–T005)
2. Complete Phase 2: Foundational (T006–T016) — **CRITICAL**
3. Complete Phase 3: US1 (T017–T022)
4. **STOP and VALIDATE**: `POST /temperature-readings` and batch endpoint working end-to-end with Docker Compose
5. Demo: device can submit temperature, records stored correctly, errors returned properly

### Incremental Delivery

1. Setup + Foundational → foundation ready
2. US1 (health-service) → ingestion MVP — deploy/demo
3. US2 (charting-api + bff-api + ui) → dashboard chart visible — deploy/demo
4. US3 (export extension) → healthcare sharing unlocked — deploy/demo

### Parallel Team Strategy (4 developers after Phase 2)

| Developer | Story | Repo |
|---|---|---|
| Dev A | US1 | `sapphire-health-service` (SDDSDLC-166) |
| Dev B | US2 backend | `sapphire-charting-api` (SDDSDLC-167) |
| Dev C | US2 BFF | `sapphire-bff-api` (SDDSDLC-168) |
| Dev D | US2 UI | `sapphire-ui` (SDDSDLC-169) |

US3 picked up by Dev B after US2 backend complete (T042–T044 extend existing export service).

---

## Notes

- `[P]` = independent file, no dependency on an incomplete parallel task — safe to run simultaneously
- `[US1]/[US2]/[US3]` labels map tasks to child Jira stories SDDSDLC-166/167/168/169 respectively
- All Java `@IntegrationTest` tasks run in Docker Compose pre-merge stage only — excluded from validation CI
- Constitution coverage gates: `TemperatureService` and `TemperatureTrendService` require **100%** line coverage; all other Java classes ≥ **80%**; UI ≥ **70%**
- Health data (temperature values) MUST NOT appear in any log field — verified in T051 quickstart run
- Commit after each logical group; each phase checkpoint is a valid demo/deploy point
