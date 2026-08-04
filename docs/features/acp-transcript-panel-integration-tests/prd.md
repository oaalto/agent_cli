## Status

ready-for-agent

**Triage:** `ready-for-agent`

## Problem Statement

ACP transcript tests stop at **shallow seams** — unit tests assert `TranscriptBodyPart` lists, markdown AST shapes, or isolated `TranscriptBlockViewFactory` rows — but few tests exercise the full chain:

`StructuredUpdate` → `TranscriptModel` → `TranscriptPanel.sync()` → mounted Swing hierarchy with scroll behaviour, column width reflow, and row reuse.

Regressions that only appear when rows sync together (double scrollbar, stick-to-bottom failure, zero-height code blocks after resize, prose vanishing while tools remain) slip through because no harness mounts the **live panel** under realistic block sequences. `TranscriptPanelScrollTest` exists but covers scroll only; `TranscriptBlockViewFactoryTest` mounts rows without panel sync incremental logic.

The **interface** for “transcript looks correct in the editor” has no deep test **adapter** — poor **leverage** for preventing the breakage class the user reports: “output rendering breaks very easily when making changes.”

## Solution

Introduce a **mounted-panel integration test harness** — `TranscriptPanelTestHarness` — that provides:

```kotlin
internal class TranscriptPanelTestHarness(
    project: Project? = null,
    columnWidth: Int = 400,
) {
    val panel: TranscriptPanel
    val scrollPane: JBScrollPane

    fun apply(updates: List<StructuredUpdate>)
    fun apply(vararg updates: StructuredUpdate)
    fun syncBlocks(blocks: List<TranscriptBlock>)
    fun setColumnWidth(width: Int)
    fun pumpEdt()
    fun assertRowCount(expected: Int)
    fun assertNoHorizontalScrollbar()
    fun assertViewportShowsBottom(tolerance: Int = 4)
    fun findCodeBlockRows(): List<JComponent>
    fun rowPreferredHeights(): List<Int>
}
```

Tests drive **`StructuredUpdate` sequences** (or block lists) through `TranscriptModel` + `TranscriptViewController` or direct panel sync — matching production wiring — and assert observable UI invariants on EDT.

Seed the harness with **golden scenarios** derived from recent regressions (CHANGELOG): auto-scroll while streaming, code block non-zero height after finalize, no nested scrollbars, column resize reflow, mixed tool + agent rows.

## User Stories

1. As a developer changing `TranscriptPanel.sync`, I want integration tests that fail when row reuse breaks, so that I catch regressions before manual IDE testing.
2. As a developer fixing code block zero-height, I want a test that applies stream chunks + finalize and asserts code row height > 0, so that the bug class stays closed.
3. As a developer removing outer scroll pane wrapper, I want a test counting scroll bars in the hierarchy, so that double-scrollbar cannot return silently.
4. As a developer working on stick-to-bottom, I want harness helpers for viewport position, so that scroll tests read declaratively.
5. As a developer refactoring block view factory, I want panel-level tests unchanged if row adapters preserve behaviour, so that the harness is stable across refactors.
6. As a CI maintainer, I want tests headless-safe on EDT with short timeouts, so that quality gate stays reliable.
7. As a developer adding a new `StructuredUpdate` variant, I want a harness recipe to extend golden scenarios, so that new block types get panel coverage by default.
8. As a user, I want fewer visual regressions per release, so that integration tests guard the transcript pane quality bar.
9. As an AI agent implementing transcript features, I want runnable examples in test sources showing harness usage, so that new tests follow one pattern.
10. As a developer testing column resize, I want `setColumnWidth` to pump layout and remeasure all rows, so that narrow-window bugs reproduce in CI.
11. As a developer testing streaming, I want `apply(AgentMessageChunk…)` sequences through real ingestion, so that finalize policy + panel interact realistically.
12. As a reviewer, I want PRs touching view layer to add or update harness scenarios when behaviour changes, so that coverage grows with code churn.
13. As a maintainer, I want harness construction isolated from `AcpAgentEditor`, so that tests do not require full editor fixture.
14. As a developer debugging preferredSize bugs, I want `rowPreferredHeights()` exported, so that assertions do not depend on fragile component tree walks in each test.
15. As a product owner, I want the harness documented in wiki testing section, so that contributors know the canonical integration seam.
16. As a developer coordinating content renderer work, I want scenarios comparing tool card and agent code block heights side by side, so that dual-pipeline bugs are caught at panel level.
17. As a tester, I want golden block sequences checked into test resources as JSON or Kotlin builders, so that large transcripts are reusable across tests.
18. As a developer on fence normalization, I want a scenario streaming malformed fences then finalizing, so that layout jump is measurable in harness assertions.

## Implementation Decisions

### Ownership

- **Vertical slice:** ACP transcript test sources — harness lives in test tree; minimal production hooks only if required for test visibility (prefer `internal` test fixture module pattern already used in repo).

### Harness wiring options

| Approach | Trade-off |
| --- | --- |
| Model + ViewController + Panel (preferred) | Highest fidelity; exercises real apply path |
| Panel.sync(blocks) only | Faster setup for pure view regressions |

Support both via harness methods.

### Production visibility

- Prefer existing test utilities (`runOnEdt`, `pumpPendingEdtTasks` from scroll test) — extract to shared test helper if duplicated.
- Avoid new production APIs unless `TranscriptPanel` needs `internal` test accessor — use same patterns as `blocksForTest()` on view controller if present.

### Initial golden scenarios (minimum set)

1. **Streaming + finalize + code block height** — chunks with fenced kotlin → finalize → assert code row height > threshold.
2. **Stick-to-bottom on append** — migrate/enhance `TranscriptPanelScrollTest` patterns into harness API.
3. **Single scrollbar** — hierarchy walk asserts one `JScrollPane` owns vertical scrolling for transcript column.
4. **Column resize reflow** — change width 400 → 250 → assert no zero-height prose rows; code blocks still visible.
5. **Mixed tool + agent** — tool complete + agent final text; assert row count and no horizontal scrollbar.

### Seam for testing

**The harness itself is the test seam** — tests call harness, not individual factories. Highest integration point below full `AcpAgentEditor`.

### CI constraints

- Use lightweight `Project` test fixture or null project where panel allows (match `TranscriptPanelScrollTest` `createPanel` pattern).
- EDT pumping with bounded timeout; fail fast on deadlock.

### Dependencies

- Benefits from stable finalize policy and content renderer but can start immediately with current stack.
- **Recommended in parallel** with content-renderer and block-view PRDs as safety net.

### ADR alignment

- **ADR 0001**: Tests guard in-tab transcript UX — aligned with product intent.

## Testing Decisions

### What makes a good test

- Assert observable UI: row counts, component heights, scrollbar presence, viewport position — not private sync indices unless no alternative.
- Use realistic `StructuredUpdate` sequences from `TranscriptEventIngestion` where possible.

### Modules to test

- `TranscriptPanel` sync + scroll + layout (primary).
- Cross-module scenarios via harness (ingestion → model → panel).

### Prior art

- `TranscriptPanelScrollTest` — migrate to harness; keep behaviour coverage.
- `AcpEditorLayoutTest` — scrollbar nesting assertion pattern.
- `TranscriptBlockViewFactoryTest` — row-level cases stay; panel harness complements.

### Verification

- `./gradlew qualityGate` passes.
- Document harness in test class KDoc as entry point for transcript UI regressions.

## Out of Scope

- UI Test Framework / remote robot tests — Swing headless EDT only.
- Screenshot/golden image comparison.
- Full `AcpAgentEditor` integration (separate future harness if needed).
- Performance benchmarking of sync.
- PTY mode transcript.

## Further Notes

- Architecture review strength: **Worth exploring** — enables safe refactors in sibling PRDs; ship early for maximum leverage.
- Top recommendation from architecture review: land in parallel with content-renderer + block-view decomposition as regression safety net.
