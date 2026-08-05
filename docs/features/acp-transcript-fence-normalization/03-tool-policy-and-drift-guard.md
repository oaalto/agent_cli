# 03 — Tool fence policy and drift guard

**Parent:** `prd.md`

**What to build:** Extend fence repair to markdown-like tool-card text bodies via gated content-render options, and add a CI construction guard so `normalizeAgentFences` cannot reappear outside the documented allowlist.

**Blocked by:** 02 — Unified agent fence call graph

**Status:** done

- [x] `ContentRenderOptions.forToolMarkdownText(text)` enables `applyFenceNormalization` when text contains `` ``` `` or `likelyContainsMarkdown` is true; plain tool dumps stay on `DEFAULT` (no normalization)
- [x] Completed tool-card text and embedded text resources route through `forToolMarkdownText` instead of `DEFAULT`
- [x] `TranscriptContentRendererTest` covers tool malformed fences normalizing to code blocks when gated; plain pre text without fences stays unchanged
- [x] `AgentFenceNormalizationConstructionTest` fails when production code calls `normalizeAgentFences(` outside `{TranscriptAgentFenceNormalizer, TranscriptContentRenderer, AgentTextRowAdapter}`
- [x] Wiki pipeline diagram reflects single normalizer seam and tool-text gating policy
- [x] `./gradlew qualityGate` passes
