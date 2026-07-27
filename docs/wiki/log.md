# Engineering Wiki Log

## [2026-07-24] skip | ACP transcript block alignment fix

- Reason: Visual layout fix only; no behavioral or architectural facts changed in the wiki.

## [2026-07-24] update | Remove dead `onTranscriptHtml`/`onTranscriptPlainLine` references from context wiki

- Updated: [Domain context & ACP transcript model](concepts/context.md)
- Sources: `docs/features/transcript-pipeline-consolidation/prd.md`, `docs/features/transcript-pipeline-consolidation/02-align-acp-transcript-wiki.md`
- Notes: Removed obsolete `onTranscriptHtml` and `onTranscriptPlainLine` from streaming finalize triggers list. `onError` retained (still live in `AcpAgentEditor`). All `TranscriptHtmlAppender`/`TranscriptPaneHtmlOps`/`committedBodyHtml`/`streamingPlainText` references already removed in prior update.

## [2026-07-24] update | ACP transcript wiki aligns with live stack

- Updated: [ACP client subsystem](subsystems/acp-client.md), [Domain context & ACP transcript model](concepts/context.md)
- Sources: `src/main/kotlin/com/oaalto/agent/acp/TranscriptViewController.kt`, `src/main/kotlin/com/oaalto/agent/acp/TranscriptModel.kt`, `src/main/kotlin/com/oaalto/agent/acp/TranscriptPanel.kt`, `src/main/kotlin/com/oaalto/agent/acp/TranscriptBlockViewFactory.kt`, `src/main/kotlin/com/oaalto/agent/acp/TranscriptBlockLabelBinder.kt`, `src/main/kotlin/com/oaalto/agent/acp/AcpEditorLayout.kt`, `docs/features/transcript-pipeline-consolidation/prd.md`
- Notes: Removed dead `TranscriptHtmlAppender` references from rendering stack, sources, and agent synthesis. Replaced obsolete `committedBodyHtml`/`streamingPlainText` facts with live `StreamingAgentText`/`CURSOR_CHAR` description. Updated `transcriptArea` layout from `JEditorPane` to `TranscriptPanel` + `JBScrollPane`. Attributed transcript ownership to `TranscriptViewController` in context wiki.

## [2026-07-24] update | ACP transcript wiki reflects ingestion consolidation

- Updated: [ACP client subsystem](subsystems/acp-client.md), [Domain context & ACP transcript model](concepts/context.md)
- Sources: `src/main/kotlin/com/oaalto/agent/acp/TranscriptEventIngestion.kt`, `src/main/kotlin/com/oaalto/agent/acp/AcpClientSessionOperationsImpl.kt`, `src/main/kotlin/com/oaalto/agent/acp/AcpSessionControllerImpl.kt`, `docs/features/transcript-pipeline-consolidation/prd.md`
- Notes: Replaced `AcpPromptEventDispatcher` / `TranscriptSessionUpdateMapper` with `TranscriptEventIngestion` in subsystem and context wikis. Sources list now points at the consolidated ingestion module. Routing and mapping sections describe `ingest()` / `ingestPromptCompleted()` instead of the deleted `dispatchSessionUpdate` / `mapUpdate` pair. Closed open question about `notify()` routing consistency (now absorbed into single seam).

## [2026-07-24] skip | ACP session transcript persistence wiki

- Reason: Feature behavior documented in ADR 0004, PRD, and changelog; subsystem wiki pages not yet updated for transcript file paths and copy-diagnostics action.

## [2026-07-24] skip | Tiered session diagnostics ticket status

- Reason: Planning artifact status-only update (`done` / `implemented`); behavior already recorded in changelog and ADR 0004.

## [2026-07-24] skip | AgentCliLog call-site migration (PR1)

- Reason: Tiered logging behavior documented in ADR 0004 and tiered-session-diagnostics PRD; wiki subsystem pages not yet updated for `AgentCliLog` usage patterns.

## [2026-07-24] skip | AgentCliLog tier gate infrastructure

- Reason: New diagnostics helper and registry keys only; ADR 0004 and tiered-session-diagnostics PRD already document the model. Wiki update deferred until call-site migration lands.

## [2026-07-24] skip | ACP transcript block alignment fix

- Reason: Surgical Swing layout fix (`preferredSize` on resize); no durable domain or subsystem knowledge change beyond existing transcript layout docs.

## [2026-07-24] update | ACP transcript features & workflow refresh

- Updated: [ACP client subsystem](subsystems/acp-client.md), [Domain context & ACP transcript model](concepts/context.md), [Quality gate & release workflow](workflows/quality-gate.md), [Agent CLI overview](concepts/agent-cli-overview.md)
- Sources: `src/main/kotlin/com/oaalto/agent/acp/TranscriptColorProvider.kt`, `src/main/kotlin/com/oaalto/agent/acp/TranscriptFooter.kt`, `src/main/kotlin/com/oaalto/agent/acp/TranscriptMarkdownRenderer.kt`, `src/main/kotlin/com/oaalto/agent/acp/TranscriptSessionUpdateMapper.kt`, `src/main/kotlin/com/oaalto/agent/acp/ui/PromptInputBar.kt`, `src/main/kotlin/com/oaalto/agent/acp/plan/PlanPanel.kt`, `CONTEXT.md`, `build.gradle.kts`, `scripts/pre-commit`
- Notes: Documented theme-aware colors, footer usage/cost bar, plan visualization, slash-command autocomplete, GFM Markdown rendering, CONTEXT glossary trim, and pre-commit/wiki-lint/graphify workflow.

## [2026-07-23] ingest | ACP client subsystem

- Updated: [ACP client subsystem](subsystems/acp-client.md)
- Sources: `src/main/kotlin/com/oaalto/agent/acp/AcpAgentEditor.kt`, `src/main/kotlin/com/oaalto/agent/acp/AcpClientSessionOperationsImpl.kt`, `src/main/kotlin/com/oaalto/agent/acp/AcpEditorLayout.kt`, `src/main/kotlin/com/oaalto/agent/acp/TranscriptViewController.kt`, `src/main/kotlin/com/oaalto/agent/acp/TranscriptHtmlAppender.kt`, `src/main/kotlin/com/oaalto/agent/acp/AcpPromptEventDispatcher.kt`
- Notes: ACP client slice — UI layout (72/28 split), transcript rendering stack, prompt event routing, session operations, structured update hierarchy.

## [2026-06-16] ingest | Agent CLI overview

- Updated: [Agent CLI overview](concepts/agent-cli-overview.md)
- Sources: `src/main/kotlin/com/oaalto/agent/`, `build.gradle.kts`
- Notes: Initial concept page for plugin purpose, layout, and PRD integration.

## [2026-06-16] ingest | Worktree subsystem

- Updated: [Worktree subsystem](subsystems/worktree.md)
- Sources: `src/main/kotlin/com/oaalto/agent/worktree/AgentWorktreePathMapper.kt`, `src/test/kotlin/com/oaalto/agent/worktree/AgentWorktreePathMapperTest.kt`
- Notes: Path mapping and isolated session filesystem layout.

## [2026-06-24] ingest | Domain context & ACP transcript model

- Updated: [Domain context & ACP transcript model](concepts/context.md)
- Sources: `CONTEXT.md`
- Notes: ACP transcript rendering stack and domain navigation for agents.

## [2026-06-24] ingest | Architecture decisions map

- Updated: [Architecture decisions map](subsystems/architecture.md)
- Sources: `docs/adr/0001-custom-acp-client-in-plugin.md`, `docs/adr/0002-kotlin-acp-sdk.md`, `docs/adr/0003-per-project-agent-selection.md`
- Notes: ADR index and accepted architectural decisions.

## [2026-06-24] ingest | Quality gate & release workflow

- Updated: [Quality gate & release workflow](workflows/quality-gate.md)
- Sources: `build.gradle.kts`, `.github/workflows/build-plugin.yml`
- Notes: `qualityGate` task chain and CI build-plugin workflow.

## [2026-06-26] update | Wiki log backfill

- Updated: [Engineering Wiki Log](log.md)
- Sources: existing wiki pages under `docs/wiki/`
- Notes: Backfilled ingest history after ADC update; no page content changes.

## [2026-06-26] update | Domain context & ACP transcript model

- Updated: [Domain context & ACP transcript model](concepts/context.md)
- Sources: `CONTEXT.md`
- Notes: CONTEXT trimmed to glossary; this page retains ACP transcript implementation detail.

## [2026-06-26] update | Agent CLI overview

- Updated: [Agent CLI overview](concepts/agent-cli-overview.md)
- Sources: `CONTEXT.md`
- Notes: PRD path corrected to `docs/features/`.
