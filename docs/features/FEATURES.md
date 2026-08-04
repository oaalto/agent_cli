# Features

Master list of features tracked in `docs/features/`. PRD status (`## Status` section) and per-ticket `**Status:**` in slice files are authoritative; this page is the rollup and ordering guide.

**Active** features are ordered by **implementation priority** (top = next). Agents maintain this order — do not ask the human to set it. When adding or updating a feature, read PRDs, ticket blocking edges, ADRs, and cross-feature dependencies; insert or re-order so the list reflects what should ship next.

**Implemented** features are listed separately, most recently completed first.

Last updated: 2026-08-04

## Summary

| Metric | Count |
| --- | --- |
| Ready-for-agent features | 6 |
| Ready-for-agent tickets | 2 |
| Completed features | 10 |

---

## Active (implementation order)

| Feature | Status | Rationale | PRD |
| --- | --- | --- | --- |
| [acp-transcript-content-renderer](acp-transcript-content-renderer/) | `ready-for-agent` | Unify dual markdown→body-part pipelines (agent text + tool cards) — top regression class | [prd.md](acp-transcript-content-renderer/prd.md) |
| [acp-transcript-block-view-decomposition](acp-transcript-block-view-decomposition/) | `ready-for-agent` | Split 650-line block view factory into row adapters; depends on stable body parts from content renderer | [prd.md](acp-transcript-block-view-decomposition/prd.md) |
| [acp-transcript-panel-integration-tests](acp-transcript-panel-integration-tests/) | `ready-for-agent` | Mounted-panel harness for sync/scroll/layout regressions; safety net for view refactors (parallel with above) | [prd.md](acp-transcript-panel-integration-tests/prd.md) |
| [acp-transcript-finalize-policy](acp-transcript-finalize-policy/) | `ready-for-agent` | Centralize `FinalizeAgentStream` emission policy scattered across ingestion and prompt executor | [prd.md](acp-transcript-finalize-policy/prd.md) |
| [acp-transcript-fence-normalization](acp-transcript-fence-normalization/) | `ready-for-agent` | Single fence normalizer for streaming binder vs final markdown render paths | [prd.md](acp-transcript-fence-normalization/prd.md) |
| [acp-transcript-package-restructure](acp-transcript-package-restructure/) | `ready-for-agent` | `transcript/model|render|view/` repackage after functional refactors stabilize | [prd.md](acp-transcript-package-restructure/prd.md) |

### acp-transcript-content-renderer

| Ticket | Blocked by | Status |
| --- | --- | --- |
| [01 — Introduce content renderer for tool text bodies](acp-transcript-content-renderer/01-introduce-content-renderer-tool-text.md) | — | `done` |
| [02 — Route agent final text through content renderer](acp-transcript-content-renderer/02-route-agent-text-through-content-renderer.md) | 01 | `ready-for-agent` |
| [03 — Consolidate helpers and seal the content-render seam](acp-transcript-content-renderer/03-consolidate-helpers-and-seal-seam.md) | 02 | `ready-for-agent` |

**Architecture conflicts:** [acp-transcript-block-view-decomposition](acp-transcript-block-view-decomposition/) should land after ticket 02 so row adapters consume a stable `TranscriptBodyPart` stream. [acp-transcript-fence-normalization](acp-transcript-fence-normalization/) can land in parallel — `ContentRenderOptions.applyFenceNormalization` is the injection point.

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
