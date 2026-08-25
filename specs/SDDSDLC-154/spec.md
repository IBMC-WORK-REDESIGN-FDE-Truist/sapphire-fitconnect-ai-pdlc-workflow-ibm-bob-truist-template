# Feature Specification: Add Support for Body Temperature Metric Ingestion, Storage, and Reporting

**Feature Branch**: `SDDSDLC-154`
**Created**: 2026-08-20
**Status**: Draft
**Jira Story**: [SDDSDLC-154](https://jsw.ibm.com/browse/SDDSDLC-154)

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Ingest Body Temperature from a Device or API (Priority: P1)

A user who owns a compatible smart device (e.g., a smart thermometer or health wearable) has set up the Sapphire platform to receive their health readings. After taking their temperature, the device or its companion app sends the reading to the platform. The user expects the data to arrive silently and correctly, associated with their account, timestamp, and device. The system must accept single readings and batches (e.g., when a device syncs several days of offline data at once), support both Celsius and Fahrenheit inputs, and reject any value outside a configurable physiological range with a clear error.

**Why this priority**: Without reliable ingestion, no downstream functionality — storage, reporting, or UI — has any data to work with. This is the foundation of the entire feature and the only story that can be developed and tested in complete isolation from the UI layer.

**Independent Test**: Can be fully tested by sending valid and invalid temperature payloads (single and batch, both units) directly to the ingestion endpoint, without any UI interaction, and verifying the responses and stored records.

**Acceptance Scenarios**:

1. **Given** a registered user with a connected smart thermometer, **When** the device submits a single valid temperature reading in Celsius (e.g., 37.1 °C) with a timestamp and device identifier, **Then** the system accepts the record, stores it associated with the user and device, and returns a success confirmation.
2. **Given** a registered user, **When** the device submits a valid temperature reading in Fahrenheit (e.g., 98.6 °F), **Then** the system accepts it and stores it with the unit preserved (or normalised consistently) without data loss.
3. **Given** a registered user, **When** a batch of up to 100 temperature readings is submitted in a single request, **Then** all valid records are stored and a summary response indicates how many were accepted.
4. **Given** a registered user, **When** a temperature value outside the configurable physiological range is submitted (e.g., −10 °C or 60 °C), **Then** the system rejects the record, returns a structured error response identifying the invalid field and reason, and does not persist the record.
5. **Given** an unauthenticated request, **When** a temperature record is submitted to the ingestion endpoint, **Then** the system returns a 401 Unauthorized response and does not store any data.
6. **Given** a batch submission where some records are valid and some are out of range, **Then** the valid records are stored and the response clearly lists which records were rejected and why.

---

### User Story 2 - View Temperature History and Trends in the Dashboard (Priority: P2)

A user opens their health dashboard in the Sapphire web application. They navigate to their metrics list and see body temperature listed alongside their other metrics. Selecting it, they see a trend chart showing their temperature readings over a selectable period (day, week, month). The chart displays the values in their preferred unit, with min, max, and average clearly indicated for the selected range.

**Why this priority**: This is the primary user-visible outcome of the story. It is ranked P2 only because it depends on data being available from P1 ingestion, but it is the highest-value deliverable from the end-user's perspective. It can be developed and demoed against seed or synthetic data without real device integration.

**Independent Test**: Can be fully tested using pre-seeded temperature records for a test user account, loading the dashboard, selecting the temperature metric, and verifying that the chart renders correct aggregated values for each selectable range without needing live device ingestion.

**Acceptance Scenarios**:

1. **Given** a logged-in user who has temperature records stored, **When** they navigate to the health dashboard, **Then** body temperature appears in their metrics list alongside existing metrics (blood pressure, SpO2, etc.).
2. **Given** a logged-in user on the temperature metric view, **When** they select the "Week" range, **Then** the chart displays one aggregated data point per day for the past 7 days, showing min, max, and average temperature values.
3. **Given** a logged-in user, **When** they select "Day", "Week", or "Month" range on the temperature chart, **Then** the chart updates without a full page reload and reflects the correct aggregated values for that range.
4. **Given** a logged-in user, **When** the temperature unit toggle is set to Fahrenheit, **Then** all values on the chart and in data labels are shown in °F; switching back to Celsius updates all values immediately.
5. **Given** a logged-in user with no temperature records yet, **When** they view the temperature metric section, **Then** the chart displays an empty-state message ("No temperature data yet. Connect a device to get started.") rather than a blank or broken chart.
6. **Given** the dashboard is loading temperature data, **When** the data fetch is in progress, **Then** a loading skeleton matching the chart dimensions is shown to prevent layout shift.
7. **Given** a network error while fetching temperature data, **When** the fetch fails, **Then** an error state is shown with a "Try again" action; no stack trace or internal error code is exposed to the user.

---

### User Story 3 - Filter and Export Temperature Data for Healthcare Sharing (Priority: P3)

A user wants to share their temperature history with their healthcare provider. They use the analytics and export functionality to filter temperature data by a specific date range and, optionally, by the device that recorded it. They then export the filtered data in a standard format (e.g., CSV or JSON) to download or share.

**Why this priority**: Sharing health data with providers is a stated user goal in the story. It is ranked P3 because it requires P1 (data ingestion) and P2 (reporting infrastructure) to be complete first, and it extends an existing export capability rather than creating a new one from scratch.

**Independent Test**: Can be fully tested by filtering temperature data for a test user by date range and device, triggering an export, and verifying the downloaded file contains accurate, correctly filtered records.

**Acceptance Scenarios**:

1. **Given** a logged-in user with temperature records from multiple devices over multiple months, **When** they apply a date-range filter (e.g., last 30 days), **Then** the displayed data and any export contain only records within that range.
2. **Given** a logged-in user, **When** they filter by a specific source device, **Then** only readings from that device appear in results and exports.
3. **Given** a logged-in user, **When** they trigger an export of their filtered temperature data, **Then** the exported file includes: value, unit, timestamp, device source, and measurement method (if available) for each record.
4. **Given** an analytics export endpoint, **When** queried for a date range and metric type of "body_temperature", **Then** temperature records are included in the response alongside other supported metrics.
5. **Given** a logged-in user, **When** both date range and device filters are active, **Then** the export reflects the intersection of both filters.

---

### Edge Cases

- What happens when a device submits a temperature reading with a timestamp in the future (clock drift)?
- How does the system handle duplicate records (same user, device, timestamp, and value submitted twice)?
- What if the unit field is missing or contains an unrecognised value — is a default unit assumed, or is the record rejected?
- What happens when a user has thousands of temperature records and selects the "Month" view — are rollup aggregates pre-computed or computed on demand?
- How are temperature readings handled when a user's account is deactivated mid-batch ingestion?
- What if a device ID referenced in a temperature record is not registered to the submitting user's account?
- What happens when the chart date range spans a daylight-saving-time boundary — are timestamps normalised to UTC?

---

## Requirements *(mandatory)*

### Functional Requirements

**Ingestion**

- **FR-001**: The system MUST accept body temperature readings submitted individually (one record per request) and in batches (multiple records per request, up to a configurable maximum batch size).
- **FR-002**: The system MUST accept temperature values expressed in Celsius or Fahrenheit and store the unit alongside the value.
- **FR-003**: The system MUST validate each submitted temperature value against a configurable physiological range (minimum and maximum thresholds). Records outside this range MUST be rejected with a structured error response identifying the invalid value and the acceptable range.
- **FR-004**: Each temperature record MUST be associated with: the authenticated user, the submission timestamp (ISO-8601 UTC), the source device identifier, and the ingestion source (device-push or API).
- **FR-005**: If a measurement method is provided in the submission (e.g., oral, axillary, tympanic), the system MUST store it alongside the record. If absent, the field MUST be stored as null.
- **FR-006**: For batch submissions containing a mix of valid and invalid records, the system MUST store all valid records and return a response listing each rejected record with its reason; the request MUST NOT be treated as all-or-nothing.

**Data Model / Schema**

- **FR-007**: The system MUST add body temperature as a supported metric type in the health metrics catalog, consistent with the schema patterns used by existing metrics (blood pressure, SpO2, activity).
- **FR-008**: The temperature metric schema MUST include: `value` (numeric), `unit` (Celsius or Fahrenheit), `timestamp` (ISO-8601 UTC), `device_source` (string identifier), `ingestion_source` (enum: device-push, API), and `measurement_method` (nullable string).
- **FR-009**: The schema MUST be compatible with the existing downstream analytics pipeline without requiring breaking changes to shared contracts.
- **FR-010**: All API schema changes MUST be reflected in updated internal and external API documentation before the feature is considered complete.

**Storage & Processing**

- **FR-011**: Temperature records MUST be stored with indexing that supports efficient time-series queries (e.g., retrieval of all records for a user within a date range).
- **FR-012**: The system MUST apply the same data retention and archival rules to temperature records as are applied to other health metrics.
- **FR-013**: The system MUST produce pre-aggregated rollups — daily, weekly, and monthly — for each user's temperature data, including: minimum value, maximum value, and average value per period.

**Reporting & Analytics**

- **FR-014**: The system MUST provide a reporting endpoint that returns temperature trend data (min, max, average) for a given user and selectable time range (day, week, month).
- **FR-015**: The reporting endpoint MUST support filtering by date range and by device source.
- **FR-016**: Temperature data MUST be included in the analytics export endpoints that serve other health metrics, using the same response contract format.

**User Interface**

- **FR-017**: Body temperature MUST appear as an entry in the user's health metrics list on the dashboard, alongside existing metrics.
- **FR-018**: A chart component MUST be provided on the temperature metric detail view, displaying temperature trend data with selectable range options: Day, Week, Month.
- **FR-019**: The chart MUST display min, max, and average temperature values for the selected range, in the user's chosen unit.
- **FR-020**: Users MUST be able to switch the displayed unit between Celsius and Fahrenheit; all values on the chart MUST update immediately without a full page reload.
- **FR-021**: The temperature chart MUST display a loading skeleton while data is being fetched, matching the chart's dimensions to prevent layout shift.
- **FR-022**: The temperature chart MUST display an empty-state message when no records exist for the user, and an error state with a retry action when a data fetch fails; stack traces and internal error codes MUST NOT be shown to users.
- **FR-023**: All chart colours and design tokens used for the temperature metric MUST follow the shared design token system and the sapphire-charting-api colour palette for health metric categories.

### Key Entities

- **TemperatureRecord**: A single body temperature measurement. Key attributes: user identifier, value (numeric), unit (Celsius or Fahrenheit), timestamp (UTC), device source identifier, ingestion source, measurement method (nullable). Relates to: User, Device.
- **TemperatureRollup**: A pre-aggregated summary of temperature records for a given user and time bucket (day, week, or month). Key attributes: user identifier, period start, period end, period type, min value, max value, average value, unit. Derived from: TemperatureRecord.
- **MetricType** (extended): The existing catalog of supported health metric types, extended to include body temperature. Used by: ingestion validation, reporting queries, analytics exports.

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Users can view their body temperature history and trend charts within 3 seconds of navigating to the temperature metric section, under normal network conditions.
- **SC-002**: The system successfully ingests 100% of valid single and batch temperature submissions without data loss or misattribution.
- **SC-003**: Invalid temperature values (out-of-range, missing required fields, unrecognised unit) are rejected 100% of the time with a structured error response; no invalid record is persisted.
- **SC-004**: Temperature trend data (min, max, average) is available for the Day, Week, and Month ranges for any user who has at least one stored record.
- **SC-005**: All temperature data exported via the analytics export endpoint matches the stored records for the requested date range and filters, with zero discrepancy.
- **SC-006**: The temperature chart empty state and error state are always displayed in place of a blank or broken UI — 0% of sessions result in a blank chart with no feedback to the user.
- **SC-007**: Unit switching between Celsius and Fahrenheit completes in under 500 milliseconds with no full page reload.
- **SC-008**: All four affected services (ingestion, reporting, BFF, UI) maintain their existing test coverage thresholds: 80% line coverage for the health-service and charting-api, 80% for any Python services, and 70% line coverage for TypeScript/React UI code; domain service classes in Java maintain 100% coverage.

---

## Assumptions

- The platform already has a concept of "metric type" in the health metrics catalog; body temperature is additive and does not require redesigning the existing schema.
- Existing smart-device ingestion infrastructure (auth, routing, device registration) is already in place; this story adds a new metric type to it rather than building new ingestion infrastructure from scratch.
- Temperature unit preference (Celsius vs Fahrenheit) is either already stored as a user preference or can be toggled per-session on the chart; if a global user preference exists it will be respected as the default.
- The physiological validation range (configurable minimum and maximum) will be defined by the product/clinical team before implementation; a reasonable default (e.g., 30–45 °C / 86–113 °F) will be used until confirmed.
- Downstream analytics pipelines consume a documented event/schema contract; schema changes will be coordinated with pipeline owners and documented before merge.
- The `sapphire-charting-api` already provides a charting component abstraction and colour palette for health metrics; the temperature chart will be built as a new instance of that abstraction, not a bespoke implementation.
- Batch ingestion maximum size is configurable server-side; a default of 100 records per batch is assumed unless stated otherwise.
- All timestamps submitted by devices will be in ISO-8601 UTC format; devices are expected to normalise their local time before submission.
- The BFF (GraphQL layer) will expose new queries/mutations for temperature data; breaking changes to existing GraphQL schema are not permitted — only additive changes.
- Rollup aggregates (daily, weekly, monthly) will be computed asynchronously or on a scheduled basis, not synchronously during ingestion, to avoid latency impact on device submissions.
