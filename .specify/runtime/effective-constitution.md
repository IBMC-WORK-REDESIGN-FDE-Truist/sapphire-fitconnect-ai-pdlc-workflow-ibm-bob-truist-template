<!-- EFFECTIVE CONSTITUTION
     Generated  : 2026-09-07T10:00:00Z
     Global     : org-constitution/constitution.md (local path)
     Local      : .specify/memory/constitution.md
     Precedence : local-over-global
     If confused, give precedence to the local constitution.
-->

# Effective Constitution

**Generated:** 2026-09-07T10:00:00Z
**Precedence:** Local constitution overrides global on conflict.
**Conflict report:** `.specify/runtime/effective-constitution-report.md`

> If a rule in Part 1 conflicts with a rule in Part 2, Part 2 wins — always.

---

## Resolved Rules — Authoritative Reference

> **Read this section first.** These are the final, binding rules for every
> topic where global and local conflict. Agents MUST apply these rules.
> Do not apply the corresponding Part 1 rule for any topic listed here.

| # | Topic | Authoritative Rule (local wins) |
|---|---|---|
| 1 | Error handling scope | Silent error swallowing is FORBIDDEN in **all languages**. Empty `catch` blocks and bare `except` clauses with no action are FORBIDDEN everywhere — not only in Python. Either handle the error explicitly with a logged, structured error event, or let it propagate to the nearest boundary handler. |

---

## PART 1 — Global Baseline

> Source: `org-constitution/constitution.md` (local path)
> Rules superseded by Part 2 are documented in the conflict report.

# Constitution

## Core Principles

### I. Code Quality

All public functions, methods, and classes MUST have a docstring or Javadoc comment
explaining intent, not implementation. No magic numbers or strings — use named constants
or enums. Functions MUST do one thing; if a block inside a function needs a comment to
explain what it does, that block MUST be extracted into a named function or method.

Cyclomatic complexity MUST NOT exceed 10 per function/method, enforced via static analysis
in CI. Commented-out code MUST NOT appear in any commit; use feature flags or delete it.

**Java / Spring Boot**

- Follow standard Spring layering: Controller → Service → Repository. Business logic in
  controllers or repositories is FORBIDDEN.
- Use `record` types for immutable DTOs. Use `sealed interface` for discriminated domain
  types.
- Prefer constructor injection. Field injection via `@Autowired` is FORBIDDEN in
  non-test production code.
- Define domain-specific checked exceptions for recoverable errors. Unchecked exceptions
  are reserved for programming errors only.
- `Optional<T>` MUST be returned for nullable values. Returning `null` from any public
  method is FORBIDDEN.
- Java compilation MUST pass before merge (for example, `mvn -q -DskipTests compile` or
  `./gradlew compileJava` in the affected repo).

**Python / FastAPI**

- Follow PEP 8. `ruff` MUST be used for linting and formatting (line length 100).
- Pydantic v2 models MUST be used for all request/response schemas and internal data
  contracts.
- All FastAPI route handlers MUST be `async`. `httpx` MUST be used for async HTTP calls;
  the `requests` library is FORBIDDEN in service code.
- Every function signature MUST be fully type-annotated including return types. Bare
  `except:` clauses are FORBIDDEN; catch specific exception types only.
- Shared resources (DB sessions, HTTP clients, configuration) MUST be injected via
  FastAPI `Depends`.
- Python compilation checks MUST pass before merge (for example,
  `python -m compileall -q .` in the affected repo).

**REST API Contracts (all services)**

- All REST API contracts MUST be resource-based. Endpoint paths MUST use plural
  resource nouns (for example, `/users`, `/orders/{orderId}/items`) and MUST NOT
  use verb-style action paths (for example, `/createUser`, `/getOrders`,
  `/calculateScore`).
- CRUD semantics MUST map to standard HTTP methods (`GET`, `POST`, `PUT`, `PATCH`,
  `DELETE`) on resources. RPC-style action tunneling over REST paths is FORBIDDEN
  unless explicitly approved in the feature specification with documented rationale.
- Resource identifiers MUST be path parameters and filtering/pagination inputs MUST
  be query parameters. Request bodies for `GET` endpoints are FORBIDDEN.
- REST contract artifacts (OpenAPI specs, endpoint docs, and tests) MUST reflect the
  same resource model and naming conventions as implemented routes.

**LangGraph / AI Agents (sapphire-wellness-agent, sapphire-wellness-coach)**

- Graph state MUST be defined as a `TypedDict` with fully `Annotated` fields specifying
  the appropriate reducer (e.g., `operator.add` for append-only lists). Raw `dict` state
  is FORBIDDEN.
- Each graph node MUST be a single-responsibility `async` function accepting and
  returning state. Node functions MUST NOT perform DB or HTTP I/O without injected async
  clients (passed via `RunnableConfig` extras or constructor-injected dependencies).
- Conditional routing MUST use named routing functions returning string literals that
  match registered edge targets. Anonymous `lambda` routing callables are FORBIDDEN.
- All production LangGraph agents MUST use a persistent checkpointer
  (`AsyncPostgresSaver` or equivalent). `MemorySaver` is permitted in unit tests and
  local development only.
- Tool definitions MUST use Pydantic v2 `BaseModel` subclasses for their input schema.
  Bare `dict` or untyped `args_schema` is FORBIDDEN.
- Graphs MUST be compiled once at application startup and reused across requests.
  Compiling a new `StateGraph` per request is FORBIDDEN.
- Human-in-the-loop pauses MUST be implemented using LangGraph's `interrupt_before` or
  `interrupt_after` mechanism. Polling loops or arbitrary `asyncio.sleep` for approval
  waiting are FORBIDDEN.
- Node failures MUST be caught and encoded into a typed `error` field in the state.
  Unhandled exceptions that propagate out of a node and crash the graph are FORBIDDEN.
- LangGraph agent execution MUST emit trace data via LangSmith or an OTEL-compatible
  callback. Silent graph execution without observable trace output is FORBIDDEN in
  non-local environments.
- Unit tests MUST test each node in isolation with minimal constructed state dicts.
  Integration tests MUST run the compiled graph end-to-end using `MemorySaver` with
  deterministic LLM stubs or recorded cassettes.

**TypeScript / React (Sapphire UI)**

- Strict TypeScript MUST be enabled project-wide. `any` is FORBIDDEN. `ts-ignore`
  requires an explanatory comment and a linked ticket reference.
- Class components are FORBIDDEN; use functional components only.
- Feature code MUST be co-located: one directory per feature containing the component,
  its hook, its types, and its tests.
- Apollo cache policies MUST be explicit on every query. Implicit `cache-first` for
  mutable health data is FORBIDDEN.
- Raw `fetch` calls inside components are FORBIDDEN. All API interaction MUST go through
  the Apollo client or a typed service module.
- TypeScript compilation MUST pass before merge (for example, `tsc --noEmit` or the repo's equivalent compile script).

**Node.js / BFF (sapphire-bff-api)**

- All GraphQL resolvers MUST validate JWT claims before delegating to backend REST calls.
- Inline SQL and raw REST URLs are FORBIDDEN. Use typed resolver helpers and
  environment-configured service clients.
- `DataLoader` MUST be used for any field resolver that could trigger N+1 calls.


**Version**: 2.2.0 | **Ratified**: 2026-03-21 | **Last Amended**: 2026-04-02

---

## PART 2 — Local Constitution (Authoritative)

> Source: `.specify/memory/constitution.md`
> Rules here take precedence over Part 1 wherever a conflict exists.

# Sapphire FitConnect Constitution
<!-- Local project constitution for the Sapphire FitConnect AI PDLC workspace.
     Global constitution source: org-constitution/constitution.md
     This file adds project-specific rules that extend (never override) the global.
     Generated by: /speckit.constitution
-->

## Core Principles

### I. Documentation & Audit Trail (NON-NEGOTIABLE)

All new features MUST be documented in markdown format before implementation begins.
Documentation MUST include:

- A feature specification (`spec.md`) describing the problem, acceptance criteria, and
  affected repos.
- An implementation plan (`plan.md`) covering tech stack decisions, data model changes,
  and API contracts.
- A tasks file (`tasks.md`) with dependency-ordered, repo-labelled tasks.

Every merged PR MUST reference its parent Jira story key in the commit message title.
Silent, undocumented changes to any shared schema (GraphQL, Avro, OpenAPI, database) are
FORBIDDEN.

### II. Brownfield-First Development

The Sapphire codebase is an established multi-repo system. All changes MUST:

- Understand and follow existing patterns in the target repo before introducing anything
  new. Deviating from existing patterns MUST be explicitly justified in the spec.
- Minimize the diff — make the smallest change that satisfies the acceptance criteria.
  Opportunistic refactoring outside the story scope is FORBIDDEN.
- Trace the full call path (entry point → data layer) before touching any file. Do not
  assume behavior from naming alone.
- Prefer additive changes over modifications. Deletions of existing public APIs, GraphQL
  fields, Avro schema fields, or Kafka topic schemas MUST be treated as breaking changes
  and require a deprecation period documented in the spec.

### III. Cross-Repo Orchestration

Sapphire features frequently span multiple repos. The following rules apply to all
cross-repo work:

- The PDLC orchestrator repo (`sapphire-fitconnect-ai-pdlc-workflow-ibm-bob-template`)
  owns the canonical spec, plan, tasks, and workflow state for every story.
- Child stories MUST be created in Jira for each affected sibling repo. Child story keys
  MUST be recorded in `workflow-state.md > Child Stories`.
- Implementation artifacts for sibling repos are authored and reviewed in the orchestrator
  repo before being applied to the target repos.
- No sibling repo branch MUST be merged to `main` before its parent story's tasks PR is
  approved and merged in the orchestrator.

### IV. Approval Gates & Role Separation

- **Spec PR**: MUST be approved by a `product_owner` before planning begins.
- **Plan PR**: MUST be approved by an `fde` before tasks are written.
- **Tasks PR**: MUST be approved by an `fde` before implementation begins.
- **Implementation PR**: MUST be reviewed and approved before merge; no self-merge.
- The submitter MAY NOT approve their own artifact PRs. Role separation is enforced by
  the PDLC workflow; circumventing it is FORBIDDEN.

### V. Contract-First Integration

All cross-service interfaces MUST be defined and agreed upon before implementation:

- **REST APIs**: An OpenAPI contract (or equivalent structured endpoint doc) MUST exist
  in `specs/<STORY_ID>/contracts/` before any controller, service, or client code is
  written.
- **GraphQL**: Schema SDL changes MUST be defined in `contracts/graphql-bff-api.md`
  before resolver implementation. Additive-only changes are the default; breaking changes
  require an approved spec amendment.
- **Kafka events**: Avro schema changes MUST be documented and backwards-compatible.
  A consumer MUST NOT break when a new optional field is added by a producer.
- **Database migrations**: Flyway/Liquibase migration scripts MUST be included in the
  tasks list and reviewed before service code that depends on them is written.

### VI. Observability & Error Handling

In addition to the global OTEL requirements, all Sapphire services MUST follow:

- Health data (temperature readings, heart rate, SpO2, step counts, wellness scores) MUST
  NEVER appear in log fields, span attributes, or metric label values. Mask or omit all
  such values before any telemetry emission.
- Every service MUST expose a `/health` (or `/actuator/health` for Spring Boot) liveness
  endpoint that returns HTTP 200 in a healthy state. Missing liveness endpoints block
  production deployments.
- Silent error swallowing is FORBIDDEN in all languages. Either handle the error
  explicitly with a logged, structured error event, or let it propagate to the nearest
  boundary handler. Empty `catch` blocks and bare `except` clauses with no action are
  FORBIDDEN.

### VII. Configuration & Environment Safety

- No hardcoded URLs, credentials, timeouts, ports, or environment names in source code.
  All such values MUST be injected via environment variables or a Spring/Pydantic
  configuration object.
- All database mutations MUST be idempotent where possible (e.g., `ON CONFLICT DO
  NOTHING`, Flyway migration versioning). Destructive operations without a reversible
  path MUST be justified in the spec.
- Secrets MUST NOT appear in any git-tracked file, log line, or environment variable
  printed to stdout. Use a secrets manager or `.env` file excluded by `.gitignore`.

---

## Testing Requirements

Coverage and test pyramid rules complement the global constitution:

- **Sapphire UI (TypeScript/React)**: Custom hooks MUST have RTL unit tests. Apollo query
  components MUST be tested with `MockedProvider`. URL-state-driven components MUST have
  tests that verify correct behavior across param combinations.
- **sapphire-bff-api (Node.js)**: All resolvers MUST have tests using mocked backend
  service clients. DataLoader batching MUST be tested to verify N+1 is eliminated.
- **Java services**: Domain service classes (e.g., `TemperatureService`,
  `RecommendationService`) MUST achieve 100% line coverage. Infrastructure classes
  (controllers, repositories) MUST achieve ≥ 80% line coverage.
- **Python services (FastAPI)**: All Pydantic model validators and async route handlers
  MUST have explicit unit tests. Integration tests MUST use `pytest-asyncio` with
  Testcontainers.
- **LangGraph agents**: Each graph node MUST be unit-tested in isolation. The compiled
  graph MUST be tested end-to-end with deterministic LLM stubs.

---

## Architecture Constraints

The following technology choices are fixed for the Sapphire workspace. Deviations require
a constitution amendment:

| Layer | Fixed Technology |
|---|---|
| Frontend | TypeScript, React 18+, Vite, Apollo Client 3 |
| BFF | Node.js 20+, Apollo Server 4, Express, DataLoader |
| Java services | Java 17, Spring Boot 3.x, Spring Data JPA, Flyway |
| Python services | Python 3.11+, FastAPI, Pydantic v2, structlog, ruff |
| AI agents | LangGraph, Ollama (local), Qdrant (vector store) |
| Auth | Keycloak OIDC/PKCE — no alternative auth provider |
| Messaging | Apache Kafka, Avro schemas, Kafka Connect |
| Workflows | Temporal (durable orchestration) |
| Observability | OpenTelemetry SDK, shared OTEL Collector |
| Primary DB | PostgreSQL (TimescaleDB extension for timeseries) |
| Cache / PubSub | Redis |

---

## Governance

This local constitution extends the global constitution at
`org-constitution/constitution.md`. In case of conflict, the **global constitution takes
precedence** except where the local rule is explicitly more restrictive — in that case
the more restrictive rule applies.

**Amendment Procedure** (local additions or changes):

1. Author opens a PR modifying `.specify/memory/constitution.md` with written rationale.
2. At least one `fde` and one `product_owner` MUST approve.
3. Version MUST be incremented before merge.
4. The effective constitution MUST be regenerated via `/constitution.resolve` after merge.

**Versioning Policy**: follows MAJOR.MINOR.PATCH — same semantics as the global
constitution.

**Version**: 1.1.0 | **Ratified**: 2026-09-07 | **Last Amended**: 2026-09-07
