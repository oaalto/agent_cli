# 02 — Unified agent fence call graph

**Parent:** `prd.md`

**What to build:** Route agent fence normalization through one production call graph for streaming and final agent text. Remove duplicate pre-parse hooks and dead binder code so a normalizer fix applies to both the live streaming preview and the finalized body-part render without double application.

**Blocked by:** 01 — Fence normalizer deep module

**Status:** done

- [x] Streaming agent text: `AgentTextRowAdapter` calls `normalizeAgentFences` then appends `TranscriptStreamingCursor.CURSOR_CHAR` in plain `JTextPane` text — cursor stays outside the normalizer
- [x] Final agent text: `TranscriptContentRenderer` with `ContentRenderOptions.AGENT_TEXT` applies `normalizeAgentFences` as the first pipeline stage before `parseToBlocks`
- [x] `TranscriptMarkdownRenderer.parseToBlocks` does **not** call `normalizeAgentFences` (content renderer is the sole pre-parse hook for final markdown)
- [x] Dead `TranscriptBlockLabelBinder` removed (zero callers after row-adapter split)
- [x] `TranscriptContentRendererTest` covers agent malformed fences normalizing to highlighted code blocks end-to-end
- [x] `./gradlew qualityGate` passes; fence **text** parity between streaming preview and final render for repaired patterns
