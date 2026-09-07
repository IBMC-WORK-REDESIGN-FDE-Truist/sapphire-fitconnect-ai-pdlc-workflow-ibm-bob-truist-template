# Effective Constitution Resolution Report

## Run Metadata

| Field | Value |
|---|---|
| **Timestamp** | 2026-09-07T10:00:00Z |
| **Status** | PASS |
| **Triggered by** | `/constitution.resolve` (session continuation) |

---

## Source Resolution

| Field | Value |
|---|---|
| **Global source mode** | `local` |
| **Global resolved path** | `org-constitution/constitution.md` |
| **Global found** | `true` |
| **Local source** | `.specify/memory/constitution.md` |
| **Local is template** | `false` (no unfilled `[PLACEHOLDER]` markers detected) |
| **Composition case** | Case A — global + real local |
| **Precedence rule applied** | local-over-global; more restrictive rule always wins |

---

## Output Artifacts

| Artifact | Path | Written |
|---|---|---|
| Global snapshot | `.specify/runtime/global-constitution.md` | ✅ |
| Effective constitution | `.specify/runtime/effective-constitution.md` | ✅ |
| Report | `.specify/runtime/effective-constitution-report.md` | ✅ (this file) |

---

## Conflict Analysis

### Override Callouts

| Override ID | Topic | Global Rule (Part 1) | Local Rule (Part 2) | Severity | Resolution | Downstream Impact |
|---|---|---|---|---|---|---|
| OVR-001 | Error handling scope | Python / FastAPI section: "Bare `except:` clauses are FORBIDDEN; catch specific exception types only." (language-scoped to Python only) | Principle VI: "Empty `catch` blocks and bare `except` clauses with no action are FORBIDDEN" in **all languages** (Java, TypeScript/Node.js, Python). | HIGH | Local wins — the more restrictive cross-language ban applies. Agents MUST enforce empty-catch prohibition in Java, TypeScript, and Node.js code, not only in Python. | All implementation artifacts for Java (sapphire-health-service, sapphire-charting-api) and TypeScript/Node.js (sapphire-bff-api, sapphire-ui) MUST have no empty catch/except blocks. PR reviewers MUST flag these during code review. |

---

## Blocking Errors

None. Resolution completed successfully.

---

## Notes

- The local constitution's Governance clause states "global takes precedence except where local is more restrictive." This is consistent with the `local-over-global` effective-constitution rule because the only identified conflict (OVR-001) is a case where local IS more restrictive — so both sources agree on the outcome.
- The global constitution (v2.2.0) does not contain principles I–VII as named in the local constitution. The local Principles I–VII are additive extensions, not overrides. No additional conflicts were identified.
- The Architecture Constraints table in the local constitution is purely additive — not present in global.
- Testing requirements in the local constitution are more specific (repo-level coverage targets) than the global (which does not specify coverage floors in v2.2.0). These are additive, not conflicting.
