# ADR 0005: Transcript row adapter registry

- **Status:** Accepted
- **Date:** 2026-08-04

## Context

`TranscriptBlockViewFactory` (~580 lines) owns creation, update, disposal, and widget assembly for every `TranscriptBlock` variant. Changes to one row family routinely risk regressions in others because they share one `update()` method with intertwined match logic.

`TranscriptPanel` already owns incremental sync and the `blockId` → component reuse map. The factory is the natural dispatch point but does not need to own reuse state.

## Decision

Decompose `TranscriptBlockViewFactory` into a **thin coordinator** plus **row adapters** implementing `TranscriptBlockRowAdapter`:

| Adapter | Block families |
| --- | --- |
| `ToolCallRowAdapter` | `ToolCallBlock` |
| `PlanRowAdapter` | `PlanBlock` |
| `AgentTextRowAdapter` | `StreamingAgentText`, `FinalAgentText` |
| `SimpleTextRowAdapter` | `UserEcho`, `Thought`, `PlainLine`, `ErrorLine`, `AuthFailureLine` |

The coordinator:

- Registers adapters in fixed order (most specific `matches` first: Tool → Plan → Agent text → Simple text).
- Dispatches `create` / `update` / `dispose` to the matched adapter.
- Logs type-mismatch diagnostics; does **not** auto-recreate rows on mismatch (preserve current `TranscriptPanel.sync` behaviour).

`TranscriptPanel` keeps the `blockId` → component map. The factory remains stateless.

Public surface to `TranscriptPanel` is unchanged: `create`, `update`, `disposeRow`.

## Alternatives considered

1. **Monolithic factory with extracted private helpers** — Rejected. Reduces line count but not review locality; `update()` match logic still couples all block families.
2. **Panel-owned dispatch (delete factory)** — Rejected. Scatters adapter registration and mismatch logging into `TranscriptPanel`, which should own scroll/sync only.
3. **Classpath / annotation discovery for adapters** — Rejected. Explicit registration order is required so `SimpleTextRowAdapter` never captures agent blocks.

## Consequences

### Positive

- Row-type PRs touch one adapter file; tests mount adapters in isolation.
- Coordinator shrinks to ~100 lines; complexity budget moves to adapters and `TranscriptBodyPartWidgetMapper`.
- `TranscriptPanel.sync` contract unchanged — zero UX churn.

### Negative

- Four new types plus mapper to maintain during migration.
- Adapter files land in `acp/` first; package restructure moves them to `transcript/view/rows/` in a follow-up PR.

### Neutral

- `onToolToggle` stays constructor-injected on `ToolCallRowAdapter`, not on shared `RowContext`.
- Width adjustment uses per-row resize listeners and shared sizing helpers, not a central `RowContext` callback.
