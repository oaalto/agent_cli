# 03 — Consolidate helpers and seal the content-render seam

**Parent:** `prd.md`

**What to build:** Finish consolidation so `TranscriptContentRenderer` is the only producer of text-derived `TranscriptBodyPart` values. Absorbed helpers become package-private, `TranscriptToolCallContentRenderer` is a thin orchestrator over content renderer plus diff/terminal/image adapters, and a grep audit confirms no duplicate markdown-parse entry points remain outside the content module.

**Blocked by:** 02 — Route agent final text through content renderer

**Status:** done

- [x] `TranscriptBlockConverter`, `TranscriptHtmlBuilder`, and `TranscriptTableBuilder` are package-private helpers callable only from the content renderer (or folded in); `TranscriptToolCallTextBodyRenderer` is deleted
- [x] `TranscriptToolCallContentRenderer.renderBodyParts` orchestrates diff, terminal, image, and embedded-resource placeholders, delegating all plain/markdown text to `renderMarkdownText`
- [x] `renderContentFragments` is a pure mapping over `renderBodyParts` with no second parse path
- [x] `TranscriptMarkdownRenderer` remains internal to the content module (parse-only); no caller outside the module invokes `parseToBlocks` directly
- [x] Residual `TranscriptMarkdownRendererTest` cases either live at the content-render interface or are removed as redundant with migrated coverage
- [x] `./gradlew qualityGate` passes; grep audit shows zero direct `TranscriptMarkdownRenderer` use outside the content-render module
