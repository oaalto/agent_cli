# 02 — Route agent final text through content renderer

**Parent:** `prd.md`

**What to build:** Final agent markdown rows produce `TranscriptBodyPart` lists via `TranscriptContentRenderer` instead of a parallel `RenderedBlock` → Swing widget path. Agent prose, fenced code, tables, blockquotes, and inline styles in `FinalAgentText` rows match tool-card body-part semantics because both callers share the same conversion seam.

**Blocked by:** 01 — Introduce content renderer for tool text bodies

**Status:** ready-for-agent

- [ ] `FinalAgentText` binding calls `renderMarkdownText` with agent-appropriate `ContentRenderOptions` (`applyFenceNormalization` on; markdown heuristic off or equivalent to current agent behaviour)
- [ ] `AgentTextRow` maps `TranscriptBodyPart` variants to Swing components (headings, list markers, code blocks, tables, blockquotes) with the same visual result as today — widget mapping may stay local to the row until block-view decomposition extracts a shared mapper
- [ ] Duplicate `RenderedBlock` → widget conversion logic in the agent row path is removed or reduced to thin delegation over body parts
- [ ] Markdown regression cases from `TranscriptMarkdownRendererTest` and `TranscriptFencedAgentTextLimitsTest` are migrated or duplicated at the `TranscriptContentRenderer` interface (assert on `TranscriptBodyPart` shapes, not AST nodes)
- [ ] Streaming agent text (`StreamingAgentText`) behaviour is unchanged
- [ ] `./gradlew qualityGate` passes; no visible agent-text UX change
