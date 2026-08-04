# ADR 0006: Separate simple-text and agent-text row shells

- **Status:** Accepted
- **Date:** 2026-08-04

## Context

Today a single `AgentTextRow` `JPanel` handles both simple text blocks (`UserEcho`, `Thought`, `PlainLine`, …) and agent markdown blocks (`StreamingAgentText`, `FinalAgentText`). Simple rows need only a `JTextPane` via `TranscriptBlockLabelBinder`; agent rows need a body-part column with code editors, hyperlinks, and streaming cursor semantics.

Block-view decomposition splits adapters by block family. A natural shortcut is one shared row shell with adapter-specific bind logic.

## Decision

`SimpleTextRowAdapter` and `AgentTextRowAdapter` produce **different Swing row shells**:

- **Simple text** — single `JTextPane`; all presentation via `TranscriptBlockLabelBinder`.
- **Agent text** — `contentColumn` with body-part widgets from `TranscriptContentRenderer` + `TranscriptBodyPartWidgetMapper`; streaming path uses label binder for cursor only.

`StreamingAgentText` and `FinalAgentText` stay in one adapter (same shell, stable `blockId` across finalize).

When `TranscriptPanel` reuses a component at a `blockId` but the block family changes (e.g. simple text → agent text), the coordinator logs a type mismatch and skips update — same as tool/plan mismatches today. No silent shell swap.

## Alternatives considered

1. **One `AgentTextRow` shell for all text blocks** — Rejected. Keeps the god-row problem; simple rows pay agent-column layout cost; `matches()` predicates become ambiguous.
2. **Auto-recreate row on family mismatch** — Rejected. UX-visible flicker and out of scope for structural refactor; panel does not tear down on mismatch today.
3. **Separate adapters for streaming vs final agent text** — Rejected. Shared rebuild/stream policy and stable `blockId` across finalize belong in one adapter.

## Consequences

### Positive

- Simple-text changes cannot break agent markdown layout and vice versa.
- Adapter `matches()` predicates align with component type (plain `JTextPane` vs multi-part column).
- `TranscriptBlockLabelBinder` stays a separate module, testable without full row trees.

### Negative

- Two row shell implementations to maintain instead of one.
- Tests must cover text-family mismatch at a reused `blockId` (log, no update).

### Neutral

- `CollapsibleToolPanel` and `PlanPanel` remain standalone widgets with thin adapters — same pattern as agent/simple split.
