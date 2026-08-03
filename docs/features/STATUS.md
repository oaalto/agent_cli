# Feature backlog — master status

Last updated: 2026-08-03

This document tracks **ready-for-agent** work in `docs/features/` and the recommended implementation order. Ticket status is authoritative per ticket file (`**Status:**` field); this page is a rollup and ordering guide.

## Summary

| Metric | Count |
| --- | --- |
| Ready-for-agent features | 5 |
| Ready-for-agent tickets | 21 |
| Completed features | 5 |
| Architecture conflicts to resolve | 1 (resume orchestration — see below) |

---

## Recommended implementation order

Features are ordered by **user impact**, **independence**, and **cross-feature conflict risk**. Within each feature, implement tickets in numeric order unless noted.

### 1. `acp-transcript-block-alignment-fix` — user-visible layout bug

**Why first:** Surgical Swing layout fix; no dependencies; no overlap with other open work. Restores readable code blocks, tables, and blockquotes in the ACP transcript.

| # | Ticket | Blocked by |
| --- | --- | --- |
| 1 | [01 — transcript row block alignment](acp-transcript-block-alignment-fix/01-transcript-row-block-alignment.md) | — |
| 2 | [02 — tool card body block alignment](acp-transcript-block-alignment-fix/02-tool-card-body-block-alignment.md) | — |

**PRD:** [acp-transcript-block-alignment-fix/prd.md](acp-transcript-block-alignment-fix/prd.md)

---

### 2. `acp-client-operations-wiring` — filesystem client-op deepening

**Why second:** Independent of session lifecycle / resume refactors. Deepens the `fsReadTextFile` / `fsWriteTextFile` path with a testable VFS seam and explicit composition root. Safe to land while resume/controller work is planned.

| # | Ticket | Blocked by |
| --- | --- | --- |
| 1 | [01 — scoped filesystem access VFS seam](acp-client-operations-wiring/01-scoped-filesystem-access-vfs-seam.md) | — |
| 2 | [02 — session filesystem operations deep module](acp-client-operations-wiring/02-session-filesystem-operations-deep-module.md) | 01 |
| 3 | [03 — SDK adapter filesystem delegation](acp-client-operations-wiring/03-sdk-adapter-filesystem-delegation.md) | 02 |
| 4 | [04 — session operations composition root](acp-client-operations-wiring/04-session-operations-composition-root.md) | 03 |
| 5 | [05 — session controller factory injection](acp-client-operations-wiring/05-session-controller-factory-injection.md) | 04 |
| 6 | [06 — wiki session operations documentation](acp-client-operations-wiring/06-wiki-session-operations-documentation.md) | 05 |

**PRD:** [acp-client-operations-wiring/prd.md](acp-client-operations-wiring/prd.md)

---

### 3. `acp-session-resume-orchestration` — resume execution deep module

**Why third:** Extracts ~90 lines of load/list/pick/persist logic from `AcpAgentEditor` into `worktree/resume/AcpSessionResumeOrchestrator` with proper slice boundaries. **Complete this before deepening tickets 05–07** (see conflict note below).

| # | Ticket | Blocked by |
| --- | --- | --- |
| 1 | [01 — prefactor LaunchResumePlan ACP resolve variant](acp-session-resume-orchestration/01-prefactor-launch-resume-plan-acp-resolve.md) | — |
| 2 | [02 — orchestrator ports and adapters](acp-session-resume-orchestration/02-orchestrator-ports-and-adapters.md) | 01 |
| 3 | [03 — AcpSessionResumeOrchestrator](acp-session-resume-orchestration/03-acp-session-resume-orchestrator.md) | 02 |
| 4 | [04 — ACP editor orchestrator delegation](acp-session-resume-orchestration/04-acp-editor-orchestrator-delegation.md) | 03 |

**PRD:** [acp-session-resume-orchestration/prd.md](acp-session-resume-orchestration/prd.md)

> **Ticket 01 can start in parallel with feature 1 or 2** — it is a small plan-type rename with no runtime behaviour change.

---

### 4. `acp-session-controller-deepening` — controller module deepening

**Why fourth:** Large structural refactor (transport, bootstrap, lifecycle, prompt executor, shrunk public API). Tickets **01–04** are independent of resume orchestration. Tickets **05–07** overlap with feature 3 — revise or skip resume portions after feature 3 lands.

| # | Ticket | Blocked by | Notes |
| --- | --- | --- | --- |
| 1 | [01 — process transport seam](acp-session-controller-deepening/01-process-transport-seam.md) | — | |
| 2 | [02 — connection bootstrap](acp-session-controller-deepening/02-connection-bootstrap.md) | 01 | |
| 3 | [03 — session lifecycle core](acp-session-controller-deepening/03-session-lifecycle-core.md) | 02 | |
| 4 | [04 — prompt executor](acp-session-controller-deepening/04-prompt-executor.md) | 03 | |
| 5 | [05 — resume orchestration in lifecycle](acp-session-controller-deepening/05-resume-orchestration-in-lifecycle.md) | 03 | ⚠️ Overlaps feature 3 — revise after 3 ships |
| 6 | [06 — shrink interface and compose start()](acp-session-controller-deepening/06-shrink-interface-and-start.md) | 04, 05 | ⚠️ `start()` resume path must align with orchestrator |
| 7 | [07 — editor migration](acp-session-controller-deepening/07-editor-migration.md) | 06 | ⚠️ Editor resume/persist already moved by feature 3 ticket 04 |

**PRD:** [acp-session-controller-deepening/prd.md](acp-session-controller-deepening/prd.md)

**Parallelism:** Tickets 01–04 can proceed in parallel with feature 3 tickets 02–03 (different subsystems). Do not land 05–07 until resume orchestration (feature 3) is resolved.

---

### 5. `worktree-pending-launch-handoff` — cross-project launch handoff deepening

**Why last:** Speculative maintainability refactor; no known user-facing defect. Prioritize only when extending worktree launch entry points or fixing a handoff bug.

| # | Ticket | Blocked by |
| --- | --- | --- |
| 1 | [01 — handoff consume path](worktree-pending-launch-handoff/01-handoff-consume-path.md) | — |
| 2 | [02 — handoff schedule and callers](worktree-pending-launch-handoff/02-handoff-schedule-and-callers.md) | 01 |

**PRD:** [worktree-pending-launch-handoff/prd.md](worktree-pending-launch-handoff/prd.md)

---

## Architecture conflict — resume orchestration

Two open features both extract resume logic from `AcpAgentEditor`:

| Feature | Resume home | Persistence home |
| --- | --- | --- |
| `acp-session-resume-orchestration` | `worktree/resume/AcpSessionResumeOrchestrator` | `WorktreeSessionBinder` adapter |
| `acp-session-controller-deepening` (05–07) | `AcpSessionLifecycle` inside controller | Editor (`AgentWorktreeStateService`) |

**Resolution:** Implement **feature 3 first**. After ticket 04 lands, adapt deepening tickets 05–07:

- **05** — Skip or reduce to `SessionPicker` seam only; resume branching already lives in the orchestrator.
- **06** — `start()` should connect + bootstrap + open session via orchestrator (or delegate resume to orchestrator), not duplicate lifecycle resume.
- **07** — Editor calls `start()`; persistence already removed from editor by orchestrator ticket 04.

Deepening ticket 05 explicitly notes this overlap and is self-contained for controller deepening only — treat it as superseded by the orchestrator path once feature 3 ships.

---

## Completed features

| Feature | Tickets | Notes |
| --- | --- | --- |
| [transcript-pipeline-consolidation](transcript-pipeline-consolidation/) | 01–03 | All `done` (2026-08-03) |
| [unified-launch-resolution](unified-launch-resolution/) | 01–05 | All `done` |
| [acp-session-transcript-persistence](acp-session-transcript-persistence/) | 01–06 | All `done` |
| [tiered-session-diagnostics](tiered-session-diagnostics/) | 01–03 | All `done`; ticket 02 has one optional unchecked test item |
| [settings-observability-help](settings-observability-help/) | 01 | Ticket `done`; PRD status field is stale (`ready-for-agent`) |

---

## How to use this document

1. Pick the next feature from the ordered list above.
2. Open its PRD, then implement tickets in order respecting **Blocked by** chains.
3. When all tickets in a feature are complete, flip each ticket's `**Status:**` to `done` and move the feature to the completed table.
4. Update this file and `CHANGELOG.md` when feature status changes.

Agents: follow **implement** (ticket/feature completion) and **to-tickets** (new backlog items) skills for `STATUS.md` maintenance steps.
