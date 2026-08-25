# GraphQL Contract: sapphire-bff-api — Temperature Schema Extension

**Feature**: SDDSDLC-154
**Service**: `sapphire-bff-api`
**Change type**: Additive only — no existing fields removed or renamed

---

## New Types

```graphql
# ---- Enums ----

enum TemperatureUnit {
  CELSIUS
  FAHRENHEIT
}

enum TemperatureRange {
  DAY
  WEEK
  MONTH
}

enum IngestionSource {
  DEVICE
  API
}

# ---- Core Types ----

type TemperatureRecord {
  id: ID!
  userId: ID!
  deviceSourceId: String!
  recordedAt: DateTime!
  value: Float!
  unit: TemperatureUnit!
  ingestionSource: IngestionSource!
  measurementMethod: String
}

type TemperatureDataPoint {
  periodStart: String!   # ISO-8601 date (YYYY-MM-DD)
  periodEnd: String!
  min: Float!
  max: Float!
  avg: Float!
  count: Int!
}

type TemperatureTrendPayload {
  userId: ID!
  range: TemperatureRange!
  unit: TemperatureUnit!
  dataPoints: [TemperatureDataPoint!]!
}

# ---- Input Types ----

input TemperatureReadingInput {
  value: Float!
  unit: TemperatureUnit!
  recordedAt: DateTime!
  deviceSourceId: String!
  ingestionSource: IngestionSource!
  measurementMethod: String
}

input IngestTemperatureInput {
  readings: [TemperatureReadingInput!]!
}

# ---- Payload Types (mutations return payloads, not raw entities) ----

type IngestTemperatureResult {
  index: Int!
  reason: String!
  value: Float!
  unit: TemperatureUnit!
}

type IngestTemperaturePayload {
  accepted: Int!
  rejected: Int!
  errors: [IngestTemperatureResult!]!
}
```

---

## New Queries

```graphql
extend type Query {
  """
  Returns pre-aggregated temperature trend data for the authenticated user.
  Resolves via sapphire-charting-api REST GET /metrics/temperature/trend.
  DataLoader batches per-user rollup lookups to prevent N+1.
  """
  temperatureTrend(
    range: TemperatureRange!
    deviceSourceId: String
    from: String
    to: String
  ): TemperatureTrendPayload!
}
```

---

## New Mutations

```graphql
extend type Mutation {
  """
  Ingests one or more temperature readings for the authenticated user.
  Delegates to sapphire-health-service REST POST /temperature-readings/batch.
  JWT claims validated before delegation.
  """
  ingestTemperatureReadings(input: IngestTemperatureInput!): IngestTemperaturePayload!
}
```

---

## Resolver Behaviour Notes

- All resolvers MUST validate JWT claims (`sub` → `userId`) before delegating to backend REST calls.
- `temperatureTrend` resolver uses a `DataLoader` keyed on `(userId, range)` to batch requests if called from multiple places within a single request.
- `ingestTemperatureReadings` passes the authenticated `userId` from JWT claims — callers cannot inject a different user ID.
- No breaking changes: existing `Query` and `Mutation` types are only extended, not modified.
- Contract tests required: any schema change must be verified against all known consumers before merge (constitution §II).
