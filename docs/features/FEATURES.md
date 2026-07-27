# Features

Master list of features tracked in `docs/features/`. Statuses are documented in the PRD (`## Status` section) and mirrored here.

**Active** features are ordered by **implementation priority** (top = next). Agents maintain this order — do not ask the human to set it. When adding or updating a row, read PRDs, ticket blocking edges, ADRs, and cross-feature dependencies; insert or re-order so the list reflects what should ship next.

**Implemented** features are listed separately, most recently completed first.

## Active (implementation order)

| Feature | Status | Description |
|---------|--------|-------------|

| [acp-transcript-block-alignment-fix](acp-transcript-block-alignment-fix/prd.md) | draft | Fix block content (code, tables, blockquotes) rendering shifted right in the ACP transcript pane |
| [acp-client-operations-wiring](acp-client-operations-wiring/prd.md) | draft | Deepen filesystem client-ops into a single deep module with an explicit composition root |
| [acp-session-controller-deepening](acp-session-controller-deepening/prd.md) | draft | Shrink AcpSessionController interface, extract transport/lifecycle internals, move session-resume orchestration into the controller |
| [acp-session-resume-orchestration](acp-session-resume-orchestration/prd.md) | draft | Consolidate resume planning, attach, and execution from three scattered modules into a single deep module |
| [worktree-pending-launch-handoff](worktree-pending-launch-handoff/prd.md) | draft | Consolidate cross-project worktree handoff (enqueue → open → consume) into a deep module with explicit contract |
| [transcript-pipeline-consolidation](transcript-pipeline-consolidation/prd.md) | draft | Consolidate fragmented transcript rendering stack, remove dead HTML path, reduce shallow-module fragmentation |

## Implemented

| Feature | Description |
|---------|-------------|
| [acp-session-transcript-persistence](acp-session-transcript-persistence/prd.md) | Persist ACP session conversations as workspace-local plain-text transcript files with correlation tokens and copy-diagnostics action |
| [settings-observability-help](settings-observability-help/prd.md) | Add session logs & transcripts help section to the Agent CLI settings page |
| [unified-launch-resolution](unified-launch-resolution/prd.md) | Extract shared launch-resolution logic into a single module used by all three launch paths |
| [tiered-session-diagnostics](tiered-session-diagnostics/prd.md) | Tiered IDE logging via AgentCliLog with session diagnostics context and dialog-only failure logging |
