# 04 — Body-part widget mapper and agent text row adapter

**Parent:** `prd.md`

**What to build:** Extract `AgentTextRowAdapter` for streaming and finalized agent text with its own multi-part row shell (content column + body-part widgets per ADR 0006). Introduce `TranscriptBodyPartWidgetMapper` with `BodyPartRenderProfile.AGENT` vs `.TOOL` so agent rows and tool card bodies share code/html/blockquote mapping while profile-specific fallbacks remain explicit. Streaming cursor display, finalize rebuild, hyperlink handling, and code-editor disposal live in the agent adapter. Migrate tool card body rendering to the shared mapper.

**Blocked by:** 02 — Tool call row adapter

**Status:** done

- [x] `AgentTextRowAdapter` `matches` `StreamingAgentText` and `FinalAgentText` on the agent row shell; simple text blocks never match here
- [x] Finalized agent text routes markdown through `TranscriptContentRenderer.renderMarkdownText` then mapper → widgets; streaming text uses label binder in place until finalize triggers full rebuild at the same `blockId`
- [x] `TranscriptBodyPartWidgetMapper` unifies shared part kinds (code, HTML, blockquote) across profiles; agent-only parts (headings, lists, images) and tool-specific HTML fallbacks use profile flags — not duplicate mappers
- [x] `ToolCallRowAdapter` body lazy-build delegates to mapper with `BodyPartRenderProfile.TOOL`
- [x] Hyperlink open behaviour scoped to agent adapter; link utilities moved out of factory companion
- [x] Headless EDT tests: streaming in-place update without child-count explosion; finalize rebuild; code editors disposed on row removal; mapper parity smoke for tool vs agent on shared part kinds
- [x] `./gradlew qualityGate` passes; zero visible agent-text or tool-body UX change
