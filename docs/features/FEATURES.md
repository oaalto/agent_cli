# Features

Master list of features tracked in `docs/features/`. PRD status (`## Status` section) and per-ticket `**Status:**` in slice files are authoritative; this page is the rollup and ordering guide.

**Active** features are ordered by **implementation priority** (top = next). Agents maintain this order — do not ask the human to set it. When adding or updating a feature, read PRDs, ticket blocking edges, ADRs, and cross-feature dependencies; insert or re-order so the list reflects what should ship next.

**Implemented** features are listed separately, most recently completed first.

Last updated: 2026-08-05

## Summary

| Metric | Count |
| --- | --- |
| Ready-for-agent features | 0 |
| Ready-for-agent tickets | 0 |
| Completed features | 16 |

---

## Active (implementation order)

| Feature | Status | Rationale | PRD |
| --- | --- | --- | --- |
| — | — | — | — |

---

## Architecture conflicts

| Features | Note |
| --- | --- |
| — | — |

---

## Implemented

| Feature | Tickets | Notes |
| --- | --- | --- |
| [acp-transcript-package-restructure](acp-transcript-package-restructure/) | 01–07 | All `done` (2026-08-05); `transcript/{model,render,view,theme}/` repackage + `TranscriptPackageDependencyTest` CI guard |
| [acp-transcript-fence-normalization](acp-transcript-fence-normalization/) | 01–03 | All `done` (2026-08-05); single `normalizeAgentFences` seam, tool-text gating, CI construction guard |
| [acp-transcript-finalize-policy](acp-transcript-finalize-policy/) | 01–02 | All `done` (2026-08-05); `TranscriptFinalizePolicy` + CI construction guard |
| [acp-transcript-panel-integration-tests](acp-transcript-panel-integration-tests/) | 01 | Harness + review follow-ups `done` (2026-08-05); mounted-panel golden scenarios on ViewController apply path |
| [acp-transcript-block-view-decomposition](acp-transcript-block-view-decomposition/) | 01–08 | All `done` (2026-08-05); adapter registry + 4 row adapters, mapper, test gaps closed in ticket 08 |
| [acp-transcript-content-renderer](acp-transcript-content-renderer/) | 01–03 | All `done` (2026-08-04); unified dual markdown→body-part pipelines |
| [worktree-pending-launch-handoff](worktree-pending-launch-handoff/) | 01–02 | All `done` (2026-08-03); `WorktreePendingLaunchHandoff` deep module |
| [acp-session-resume-orchestration](acp-session-resume-orchestration/) | 01–04 | All `done`; resume orchestration extracted to `AcpSessionResumeOrchestrator` |
| [acp-session-controller-deepening](acp-session-controller-deepening/) | 01–07 | All `done`; controller API shrunk to `start` / `prompt` / `cancelPrompt` / `dispose` |
| [transcript-pipeline-consolidation](transcript-pipeline-consolidation/) | 01–03 | All `done` (2026-08-03) |
| [acp-client-operations-wiring](acp-client-operations-wiring/) | 01–06 | All `done`; `SessionFilesystemOperations` deep module |
| [acp-transcript-block-alignment-fix](acp-transcript-block-alignment-fix/) | 01–02 | All `done` |
| [acp-session-transcript-persistence](acp-session-transcript-persistence/) | 01–06 | All `done` |
| [unified-launch-resolution](unified-launch-resolution/) | 01–05 | All `done` |
| [tiered-session-diagnostics](tiered-session-diagnostics/) | 01–03 | All `done` |
| [settings-observability-help](settings-observability-help/) | 01 | Ticket `done` |

---

## How to use this document

1. Pick the next feature from **Active**.
2. Open its PRD, then implement tickets in order respecting **Blocked by** chains.
3. When all tickets in a feature are complete, flip each ticket's `**Status:**` to `done` and move the feature to **Implemented**.
4. Update this file and `CHANGELOG.md` when feature status changes materially.

Agents: follow **implement** and **to-tickets** skills; see `docs/agents/issue-tracker.md` for master-list rules.