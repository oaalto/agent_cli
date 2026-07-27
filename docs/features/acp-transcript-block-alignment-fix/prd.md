## Status

implemented

## Problem Statement

In ACP Client mode, block content (fenced code blocks, markdown tables, blockquotes, and thematic breaks) renders shifted significantly to the right — often partially or fully off the visible viewport — while inline prose remains correctly left-aligned. This creates a jarring visual inconsistency where the transcript appears to have two different column widths depending on content type. The issue makes code output, tables, and quoted blocks difficult or impossible to read without manual horizontal scrolling, which is disabled by default.

This degrades the in-tab UX that ADR 0001 defines as the product's differentiator — a clean, professional transcript pane.

## Solution

Fix the layout pipeline so that block content (code, tables, blockquotes) occupies the same effective column width as inline prose. The primary cause is a `preferredSize` stale-value bug in `AgentTextRow.widthAdjustment()` that makes block content containers wider than the viewport, causing clipping under `HORIZONTAL_SCROLLBAR_NEVER`. The secondary cause is the 20px left inset applied to every block type, which compounds with the clipping to make content appear shifted right.

The fix:
1. Update `preferredSize` for all children in `widthAdjustment()`, not just `JTextPane`.
2. Verify the 20px left inset is intentional and aligns across all block types (it does — code blocks, tables, and blockquotes all use 20px, which is the desired alignment).
3. Ensure the `Box.Y_AXIS` content column width stays within the scroll pane viewport after resize.

## User Stories

1. As a user reading a long agent response, I want code blocks, tables, and blockquotes to align with inline prose on the left edge, so that my eye tracks straight down the transcript without jumping.
2. As a user pasting agent output with fenced code, I want the code to be fully visible without horizontal scrolling, so that I can read it immediately.
3. As a user comparing a table of data to surrounding text, I want the table to start at the same left margin as the text, so that the structure feels consistent.
4. As a user scrolling back through old messages, I want block content to remain visible without needing to scroll horizontally, so that reviewing is comfortable.
5. As a developer adding a new block type to the transcript, I want the layout pipeline to constrain all children to the viewport width, so that new blocks don't introduce alignment issues.
6. As a user with a narrow window, I want the transcript to respect the available width for all content types, so that resizing the IDE doesn't push code off-screen.
7. As a user with the IDE in any theme, I want the layout to work identically — no theme-specific width calculations, so that the fix applies everywhere.
8. As a reviewer of layout changes, I want the fix to be testable via observable behavior (no clipping at any window width above 400px), so that I can verify it without manual inspection.

## Implementation Decisions

### Ownership

- **Vertical slice:** `acp/` — all affected files live in `agent/acp/`.
- **No changes to** `pty/`, `worktree/`, or `settings/`.

### Modules to modify

- **`TranscriptBlockViewFactory.kt`** — `AgentTextRow.widthAdjustment()`: update `preferredSize` for `JComponent` children, not just `JTextPane`.
- **`CollapsibleToolPanel.kt`** — `adjustBodyHeight()`: same fix as above for tool card body children.
- **No new modules** — this is a surgical fix to existing layout logic.

### The fix

In `AgentTextRow.widthAdjustment()`, the `JComponent` branch currently does:

```kotlin
is JComponent -> {
    child.setSize(w, child.preferredSize.height)
    child.maximumSize = Dimension(Int.MAX_VALUE, child.preferredSize.height)
}
```

It should also update `preferredSize`:

```kotlin
is JComponent -> {
    val height = child.preferredSize.height
    child.setSize(w, height)
    child.preferredSize = Dimension(w, height)
    child.maximumSize = Dimension(Int.MAX_VALUE, height)
}
```

Same change applies to `CollapsibleToolPanel.adjustBodyHeight()` for `JComponent` children (code blocks and HTML parts in tool card bodies).

### Why this works

- `Box.Y_AXIS` layout determines container width from children's `preferredSize`.
- Without updating `preferredSize`, the content column retains the old (potentially wider) preferred width from before resize.
- The scroll pane's viewport is narrower than the content column's preferred width → content clips on the right.
- With `preferredSize` updated to match the actual rendered width, the content column shrinks to fit the viewport → no clipping.

### 20px left inset is intentional

All block types use a consistent 20px left inset:
- Code blocks: `CODE_BLOCK_LEFT_INSET = 20` (via `JBUI.Borders.emptyLeft`)
- Tables: `TABLE_LEFT_INSET = 20` (via CSS `margin-left:20px`)
- Blockquotes: `BLOCKQUOTE_TEXT_INSET = 20` (via `JBUI.Borders.emptyLeft`)
- Thematic breaks: `THEMATIC_BREAK_HORIZONTAL_INSET = 20` (via `JBUI.Borders.empty`)

This is the desired alignment. The bug is not that the inset is wrong — it's that the container is too wide, causing the right edge to clip and making the content appear shifted.

### Nested scroll pane (informational, no change)

`AcpEditorLayout.buildRootPanel()` wraps the transcript (already a `JBScrollPane` from `TranscriptPanel`) in another `JBScrollPane`. This is intentional to allow the auth/permission prompts to scroll with the transcript. The nested structure doesn't contribute to the bug — the fix at the `widthAdjustment` level resolves it regardless.

### ADR alignment

- **ADR 0001** (custom ACP client): Purely visual fix. No protocol, transport, or UX model changes. Aligned.
- **ADR 0002** (Kotlin ACP SDK): No SDK interaction changes. Aligned.
- **ADR 0003** (per-project agent selection): No settings changes. Aligned.

## Testing Decisions

### What to test

- **External behavior only:** observable visual layout properties (no clipping at any window width above 400px, consistent left alignment across all block types).
- No unit tests for Swing layout directly — the IDE's visual rendering is the test surface.
- One unit test for the fix's core logic: verify that `preferredSize` is set correctly after `widthAdjustment()`.

### Modules to test

- **`AgentTextRow.widthAdjustment()`** — verify that after calling this method, all child components have `preferredSize.width == w` (where `w` is the row's available width).
- **`CollapsibleToolPanel.adjustBodyHeight()`** — same verification.

### Prior art

- `TranscriptPanelTest` already mocks `JBScrollPane` behavior. Follow that pattern for verifying layout constraints.
- The fix changes only layout sizing, not block rendering or content. Existing block tests should continue to pass.

### Verification

`./gradlew qualityGate` passes after implementation.

## Out of Scope

- Changing the 20px left inset value (it's intentional and aligned).
- Redesigning the overall layout (72/28 transcript/bottom split, 20/80 prompt/shell split).
- Adding horizontal scrollbars (the fix eliminates the need for them).
- Changing `SCROLL_BOTTOM_THRESHOLD` or heading sizes (separate concerns, covered in a previous PRD that has been processed).
- Adding new block types or rendering features.
- Theme-specific layout fixes — the color provider handles theme adaptation; only geometry is in scope.
- PTY Passthrough mode layout — not affected by these changes.

## Further Notes

- The root cause is a `preferredSize` stale-value bug in the layout resize pipeline. The fix is surgical: update `preferredSize` for `JComponent` children in two methods (`widthAdjustment` and `adjustBodyHeight`).
- The 20px left inset is consistent across all block types and is the desired behavior. The bug makes the container too wide, causing clipping that manifests as content appearing shifted right.
- Implementers should update `CHANGELOG.md` under `### Fixed` when the code ships.
- Related wiki page: [Domain context & ACP transcript model](../concepts/context.md) — this PRD doesn't change the verified facts listed there.
