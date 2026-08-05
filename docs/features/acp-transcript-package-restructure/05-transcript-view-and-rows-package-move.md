# 05 — Transcript view and row adapters package move

**Parent:** `prd.md`

**What to build:** Relocate the Swing transcript UI stack (`TranscriptViewController`, `TranscriptPanel`, block view factory, footer, code-block view factory, column sizing, collapsible tool panel) into `transcript/view/`. Move row adapters (`SimpleText`, `AgentText`, `ToolCall`, `Plan`), `TranscriptBodyPartWidgetMapper`, and `TranscriptStreamingCursor` into `transcript/view/rows/` with the row-adapter interface remaining in `view/`. Mirror view and harness tests. Update orchestration entry points so the live transcript panel still renders and updates exactly as today.

**Blocked by:** 04 — Transcript event ingestion model move

**Status:** done

- [x] View and row-adapter types plus harness tests live under `transcript/view` and `transcript/view/rows`
- [x] Adapter registry dispatch order unchanged; mounted-panel harness scenarios pass
- [x] `TranscriptFooter` usage updates still flow from `StructuredUpdate.Usage`
- [x] `./gradlew qualityGate` passes with zero visible transcript UX change
