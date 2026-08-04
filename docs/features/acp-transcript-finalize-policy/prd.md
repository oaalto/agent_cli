## Status

ready-for-agent

**Triage:** `ready-for-agent`

## Problem Statement

`StructuredUpdate.FinalizeAgentStream` — the signal that converts `StreamingAgentText` into `FinalAgentText` and triggers full markdown rendering — is emitted from **four or more call sites** with overlapping but not identical intent:

- Event ingestion finalizes before non-chunk `SessionUpdate` events (finalize-before-non-chunk policy).
- Prompt executor finalizes before each new prompt, after prompt completion, on transport failure, and on cancel.
- View controller may apply finalize on editor lifecycle edges.
- Tests and model callers inject finalize directly.

When streaming rendering breaks (zero-height code blocks, missing finalize on prompt end, double-finalize glitches), fixes land at whichever call site surfaced the symptom, leaving sibling paths inconsistent. The **interface** for “when must the agent stream finalize?” is implicit — spread across modules with poor **locality**. Maintainers cannot answer “is finalize guaranteed on prompt end?” without reading multiple files.

## Solution

Centralize finalize **policy** in one module — `TranscriptFinalizePolicy` (name TBD) — that decides *when* to emit `FinalizeAgentStream` given lifecycle events. Call sites invoke policy methods instead of constructing `StructuredUpdate.FinalizeAgentStream` directly.

```kotlin
internal object TranscriptFinalizePolicy {
    fun onSessionUpdate(update: SessionUpdate): List<StructuredUpdate>  // wraps ingest pre-pass
    fun onPromptStarting(): List<StructuredUpdate>
    fun onPromptFlowCompleted(transportSentResponse: Boolean): List<StructuredUpdate>
    fun onPromptFailed(): List<StructuredUpdate>
    fun onPromptCancelled(): List<StructuredUpdate>
    fun onEditorClosing(): List<StructuredUpdate>
}
```

`TranscriptEventIngestion` retains mapping logic but delegates finalize-before-non-chunk to policy. `AcpPromptExecutor` delegates all prompt-edge finalize decisions. `TranscriptViewController` delegates editor-close finalize if applicable.

The policy module owns documented invariants (see below) as KDoc + tests — the **deep interface** for finalize semantics.

### Documented invariants

1. **Non-chunk session update:** any `SessionUpdate` except `AgentMessageChunk` finalizes the active stream before the mapped update applies.
2. **New prompt:** finalize before dispatching user echo / starting prompt job (closes prior agent stream).
3. **Prompt completion:** finalize at least once when prompt flow ends, even if transport omits `PromptResponseEvent`.
4. **Failure/cancel:** finalize before surfacing error or returning to idle.
5. **Idempotent finalize:** model `finalizeAgentStream()` is safe when no streaming block exists (no-op).

## User Stories

1. As a developer answering “when does streaming become final?”, I want one policy module with documented invariants, so that I do not grep four packages.
2. As a developer fixing missing finalize on prompt end, I want to fix policy once, so that all prompt exit paths inherit the fix.
3. As a developer fixing double-finalize flicker, I want policy tests that enumerate event sequences, so that regressions are caught without UI tests.
4. As a developer adding a new `SessionUpdate` variant, I want ingestion to ask policy whether to finalize first, so that the new variant cannot forget the rule.
5. As a developer writing `TranscriptEventIngestion` tests, I want finalize expectations driven from policy tables, so that ingestion and policy tests do not duplicate matrices.
6. As a user, I want agent code blocks to reach final highlighted state after each reply completes, so that streaming glitches do not persist as blank panels.
7. As a user submitting a new prompt while the prior reply streams, I want the prior stream finalized cleanly, so that the transcript does not show two concurrent streaming rows.
8. As a user cancelling a long-running prompt, I want partial agent text preserved as final, so that work is not lost visually.
9. As a developer on prompt executor, I want thin calls like `policy.onPromptFlowCompleted(sentResponse)`, so that executor does not encode finalize rules inline.
10. As a maintainer applying the **deletion test**, I want direct `FinalizeAgentStream` construction outside policy forbidden by convention and grep CI check, so that policy cannot be bypassed accidentally.
11. As an AI agent, I want a single module name for finalize semantics in wiki/graphify, so that navigation lands on the policy seam.
12. As a developer debugging transport quirks, I want `onPromptFlowCompleted(transportSentResponse)` to encode the “omit PromptResponseEvent” workaround in one place, so that changelog history is not the only documentation.
13. As a user receiving tool calls mid-agent-reply, I want finalize-before-tool-call enforced by policy when non-chunk updates arrive, so that tool cards do not append to a streaming ghost row.
14. As a developer writing model tests, I want to keep applying `FinalizeAgentStream` directly in model unit tests, so that model behaviour stays testable without pulling policy (policy tests cover orchestration).
15. As a reviewer, I want a sequence diagram in wiki updated to show policy as the finalize gate, so that docs match code.
16. As a product owner, I want no user-visible behaviour change except fixing known finalize gaps, so that this is policy extraction plus bug closure.
17. As a developer integrating future session pause/resume, I want policy hooks for pause boundaries, so that finalize rules extend without new scatter.
18. As a tester, I want table-driven tests for event sequences `[chunk, chunk, tool_update]`, `[chunk, prompt_complete]`, `[chunk, error]`, so that coverage is exhaustive at the policy seam.

## Implementation Decisions

### Ownership

- **Vertical slice:** ACP transcript event + prompt orchestration — touches ingestion, prompt executor, optionally view controller; **not** model internals (model keeps `finalizeAgentStream()` implementation).

### Policy vs model

- **Policy** decides *when* to emit `StructuredUpdate.FinalizeAgentStream`.
- **TranscriptModel** decides *how* finalize mutates block list (`StreamingAgentText` → `FinalAgentText`).
- Do not merge policy into model — model stays pure over `StructuredUpdate` list; policy sits at orchestration layer.

### Call site migration

| Caller | Before | After |
| --- | --- | --- |
| `TranscriptEventIngestion.ingest` | inline finalize before non-chunk | `TranscriptFinalizePolicy.onSessionUpdate` prelude + mapping |
| `AcpPromptExecutor` | five direct finalize emissions | policy methods per lifecycle hook |
| `TranscriptViewController` | direct finalize on specific paths | `onEditorClosing` if still needed |
| Tests for ingestion/executor | assert finalize in sequences | assert via policy or migrated table tests |

### Seam for testing

**Primary test seam:** `TranscriptFinalizePolicy` — given lifecycle event, expect ordered `StructuredUpdate` list (often singleton finalize + optional mapped updates from ingestion delegation).

Ingestion tests focus on mapping; policy tests focus on finalize presence/absence/ordering.

### Grep enforcement (optional follow-up)

- Add detekt or simple test that fails if `StructuredUpdate.FinalizeAgentStream` appears outside policy, model, and test fixtures — document allowed exceptions.

### ADR alignment

- **ADR 0002**: Policy operates on SDK `SessionUpdate` types; no protocol change. Aligned.

### Relationship to fence normalization

- Finalize triggers full markdown render where fence normalization must match streaming path — coordinate with `acp-transcript-fence-normalization` PRD so finalized text uses same normalizer.

## Testing Decisions

### What makes a good test

- Table-driven: input = sequence of lifecycle events; output = expected finalize count and positions relative to other updates.
- Do not assert on private executor fields — drive through policy public methods.

### Modules to test

- `TranscriptFinalizePolicy` — new primary test class.
- `TranscriptEventIngestionTest` — adjust to policy delegation; retain variant mapping coverage.
- Prompt executor integration test (if exists) or new focused test with fake listener recording updates.

### Prior art

- `TranscriptEventIngestionTest` — already asserts finalize-before-non-chunk; migrate finalize expectations to policy tests.
- `TranscriptModelTest` — finalize application behaviour stays here.

### Verification

- `./gradlew qualityGate` passes.
- Manual smoke: streaming reply → tool call → final code block visible without second prompt.

## Out of Scope

- Changing `StructuredUpdate.FinalizeAgentStream` type shape.
- Rewriting `TranscriptModel.finalizeAgentStream()` merge logic.
- Session transport/protocol changes to guarantee `PromptResponseEvent`.
- PTY mode transcript.
- Merging ingestion back into prompt executor (separate concerns).

## Further Notes

- Architecture review strength: **Worth exploring** — high clarity win; moderate regression risk if migration misses a call site.
- Changelog history documents multiple finalize fixes at executor — this PRD prevents recurrence.
- Can land independently of content-renderer and block-view work; recommended before large view refactors so streaming/final transitions stay stable.
