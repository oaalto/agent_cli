## Status

draft

## Problem Statement

The ACP transcript stack in `agent/acp/` carries two parallel rendering implementations and a wide surface of shallow modules. Understanding how a single `SessionUpdate` becomes a visible transcript row requires bouncing across many files with overlapping responsibilities and no single deep seam.

**Active path (live):** `AcpAgentEditor` → `AcpPromptEventDispatcher` → `TranscriptSessionUpdateMapper` → `StructuredUpdate` → `TranscriptViewController` → `TranscriptModel` → `TranscriptPanel` → `TranscriptBlockViewFactory` (Swing block views, `JTextPane` / nested `JEditorPane` fragments per block).

**Legacy/dead path (orphaned):** `TranscriptHtmlAppender` → `TranscriptPaneHtmlOps` — a monolithic `JEditorPane` in `text/html` mode that appended HTML fragments into one document. Grep confirms **zero production callers**: `TranscriptHtmlAppender` is referenced only in its own file, `TranscriptPaneHtmlOps` (KDoc), and `TranscriptHtmlAppenderStreamingTest`. `AcpAgentEditor` wires `TranscriptViewController`, not `TranscriptHtmlAppender`.

**Stale documentation:** `docs/wiki/subsystems/acp-client.md` and `docs/wiki/concepts/context.md` still list `TranscriptHtmlAppender` as part of the active rendering stack and describe `transcriptArea` as a scrollable `JEditorPane`. The live UI uses `TranscriptPanel` (a `JBScrollPane` over a vertical `BoxLayout` column of block rows).

**Shallow-module fragmentation:** 27 `Transcript*.kt` files in `acp/`. Event ingestion is split across `AcpPromptEventDispatcher` (28 lines, finalize-then-map policy) and `TranscriptSessionUpdateMapper` (108 lines, `SessionUpdate` → `StructuredUpdate`). Rendering is split across `TranscriptRenderer` (text extraction and plain-text formatters — not view rendering), `TranscriptMarkdownRenderer`, `TranscriptBlockConverter`, `TranscriptHtmlBuilder`, `TranscriptTableBuilder`, three `TranscriptToolCall*Renderer` files, `TranscriptBlockViewFactory` (~640 lines), `TranscriptBlockLabelBinder`, and `TranscriptRenderHelpers`. The **interface** (what a maintainer must know to trace one update) is nearly as large as the combined **implementation**, which is the hallmark of shallow modules with poor **locality**.

**Shared code with dual ancestry:** `TranscriptStreamingCursor` serves both paths — HTML cursor entities for the dead `JEditorPane` appender and `CURSOR_CHAR` for live `JTextPane` streaming in `TranscriptBlockLabelBinder`. `TranscriptRenderHelpers` mixes dead-path document wrappers (`userPromptSpan`, `plainLineSpan`, `HTML_DOCUMENT_END`) with live-path fragment builders used by tool cards and markdown blocks.

This fragmentation increases regression risk (wiki and code disagree on the architecture), makes AI navigation harder, and leaves dead code that still earns test maintenance.

## Solution

Consolidate the transcript pipeline into one **deep module** with a small **interface** and high **leverage**: ACP events and imperative editor calls enter as `StructuredUpdate`; the module owns ordered `TranscriptBlock` state and exposes a single Swing `JComponent` for the transcript column. Delete the dead HTML document path. Fold shallow ingestion and rendering helpers behind seams with clear ownership.

### Phase 1 — Delete dead path (high leverage, low risk)

Remove modules that fail the **deletion test** — deleting them does not scatter complexity to callers because no production code invokes them:

| Module | Action |
| --- | --- |
| `TranscriptHtmlAppender.kt` | **Delete** |
| `TranscriptPaneHtmlOps.kt` | **Delete** |
| `TranscriptHtmlAppenderStreamingTest.kt` | **Delete** |
| `TranscriptRenderHelpers.userPromptSpan`, `plainLineSpan` | **Delete** (only dead-path callers) |
| `TranscriptStreamingCursor.streamBlockHtml`, `finalizedBlockHtml`, `stripCursor`, `hasCursor` | **Delete** or relocate `CURSOR_CHAR` to the live streaming binder |
| `TranscriptStreamingCursorTest.kt` | **Delete** if HTML cursor helpers are removed |

Retain `TranscriptHtmlBuilder`, `TranscriptRenderHelpers.escapeHtml`, `formatToolStatusHtml`, and `htmlDocumentStart` — these serve live Swing block views (tool card bodies, markdown HTML fragments inside `TranscriptBlockViewFactory` / `CollapsibleToolPanel`), not the retired monolithic pane.

### Phase 2 — Deepen event ingestion seam

Merge `AcpPromptEventDispatcher` and `TranscriptSessionUpdateMapper` into one module (e.g. `TranscriptEventIngestion`) whose **interface** is:

```kotlin
fun ingest(update: SessionUpdate): List<StructuredUpdate>
fun ingestPromptCompleted(): List<StructuredUpdate>  // [FinalizeAgentStream]
```

**Implementation** owns the finalize-before-non-chunk policy currently in `AcpPromptEventDispatcher` and all `SessionUpdate` → `StructuredUpdate` mapping. `AcpClientSessionOperationsImpl` and prompt routing call this single seam instead of two objects.

Rename or absorb `TranscriptRenderer`'s extraction helpers (`extractText`, `renderEventText`, `renderToolCallBodyParts`) into this module or a package-private `AcpContentText` helper so the name matches behaviour. The public **interface** for callers remains `StructuredUpdate` variants only.

### Phase 3 — Deepen view seam (optional follow-up in same PR if scope allows)

`TranscriptViewController` already acts as the EDT-safe **adapter** between model and panel. Strengthen **locality** by:

1. Co-locating `TranscriptModel`, `TranscriptBlock`, and `StructuredUpdate` under a `transcript/` package (or `transcript/model/`).
2. Co-locating `TranscriptPanel`, `TranscriptBlockViewFactory`, `TranscriptBlockLabelBinder`, `TranscriptCodeBlockViewFactory` under `transcript/view/`.
3. Co-locating markdown → body-part conversion (`TranscriptMarkdownRenderer`, `TranscriptBlockConverter`, `TranscriptHtmlBuilder`, `TranscriptTableBuilder`, `TranscriptToolCall*Renderer`) under `transcript/render/` with one entry point, e.g. `TranscriptContentRenderer.renderAgentText(text): List<TranscriptBodyPart>`.

The **interface** for rendering agent markdown should hide the `RenderedBlock` → `TranscriptBodyPart` → Swing widget chain from `AcpAgentEditor` and the event mapper.

### Phase 4 — Documentation alignment

Update wiki sources to describe the live stack only:

- `docs/wiki/subsystems/acp-client.md` — remove `TranscriptHtmlAppender` from the rendering stack; document `TranscriptViewController` → `TranscriptModel` → `TranscriptPanel`.
- `docs/wiki/concepts/context.md` — replace "owns `TranscriptHtmlAppender`" with `TranscriptViewController`; remove `committedBodyHtml` / `streamingPlainText` facts.

### Target architecture (after)

```
SessionUpdate / editor imperative calls
        │
        ▼
TranscriptEventIngestion  (deep: finalize policy + mapping)
        │ List<StructuredUpdate>
        ▼
TranscriptViewController  (deep: EDT bridge)
        │
   ┌────┴────┐
   ▼         ▼
TranscriptModel   TranscriptPanel
(block state)     (block views via BlockViewFactory)
        │
        ▼
TranscriptContentRenderer  (deep: markdown + tool bodies → TranscriptBodyPart)
```

## User Stories

1. As a developer tracing a tool-call update, I want one ingestion module to map `SessionUpdate` → `StructuredUpdate`, so I do not bounce between a dispatcher and a mapper to learn finalize semantics.
2. As a developer fixing streaming cursor behaviour, I want a single live-path implementation (`TranscriptBlockLabelBinder` + `TranscriptBlock.StreamingAgentText`), so I am not tempted to patch the dead `JEditorPane` appender.
3. As a developer onboarding to ACP transcript work, I want wiki pages to match production wiring (`TranscriptViewController`), so documentation does not send me to deleted architecture.
4. As a developer adding a new `StructuredUpdate` variant, I want model, view factory, and label binder co-located under one package, so **locality** keeps the change in one place.
5. As a developer reviewing a PR, I want dead modules deleted rather than deprecated, so the codebase does not carry two rendering stories.
6. As a developer writing tests, I want the **interface** (`StructuredUpdate` in → blocks / component state out) to be the test surface, so I can verify behaviour without reaching into HTML document splice internals.
7. As an AI agent exploring the repo, I want fewer shallow `Transcript*` files at the package root, so graphify and wiki routes land on a deep module faster.
8. As a user of the ACP client, I want zero visible behaviour change from consolidation, so refactoring improves maintainability without UX regression.

## Implementation Decisions

### Ownership

- **Vertical slice:** `acp/` — all affected files live in `src/main/kotlin/com/oaalto/agent/acp/`.
- **No changes to** `pty/`, `worktree/`, or `settings/` except import path updates if packages move.

### Modules to delete

- `TranscriptHtmlAppender.kt`
- `TranscriptPaneHtmlOps.kt`
- `TranscriptHtmlAppenderStreamingTest.kt`
- Dead-path-only helpers in `TranscriptStreamingCursor.kt` and `TranscriptRenderHelpers.kt` (see Phase 1 table)
- `TranscriptStreamingCursorTest.kt` if HTML cursor API is removed

### Modules to consolidate

| Current modules | Target | Rationale |
| --- | --- | --- |
| `AcpPromptEventDispatcher` + `TranscriptSessionUpdateMapper` | `TranscriptEventIngestion` (name TBD) | Single **seam** for event → `StructuredUpdate`; policy and mapping share **locality** |
| `TranscriptRenderer` (extraction only) | Absorbed into ingestion or `AcpContentText` | **Deletion test**: name implies view rendering but module only extracts text |
| `TranscriptMarkdownRenderer` + `TranscriptBlockConverter` + `TranscriptHtmlBuilder` + `TranscriptTableBuilder` + `TranscriptToolCall*Renderer` | `TranscriptContentRenderer` package | Deep rendering **interface**: markdown/tool content → `TranscriptBodyPart` |
| `TranscriptViewController` + `TranscriptModel` + `TranscriptPanel` | Keep; optionally repackage under `transcript/` | Already the live deep view **adapter** |

### Modules to keep unchanged (this PRD)

- `TranscriptBlockViewFactory.kt` — large but earns its keep; layout logic is distinct from content rendering. Package move only.
- `TranscriptColorProvider.kt`, `TranscriptFooter.kt` — orthogonal concerns at clear **seams**.
- `TranscriptHtmlBuilder.kt` — **not** part of the dead path; builds HTML fragments for Swing `JEditorPane` cells inside block rows. Rename only if confusion persists after dead-code removal.

### Deletion test summary

| Module | Deletion test result |
| --- | --- |
| `TranscriptHtmlAppender` | Pass — complexity does not reappear in callers (none) |
| `TranscriptPaneHtmlOps` | Pass — only called by appender |
| `AcpPromptEventDispatcher` | Fail alone — finalize policy must land in merged ingestion module |
| `TranscriptSessionUpdateMapper` | Fail alone — mapping must land in merged ingestion module |
| `TranscriptHtmlBuilder` | Fail — live tool/markdown HTML fragments still need it |

### ADR alignment

- **ADR 0001** (custom ACP client): Consolidation is internal refactor; transcript UX model unchanged. Aligned.
- **ADR 0002** (Kotlin ACP SDK): Ingestion module still maps `SessionUpdate` from SDK types. Aligned.
- **ADR 0003** (per-project agent selection): No settings changes. Aligned.

### Migration notes

- `AcpAgentEditor` session listener and `AcpClientSessionOperationsImpl.notify()` switch from `AcpPromptEventDispatcher.dispatchSessionUpdate` to the merged ingestion API.
- No change to `StructuredUpdate` sealed hierarchy unless a follow-up PR simplifies variants; this PRD treats `StructuredUpdate` as the stable **interface** between ingestion and view.

## Testing Decisions

### What to test

- **External behaviour through the deep interface:** `StructuredUpdate` sequences produce expected `TranscriptBlock` lists and survive `TranscriptPanel.sync` without regression.
- **Ingestion module:** unit tests migrated from `TranscriptSessionUpdateMapperTest`; add cases for finalize-before-non-chunk policy currently implicit in `AcpPromptEventDispatcher`.
- **Deletion verification:** after removing dead path, no references to `TranscriptHtmlAppender` or `TranscriptPaneHtmlOps` remain (compile + test suite).

### Modules to test

- `TranscriptEventIngestion` (merged) — all `SessionUpdate` variant mappings, plan events, agent chunk streaming, finalize-on-non-chunk.
- `TranscriptModel` — existing tests remain; no behaviour change expected.
- `TranscriptViewController` — existing integration patterns via `blocksForTest()` if present.
- `TranscriptBlockLabelBinder` / streaming — retain or add test that `StreamingAgentText` shows `CURSOR_CHAR` and `FinalAgentText` does not.

### Tests to delete

- `TranscriptHtmlAppenderStreamingTest.kt`
- `TranscriptStreamingCursorTest.kt` (if HTML cursor API deleted)

### Prior art

- `TranscriptSessionUpdateMapperTest.kt` — migrate to merged ingestion tests.
- `TranscriptModelTest.kt`, `TranscriptModelPlanTest.kt` — should pass unchanged.

### Verification

`./gradlew qualityGate` passes after implementation. Wiki lint (`docs/wiki/path-map.json`) updated if paths change.

## Out of Scope

- Visual layout fixes (block alignment, insets, `preferredSize`) — covered by `acp-transcript-block-alignment-fix` PRD.
- New block types, markdown features, or theme redesign.
- Changing `StructuredUpdate` variant shapes or ACP protocol handling beyond module moves.
- Collapsing `TranscriptBlockViewFactory` into fewer files — repackaging only unless a clear shallow pass-through is found during implementation.
- PTY Passthrough mode — separate transcript stack.
- Performance optimisation of `TranscriptPanel.sync` incremental updates (future work).
- Renaming `TranscriptHtmlBuilder` unless dead-code deletion still leaves naming confusion.

## Further Notes

- Grep evidence (2026-07-24): `TranscriptHtmlAppender` has **no production references**. The live entry point is `AcpAgentEditor` line 56: `TranscriptViewController(project, ::runOnEdt)`.
- Wiki pages `acp-client.md` and `context.md` cite `TranscriptHtmlAppender` as a source and describe `committedBodyHtml` streaming — these facts are obsolete and should be corrected in the same change that deletes the dead path.
- `TranscriptStreamingCursor.CURSOR_CHAR` remains the live streaming indicator; HTML entity helpers exist solely for the retired `JEditorPane` document model.
- Implementers should update `CHANGELOG.md` under `### Changed` (refactor) and `### Removed` (dead modules) when code ships.
- Related wiki: [ACP client subsystem](../../wiki/subsystems/acp-client.md), [Domain context & ACP transcript model](../../wiki/concepts/context.md).
- Recommended implementation order: Phase 1 (delete dead path + fix wiki) → Phase 2 (merge ingestion) → Phase 3 (package deepening) if timeboxed, Phase 3 can be a follow-up PR since it is structural only.
