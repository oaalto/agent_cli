# 01 — Introduce content renderer for tool text bodies

**Parent:** `prd.md`

**What to build:** Add `TranscriptContentRenderer.renderMarkdownText` with `ContentRenderOptions` and route completed tool-card text bodies through it end-to-end. Tool cards still render the same HTML and highlighted code blocks, but production of `TranscriptBodyPart` lists for plain and markdown-ish tool text happens at one seam instead of `TranscriptToolCallTextBodyRenderer` choosing its own parse path.

**Blocked by:** None — can start immediately

**Status:** done

- [x] `TranscriptContentRenderer` exposes `renderMarkdownText(text, options)` returning ordered `TranscriptBodyPart` values; `ContentRenderOptions` carries truncation ceiling, max highlighted code blocks, `useMarkdownHeuristic` (on for tool-equivalent callers), and `applyFenceNormalization` (off by default for tool text until fence-normalization work lands)
- [x] Truncation and highlight limits are defined at the content-render interface (re-export or own the existing `MAX_TEXT_CHARACTERS` and `MAX_HIGHLIGHTED_CODE_BLOCKS` constants) so callers do not duplicate them
- [x] `TranscriptToolCallContentRenderer` text and embedded-text-resource paths delegate to `renderMarkdownText` instead of `TranscriptToolCallTextBodyRenderer`
- [x] New `TranscriptContentRendererTest` covers tool-text scenarios migrated from overlapping `TranscriptToolCallContentRendererTest` cases (plain pre, markdown fences, tables, highlight cap, truncation)
- [x] Diff, terminal, image, and non-text tool content paths are unchanged
- [x] `./gradlew qualityGate` passes; no visible tool-card UX change
