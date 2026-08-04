## Status

ready-for-agent

**Triage:** `ready-for-agent`

## Problem Statement

`TranscriptBlockViewFactory` is a shallow god-module (~650 lines) that owns row creation, row update, markdown widget assembly, tool-card wiring, plan-panel attachment, width adjustment, link handling, and disposal for every `TranscriptBlock` variant. Its **interface** (what a maintainer must understand to change one block type) is nearly as large as the combined row implementations — poor **depth**, poor **locality**.

Changes to agent text layout routinely risk breaking tool cards, plan rows, or streaming rows because they share one `update()` method with intertwined match logic. Detekt suppressions (`CyclomaticComplexMethod`) and extracted one-off helpers signal the module is doing too much. Tests mount the entire factory to assert single-row behaviour, so failures are hard to localize.

## Solution

Decompose `TranscriptBlockViewFactory` into a **deep coordinator** plus **row adapters** — one adapter per major block family:

| Adapter | Owns |
| --- | --- |
| `AgentTextRowAdapter` | `FinalAgentText`, `StreamingAgentText` — body-part column, width adjustment, streaming in-place update |
| `ToolCallRowAdapter` | `ToolCallBlock` — `CollapsibleToolPanel` lifecycle |
| `PlanRowAdapter` | `PlanBlock` — `PlanPanel` attach/detach |
| `SimpleTextRowAdapter` | `UserEcho`, `Thought`, `PlainLine`, `ErrorLine`, `AuthFailureLine` |

The factory **interface** shrinks to:

```kotlin
interface TranscriptBlockRowAdapter {
    fun matches(block: TranscriptBlock, existing: JComponent?): Boolean
    fun create(block: TranscriptBlock, context: RowContext): JComponent
    fun update(existing: JComponent, block: TranscriptBlock, context: RowContext)
    fun dispose(existing: JComponent)
}
```

`TranscriptBlockViewFactory` becomes a registry/dispatcher (~100 lines) that picks the adapter, preserves row reuse keys (`blockId`), and delegates layout constants to shared `RowContext` (column width, color provider, code block factory).

## User Stories

1. As a developer fixing agent text width adjustment, I want to edit only `AgentTextRowAdapter`, so that tool cards and plan rows are unaffected.
2. As a developer adding a new `TranscriptBlock` variant, I want to add one adapter file and register it, so that I do not grow an already cyclomatic `update()` method.
3. As a developer reviewing a PR, I want row-type changes isolated to one adapter diff, so that review scope is predictable.
4. As a developer debugging a tool-card height bug, I want tests that mount only `ToolCallRowAdapter`, so that failures localize without agent-text fixtures.
5. As an AI agent, I want smaller modules with clear **seams**, so that navigation graphs show adapter boundaries instead of one 650-line node.
6. As a user, I want zero visible behaviour change from decomposition, so that refactoring improves safety without UX churn.
7. As a developer fixing streaming cursor display, I want streaming logic colocated with agent text adapter and label binder calls, so that finalize/stream transitions stay in one place.
8. As a developer adjusting plan panel insets, I want plan layout constants in `PlanRowAdapter` only, so that plan tweaks do not touch prose rows.
9. As a maintainer, I want the factory coordinator to pass the **deletion test** — deleting it would scatter dispatch logic to `TranscriptPanel`, confirming it earns its keep as a thin registry.
10. As a developer writing EDT tests, I want adapters testable with minimal `RowContext` fakes, so that headless CI does not require full editor fixtures.
11. As a developer coordinating content-renderer work, I want agent and tool adapters to consume `TranscriptBodyPart` lists from the unified content module, so that adapters only map parts → widgets.
12. As a user resizing the IDE window, I want width adjustment invoked uniformly through `RowContext.onColumnWidthChanged`, so that every adapter remeasures consistently.
13. As a developer disposing editors on row removal, I want `dispose()` implemented per adapter, so that code-block editors in agent rows do not leak when tool rows are removed.
14. As a developer fixing link click handling in agent markdown, I want hyperlink listeners scoped to `AgentTextRowAdapter`, so that plain-line rows stay simple.
15. As a reviewer enforcing detekt rules, I want cyclomatic complexity distributed across adapters below threshold, so that suppressions can be removed.
16. As a developer matching blocks on update, I want `matches()` colocated with `update()` in the same adapter, so that match predicates and update logic do not drift.
17. As a user reading mixed tool and prose output, I want row ordering and spacing unchanged, so that decomposition is structural only.
18. As a product owner, I want this refactor to unblock faster iteration on ACP rendering without weekly regressions, so that the transcript pane stays the product differentiator per ADR 0001.

## Implementation Decisions

### Ownership

- **Vertical slice:** ACP transcript view layer — `TranscriptBlockViewFactory` and row-private helpers move into adapter types; `TranscriptPanel` sync contract unchanged.

### Coordinator responsibilities (keep in factory)

- Adapter registration order (specific before generic).
- `blockId` → component reuse map.
- Delegating `create` / `update` / `dispose` / `widthAdjustment` to matched adapter.
- Logging type-mismatch diagnostics (preserve existing mismatch logs for debugging).

### Adapter responsibilities (move out)

- Widget tree construction per block family.
- In-place streaming update vs full rebuild policy for agent text.
- Tool card expand/collapse body lazy build.
- Plan panel identity matching by plan id.

### Shared context object

```kotlin
data class RowContext(
    val project: Project?,
    val columnWidth: Int,
    val codeBlockViewFactory: TranscriptCodeBlockViewFactory,
    val colorProvider: TranscriptColorProvider,
    val onColumnWidthChanged: (Int) -> Unit,
)
```

### Seam for testing

**Primary test seam:** each `TranscriptBlockRowAdapter` in isolation — `create` + `update` + `widthAdjustment` on a mounted `JPanel` test harness without full `TranscriptPanel`.

**Integration seam:** existing `TranscriptBlockViewFactoryTest` cases remain as coordinator-level smoke tests.

### Migration strategy

1. Extract `SimpleTextRowAdapter` first (lowest risk).
2. Extract `ToolCallRowAdapter` and `PlanRowAdapter`.
3. Extract `AgentTextRowAdapter` last (highest complexity: markdown body parts, code blocks, streaming).

### Dependencies

- **Recommended after** [acp-transcript-content-renderer](../acp-transcript-content-renderer/prd.md) so agent and tool adapters both consume unified `List<TranscriptBodyPart>` from `renderMarkdownText`.
- Shared **body-part → widget mapper** (extracted with adapters) delivers visual parity between agent rows and tool cards; content renderer alone guarantees semantic/budget parity only.
- Can start in parallel if adapters initially call existing renderers.

### ADR alignment

- **ADR 0001**: Layout/visual behaviour preserved. Aligned.

## Testing Decisions

### What makes a good test

- Mount adapter output in a headless-friendly `JPanel` on EDT; assert preferred heights, child counts, visibility — observable widget behaviour, not private method calls.
- Streaming tests: apply two `StreamingAgentText` blocks with growing text; assert in-place update without child count explosion.

### Modules to test

- One test class per adapter (or nested classes in `TranscriptBlockRowAdapterTest`).
- Retain `TranscriptBlockViewFactoryTest` as integration smoke — should shrink as cases move down.

### Prior art

- `TranscriptBlockViewFactoryTest` — migrate cases to adapter-scoped tests where possible.
- `CollapsibleToolPanel` tests — tool adapter may reuse patterns for expand/collapse.

### Verification

- `./gradlew qualityGate` passes; detekt `CyclomaticComplexMethod` suppression on factory `update` removed.

## Out of Scope

- Markdown/content rendering logic — `acp-transcript-content-renderer` PRD.
- `TranscriptPanel.sync` incremental algorithm changes.
- New block types or visual redesign.
- Package directory moves — `acp-transcript-package-restructure` PRD.
- Plan domain mapping (`PlanUpdateMapper`) — stays in plan package.

## Further Notes

- Architecture review strength: **Strong** — pairs with content-renderer unification as top regression fix.
- Existing detekt suppressions on factory are acceptance criteria for done: remove or reduce to adapter level only.
