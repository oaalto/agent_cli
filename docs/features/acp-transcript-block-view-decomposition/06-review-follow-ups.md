# 06 — Code review follow-ups

**Parent:** `prd.md`

**What to build:** Close gaps found in the post-implementation code review (standards + spec axes) so block-view decomposition fully matches tickets 01–05, ADR 0005/0006, and the PRD — without visible transcript UX change. Restore lost test coverage, fix simple-text adapter contract bugs, align column-resize behaviour across row families, and add missing agent-text and mapper regression tests.

**Blocked by:** None — can start immediately

**Status:** done

- [x] **Simple-text type mismatch:** `SimpleTextRowAdapter.update()` now checks `matches(block)` first; logs mismatch when block is not simple-text family; row not silently rebound
- [x] **Simple-text presentation seam:** `SimpleTextRow.bind()` now delegates to `bindTranscriptBlock` (no duplicated inline colour/text logic)
- [x] **Column resize parity:** `SimpleTextRow` resize handler uses `applyTranscriptColumnWidth` (same shared helper as agent/tool rows); wrapped text remeasures height on column width change
- [x] **Agent text finalize path:** Added headless tests for streaming → finalized agent text at same `blockId`: in-place rebuild, stable child count, code-editor disposal, cursor removal
- [x] **Mapper regression coverage:** New `TranscriptBodyPartWidgetMapperTest` with dedicated tests for code blocks, HTML fallback, blockquote, highlight-budget, agent/tool profile parity, gaps, headings, images, list lines
- [x] **Tool-card test migration:** Added tests for collapsed tool card skipping code-block creation and expanded tool card body remeasuring children on resize
- [x] **Link-open diagnostics:** `tryOpenUrl` now logs warning on failure with specific exception types (URISyntaxException, HeadlessException, IOException)
- [x] **Coordinator unknown-block path:** Removed broken undocumented `AgentTextRowAdapter` fallback; returns empty `JPanel` with warning log
- [x] **Row context shape:** Added optional `project: Project?` field to `RowContext` and `TranscriptBlockViewFactory`; wired through `TranscriptPanel.create()`
- [x] `./gradlew qualityGate` passes; zero visible transcript UX change across all block families
