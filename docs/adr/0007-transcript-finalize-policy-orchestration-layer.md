# ADR 0007: Transcript finalize policy at orchestration layer

- **Status:** Accepted
- **Date:** 2026-08-05

## Context

`StructuredUpdate.FinalizeAgentStream` converts `StreamingAgentText` into `FinalAgentText` and triggers full markdown rendering. Today it is emitted from multiple call sites — `TranscriptEventIngestion` (finalize-before-non-chunk), `AcpPromptExecutor` (prompt start, completion, failure, cancel, dispose), and `AcpAgentEditor` (before user echo and on error) — with overlapping intent but no single documented interface.

`transcript-pipeline-consolidation` merged dispatcher and mapper into `TranscriptEventIngestion` but left finalize rules embedded there. That improved routing locality but did not answer “when must the agent stream finalize?” without reading several modules.

## Decision

Extract **`TranscriptFinalizePolicy`** as a stateless `internal object` at the orchestration layer (alongside ingestion and prompt executor, later `acp/transcript/model/` after package restructure).

- **Policy** decides *when* to emit `FinalizeAgentStream`.
- **`TranscriptEventIngestion`** retains `SessionUpdate` → `StructuredUpdate` mapping; it prepends `policy.finalizePrelude(update)` before `mapUpdate(update)`.
- **`TranscriptModel`** retains *how* finalize mutates the block list; it stays pure over `StructuredUpdate`.

### Policy hooks (v1)

| Hook | Caller | Behaviour |
| --- | --- | --- |
| `finalizePrelude(update)` | `TranscriptEventIngestion` | Emit `FinalizeAgentStream` for every `SessionUpdate` except `AgentMessageChunk` (including blank non-chunk that maps to empty list) |
| `onPromptStarting()` | `AcpAgentEditor`, `AcpPromptExecutor` | Finalize before new prompt / user echo |
| `onPromptResponse()` | `AcpPromptExecutor` (`PromptResponseEvent`) | Finalize on transport completion signal |
| `onPromptFlowCompleted()` | `AcpPromptExecutor` (post-`collect`) | Always finalize — covers omitted `PromptResponseEvent`; `transportSentResponse` is KDoc-only in v1 |
| `onPromptFailed()` | `AcpAgentEditor`, `AcpPromptExecutor` | Finalize before error surfacing |
| `onPromptInterrupted()` | `AcpPromptExecutor` (`cancelPrompt`, `disposePromptWork`) | Finalize before cancel/dispose |

No separate `onEditorClosing()` — editor `dispose()` does not finalize; prompt interrupt paths already cover dispose.

### Invariants

1. Non-chunk `SessionUpdate` finalizes before the mapped update applies.
2. New prompt finalizes before dispatch (closes prior agent stream).
3. Prompt flow end finalizes at least once even without `PromptResponseEvent`.
4. Failure/cancel/interrupt finalizes before idle/error.
5. Redundant finalize is acceptable — `TranscriptModel.finalizeAgentStream()` no-ops when no streaming block exists.

Direct `StructuredUpdate.FinalizeAgentStream` construction remains allowed in model, harness, and adapter tests; production ingestion/executor/editor paths delegate to policy.

## Alternatives considered

1. **Keep finalize inside `TranscriptEventIngestion`** — Rejected. Ingestion owns *what* to map; scattering prompt-edge rules across executor and editor persists.
2. **Merge policy into `TranscriptModel`** — Rejected. Model must stay pure over `StructuredUpdate`; orchestration lifecycle events are outside model scope.
3. **Stateful dedup in policy v1** — Rejected. Model idempotency already absorbs double-finalize; state adds complexity without user-visible benefit.
4. **Injectable policy interface** — Rejected. One stateless implementation; no runtime swap.

## Consequences

### Positive

- One module and test seam (`TranscriptFinalizePolicyTest`) for finalize ordering.
- New `SessionUpdate` variants route finalize through policy prelude — cannot forget the rule at ingestion.
- Wiki and graphify have a single navigation target for finalize semantics.

### Negative

- Call-site migration must cover ingestion, executor, and editor — missed site = regression.
- Redundant finalize may still trigger `TranscriptPanel.sync` (existing behaviour).

### Neutral

- No ACP protocol change (ADR 0002 unchanged).
- Grep enforcement of allowed `FinalizeAgentStream` sites — slice 02 (`FinalizeAgentStreamConstructionTest`).
- Pause/resume hooks deferred — KDoc extension point only.
