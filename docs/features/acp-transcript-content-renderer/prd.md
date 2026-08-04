## Status

done

**Triage:** `done`

## Problem Statement

ACP transcript rendering breaks easily when markdown or tool-card content changes because agent text and tool-card bodies follow **two post-parse presentation pipelines** that must stay behaviourally identical but live in separate call sites. Markdown **parsing** is already shared (`TranscriptMarkdownRenderer.parseToBlocks`); the split is after parse: tool text flows `RenderedBlock` → `TranscriptBlockConverter` → `TranscriptBodyPart`, while agent `FinalAgentText` maps `RenderedBlock` → Swing widgets directly in `TranscriptBlockViewFactory`. Truncation limits, fence handling, syntax-highlighting caps, and table/blockquote semantics are duplicated or diverge subtly across those paths.

A maintainer fixing fenced-code highlighting in agent prose can miss the equivalent path in tool cards — or vice versa — producing visible inconsistencies in the same transcript session. The **interface** for “turn markdown-ish text into `TranscriptBodyPart` list” is shallow: callers must know which renderer to invoke and which invariants each enforces. **Locality** is poor: one conceptual change touches multiple objects with no single **seam** to test.

## Solution

Introduce one **deep content-render module** — `TranscriptContentRenderer` — whose **interface** is:

- Input: raw text (agent markdown or tool text body), optional render context (truncation profile, highlight budget).
- Output: ordered `List<TranscriptBodyPart>` suitable for both `AgentTextRow` and `CollapsibleToolPanel` body assembly.

Absorb the agent-text path (`TranscriptMarkdownRenderer` → `RenderedBlock` → body parts) and the tool-text path (`TranscriptToolCallTextBodyRenderer` and related fragment builders) behind this module. Diff, terminal, image, and embedded-resource tool content remain distinct entry points but delegate shared text/markdown conversion to the same core.

Callers (`TranscriptBlockViewFactory`, `TranscriptToolCallContentRenderer`, ingestion helpers) invoke one seam instead of choosing between parallel pipelines.

## User Stories

1. As a developer fixing fenced-code highlighting, I want one module to own markdown → body-part conversion, so that agent text and tool cards stay visually consistent after my change.
2. As a developer adding GFM table support, I want to implement it once behind the content-render seam, so that tables render identically in agent replies and tool output.
3. As a developer reviewing a PR that touches transcript markdown, I want a single test suite at the content-render **interface**, so that I can verify behaviour without reading two parallel implementations.
4. As a developer onboarding to ACP transcript work, I want “how does markdown become pixels?” to have one answer, so that I am not sent to three renderer files.
5. As an AI agent exploring the repo, I want a deep module with high **leverage** at the content boundary, so that graphify and wiki routes land on the right seam quickly.
6. As a user reading agent responses, I want code blocks in tool cards to look like code blocks in agent prose, so that the transcript feels cohesive.
7. As a user reading long tool output, I want truncation and highlight limits applied consistently, so that performance does not degrade unpredictably per block type.
8. As a developer adding inline-code styling, I want the styled-run model shared across agent and tool paths, so that bold/italic/code spans align.
9. As a developer fixing blockquote indentation, I want one place to adjust inset semantics, so that blockquotes do not drift between row types.
10. As a developer writing regression tests, I want golden `TranscriptBodyPart` fixtures independent of Swing, so that tests run fast and survive widget refactors.
11. As a maintainer applying the **deletion test**, I want absorbed helpers to fail the test only when their logic truly reappears in callers, so that consolidation does not leave shadow copies.
12. As a developer extending tool cards with a new `ContentBlock` variant, I want plain-text bodies to route through the shared renderer automatically, so that I only write bespoke logic for genuinely different content shapes.
13. As a user with a dark IDE theme, I want HTML fragments and highlighted code to inherit the same colour contract, so that theme changes do not break one path only.
14. As a developer debugging a rendering bug, I want a single `renderAgentOrToolText(text)` entry I can call from a scratch test, so that reproduction does not require mounting Swing rows.
15. As a product owner, I want zero visible UX change from consolidation, so that maintainability improves without user-facing churn.
16. As a developer coordinating with fence-normalization work, I want the content renderer to call normalization as a pre-pass hook via `ContentRenderOptions.applyFenceNormalization`, so that streaming and final paths share one policy injection point (see [fence-normalization PRD](../acp-transcript-fence-normalization/prd.md)).
17. As a developer touching `TranscriptBodyPart` sealed variants, I want the content module to be the only producer of **text-derived** body parts, so that new part kinds have one birthplace (diff/terminal/image placeholders stay on `TranscriptToolCallContentRenderer`).
18. As a reviewer checking ADR alignment, I want the refactor to stay inside the ACP client slice, so that ADR 0001’s in-tab transcript model is unchanged.

## Implementation Decisions

### Ownership

- **Vertical slice:** ACP client transcript stack — content conversion only; no changes to session transport, PTY mode, or worktree launch.
- **Depends on:** `transcript-pipeline-consolidation` (implemented) — `StructuredUpdate` → `TranscriptBlock` seam is stable.

### Target module interface

```kotlin
internal object TranscriptContentRenderer {
    fun renderMarkdownText(
        text: String,
        options: ContentRenderOptions = ContentRenderOptions.DEFAULT,
    ): List<TranscriptBodyPart>
}
```

`ContentRenderOptions` carries truncation ceiling, max highlighted code blocks, whether to apply agent fence normalization (`applyFenceNormalization` — see [fence-normalization PRD](../acp-transcript-fence-normalization/prd.md)), and whether to skip markdown parsing for plain-looking tool text (`useMarkdownHeuristic`, default on for tool-equivalent callers).

### Modules to absorb or delegate

| Current responsibility | Action |
| --- | --- |
| Agent markdown AST walk | Move behind `TranscriptContentRenderer`; keep `RenderedBlock` as internal AST shape or fold into body-part builder |
| Tool text body segmentation | Route through same `renderMarkdownText` |
| `TranscriptBlockConverter`, `TranscriptHtmlBuilder`, `TranscriptTableBuilder` | Package-private helpers called only from content renderer |
| `TranscriptToolCallDiffRenderer`, terminal/image embed paths | Stay separate public methods on content renderer or sibling `TranscriptToolCallContentRenderer` that **delegates** text to core |

### Seam for testing

**Primary test seam:** `TranscriptContentRenderer.renderMarkdownText` — highest point that captures markdown → `TranscriptBodyPart` without Swing.

Secondary: existing `TranscriptToolCallContentRenderer.renderBodyParts` becomes a thin orchestrator over content renderer + diff/terminal adapters.

### Behaviour preservation

- Re-export or preserve existing truncation constants (`MAX_TEXT_CHARACTERS`, `MAX_HIGHLIGHTED_CODE_BLOCKS`) at the content-render **interface** so callers do not hardcode duplicates.
- `renderContentFragments` (HTML string list for legacy call sites) becomes a pure mapping over `renderBodyParts` — no second parse path.

### ADR alignment

- **ADR 0001** (custom ACP client): Internal refactor; transcript UX unchanged. Aligned.
- **ADR 0002** (Kotlin ACP SDK): No protocol changes. Aligned.

### Relationship to sibling features

- **[Block view decomposition](../acp-transcript-block-view-decomposition/prd.md)** — adapters consume unified body parts; shared part → widget mapper delivers visual parity (links, blockquote chrome). Content renderer unifies the body-part stream; block-view owns Swing layout.
- **[Fence normalization](../acp-transcript-fence-normalization/prd.md)** — inject normalization before `renderMarkdownText` via `ContentRenderOptions.applyFenceNormalization`; until that lands, content renderer may call existing normalizer internally. Content-renderer lands first; fence PRD extracts duplicate streaming/final hooks without API churn.

## Testing Decisions

### What makes a good test

- Assert on **external behaviour** at the content-render **interface**: given input text, expect ordered `TranscriptBodyPart` variants with correct language ids, truncation markers, and HTML payloads.
- Do **not** assert on AST node shapes or private helper call order unless testing a documented invariant.

### Modules to test

- `TranscriptContentRenderer` — primary new test module, migrated cases from `TranscriptMarkdownRendererTest` and overlapping `TranscriptToolCallContentRendererTest` text scenarios.
- `TranscriptToolCallContentRenderer` — slim integration tests for diff/terminal/image paths delegating text to core.

### Prior art

- `TranscriptMarkdownRendererTest` — migrate markdown cases (headings, lists, tables, fences, inline styles).
- `TranscriptToolCallContentRendererTest` — migrate text-body cases; keep diff/fenced mixed tests as orchestration checks.

### Verification

- `./gradlew qualityGate` passes.
- No duplicate markdown parse entry points remain (grep audit for direct `TranscriptMarkdownRenderer` use outside content module).

## Out of Scope

- Swing row layout, `preferredSize`, column width — covered by alignment and block-view PRDs.
- `TranscriptBlockViewFactory` decomposition — separate PRD.
- Changing `TranscriptBodyPart` sealed hierarchy shapes unless required for unification.
- PTY passthrough transcript stack.
- New markdown features not already supported in either path.
- Deleting `TranscriptHtmlBuilder` — still needed for HTML body parts; only call sites move.

## Further Notes

- Architecture review strength: **Strong** — highest leverage for regression class “markdown tweak breaks one path only.”
- Recommended to land before or in parallel with block-view decomposition so row adapters consume a stable body-part stream.
- Parent consolidation PRD (`transcript-pipeline-consolidation`) Phase 3 proposed this module; this spec scopes Phase 3’s content-render portion only.
