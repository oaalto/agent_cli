# Features

Master list of features tracked in `docs/features/`. PRD status (`## Status` section) and per-ticket `**Status:**` in slice files are authoritative; this page is the rollup and ordering guide.

**Active** features are ordered by **implementation priority** (top = next). Agents maintain this order — do not ask the human to set it. When adding or updating a feature, read PRDs, ticket blocking edges, ADRs, and cross-feature dependencies; insert or re-order so the list reflects what should ship next.

**Implemented** features are listed separately, most recently completed first.

Last updated: 2026-08-03

## Summary

| Metric | Count |
| --- | --- |
| Ready-for-agent features | 0 |
| Ready-for-agent tickets | 0 |
| Completed features | 10 |

---

## Active (implementation order)

_No active features._

---

## Implemented

| Feature | Tickets | Notes |
| --- | --- | --- |
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
