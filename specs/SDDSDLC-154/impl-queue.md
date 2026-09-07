# Implementation Queue: SDDSDLC-154 — Body Temperature Metric Ingestion, Storage, and Reporting

**Generated**: 2026-08-20
**Tasks file**: specs/SDDSDLC-154/tasks.md
**Total tasks**: 51 across 4 repos and 6 phases

---

## Execution Order

Tasks execute repo-by-repo, phase-by-phase. Within a phase, tasks marked `[P]` are parallel-safe.

| # | Repo | Phase | Tasks | Status |
|---|------|-------|-------|--------|
| 1 | sapphire-health-service | Phase 1: Setup | T001, T003, T004, T005 | [x] |
| 2 | sapphire-charting-api | Phase 1: Setup | T002, T004 | [x] |
| 3 | sapphire-health-service | Phase 2: Foundational | T006, T007, T008, T009, T010, T011, T012, T013 | [x] |
| 4 | sapphire-charting-api | Phase 2: Foundational | T014, T015, T016 | [x] |
| 5 | sapphire-health-service | Phase 3: US1 Ingestion | T017, T018, T019, T020, T021, T022 | [x] |
| 6 | sapphire-charting-api | Phase 4: US2 Trend Service | T023, T024, T025, T026, T027, T028 | [x] |
| 7 | sapphire-bff-api | Phase 4: US2 GraphQL | T029, T030, T031, T032 | [x] |
| 8 | sapphire-ui | Phase 4: US2 UI Components | T033, T034, T035, T036, T037, T038, T039, T040, T041 | [x] |
| 9 | sapphire-charting-api | Phase 5: US3 Export | T042, T043, T044 | [x] |
| 10 | all repos | Phase 6: Polish | T045, T046, T047, T048, T049, T050, T051 | [x] |

---

## Notes

- All implementation artifacts are created in the orchestrator repo under `specs/SDDSDLC-154/impl/` since sibling repos are not cloned locally.
- Each queue entry corresponds to a `/speckit.implement STORY_ID=SDDSDLC-154 REPO=<repo> PHASE="<phase>"` invocation in scoped mode.
- Mark each row `[x]` as the phase completes.
