# Engineering Wiki Log

## [2026-08-05] skip | Transcript theme package move (slice 01)

- No wiki update: grill-accept already documents `transcript/theme/` layout; slice 01 is moves and import fixes only.

## [2026-08-05] update | ACP transcript package restructure (grill accept)

- Updated: [ACP client subsystem](subsystems/acp-client.md), [Domain context & ACP transcript model](concepts/context.md), [CONTEXT.md](../../CONTEXT.md), [package-restructure PRD](../../docs/features/acp-transcript-package-restructure/prd.md)
- Sources: grill-with-docs-batch acceptance for `acp-transcript-package-restructure` PRD
- Notes: `transcript/{model,render,view,theme}` layout; dependency matrix; `view/rows/` for adapters; ingestion→render exception; session transcript file stays at `acp/` root; label binder obsolete.

## [2026-08-05] update | Agent fence normalization unification (grill accept)

- Updated: [ACP client subsystem](subsystems/acp-client.md), [CONTEXT.md](../../CONTEXT.md), [fence-normalization PRD](../../docs/features/acp-transcript-fence-normalization/prd.md)
- Sources: `TranscriptAgentFenceNormalizer.kt`, `TranscriptContentRenderer.kt`, `AgentTextRowAdapter.kt`, `AgentFenceNormalizationConstructionTest.kt`
- Notes: Single `normalizeAgentFences` entry; removed duplicate parseToBlocks hook and dead `TranscriptBlockLabelBinder`; tool text uses `forToolMarkdownText`; CI allowlist guard.

## [2026-08-05] update | FinalizeAgentStream construction grep enforcement (slice 02)

- Updated: [ACP client subsystem](subsystems/acp-client.md), [Domain context & ACP transcript model](concepts/context.md)
- Sources: `02-grep-enforcement.md`, `FinalizeAgentStreamConstructionTest.kt`, `TranscriptFinalizePolicy.kt`
- Notes: CI allowlist documented; `FinalizeAgentStreamConstructionTest` scans `src/main/kotlin`; open-question entries removed.

## [2026-08-05] skip | Transcript finalize policy implementation (slice 01)

- No wiki update: code landed matching prior grill-accept wiki/ADR; grep enforcement deferred to slice 02.

## [2026-08-05] update | Transcript finalize policy (grill accept)

## [2026-08-05] update | Harness review follow-ups

- Updated: [ACP client subsystem](subsystems/acp-client.md)
- Sources: `01-harness-review-follow-ups.md`, `TranscriptPanelTestHarness.kt`, `TranscriptPanelHarnessTest.kt`, `TranscriptEdtTestSupport.kt`
- Notes: EDT-safe harness helpers; golden scenarios on ViewController `apply`; hierarchy scroll ownership and horizontal scrollbar tree checks; `applyIngest` finalize-ordering scenario; shared `collectTranscriptDescendants`.

## [2026-08-04] update | Transcript panel integration test harness

- Updated: [ACP client subsystem](subsystems/acp-client.md)
- Sources: grill-with-docs-batch acceptance for `acp-transcript-panel-integration-tests` PRD, `TranscriptPanelTestHarness.kt`, `TranscriptPanelHarnessTest.kt`, `TranscriptViewController.kt`
- Notes: `TranscriptPanelTestHarness` is the canonical mounted-panel seam; default `apply()` path through ViewController; `syncBlocks()` for view-only regressions; headless `PlainMonospace` default; five golden scenarios; row-adapter tests complement harness.

## [2026-08-05] skip | Third review follow-ups (ticket 08)

- No wiki update: test fixes and changelog corrections only; no durable knowledge changes.

## [2026-08-04] update | Body-part widget mapper and agent text adapter implementation

- Updated: [Domain context & ACP transcript model](concepts/context.md), [ACP client subsystem](subsystems/acp-client.md)
- Sources: `TranscriptBodyPartWidgetMapper.kt`, `AgentTextRowAdapter.kt`, `CollapsibleToolPanel.kt`, `TranscriptBlockViewFactory.kt`
- Notes: `TranscriptBodyPartWidgetMapper` with `BodyPartRenderProfile` enum unifies body-part → widget rendering; `AgentTextRowAdapter` handles StreamingAgentText and FinalAgentText; factory uses pure adapter dispatch; `CollapsibleToolPanel` uses mapper with TOOL profile.

## [2026-08-04] update | Row adapter scaffold and simple text implementation

- Updated: [Domain context & ACP transcript model](concepts/context.md), [ACP client subsystem](subsystems/acp-client.md)
- Sources: `TranscriptBlockRowAdapter.kt`, `SimpleTextRowAdapter.kt`, `TranscriptBlockViewFactory.kt`, ADR 0005, ADR 0006
- Notes: `TranscriptBlockRowAdapter` interface and `RowContext` data class implemented; `SimpleTextRowAdapter` handles UserEcho, Thought, PlainLine, ErrorLine, AuthFailureLine; factory delegates to registered adapters with adapter-first dispatch.

## [2026-08-04] update | Transcript row adapter decomposition

- Updated: [Domain context & ACP transcript model](concepts/context.md), [ACP client subsystem](subsystems/acp-client.md)
- Sources: grill-with-docs-batch acceptance for `acp-transcript-block-view-decomposition` PRD, ADR 0005, ADR 0006, `CONTEXT.md`
- Notes: Row adapter registry pattern; `TranscriptPanel` owns `blockId` map; factory is stateless dispatcher; `TranscriptBodyPartWidgetMapper` planned; separate simple vs agent row shells.

## [2026-08-04] skip | Agent text content-renderer routing (ticket 02)

- Reason: Implementation follows existing PRD/wiki seam; no new durable architecture beyond ticket 01 docs.

## [2026-08-04] skip | ACP transcript content-renderer tickets

- Reason: Ticket slice files and FEATURES.md ticket table only; PRD and wiki already document the content-render seam.

## [2026-08-04] update | Transcript content renderer seam

- Updated: [Domain context & ACP transcript model](concepts/context.md), [ACP client subsystem](subsystems/acp-client.md)
- Sources: grill-with-docs-batch acceptance for `acp-transcript-content-renderer` PRD, `CONTEXT.md`
- Notes: `TranscriptContentRenderer.renderMarkdownText` is canonical markdown→body-part seam; one parse pipeline, two post-parse paths today; `ContentRenderOptions` and sibling PRD landing order documented.

## [2026-08-04] update | ACP transcript layout scroll ownership

- Updated: [ACP client subsystem](subsystems/acp-client.md)
- Sources: `AcpEditorLayout.kt`, `TranscriptPanel.kt`
- Notes: `transcriptArea` is `TranscriptPanel`'s own `JBScrollPane` in CENTER; removed obsolete outer wrapper that caused nested scrollbars.

## [2026-08-04] skip | Session transcript timestamped filenames

- Reason: Filename presentation change in `TranscriptFileStore`; logical key and restore behavior already documented in ADR 0004 and CONTEXT.md.

## [2026-08-04] skip | ACP transcript auto-scroll

- Reason: Bug fix in `TranscriptPanel` stick-to-bottom timing only; no durable wiki fact change beyond existing scroll-on-stream behavior.

## [2026-08-03] update | ACP mid-line fences and code selection

- Updated: [ACP client subsystem](subsystems/acp-client.md)
- Sources: `TranscriptAgentFenceNormalizer.kt`, `TranscriptMarkdownRenderer.kt`, `TranscriptCodeBlockViewFactory.kt`, `TranscriptBlockViewFactory.kt`
- Notes: Split fences when prose precedes ``` on the same line; preserve code newlines from parser text nodes; enable selection in read-only code blocks.


- Reason: Bug fix in `applyStartResult` only; transcript persistence behavior already documented in ACP client wiki.

## [2026-08-03] update | ACP code block zero-height and finalize

- Updated: [ACP client subsystem](subsystems/acp-client.md)
- Sources: `TranscriptCodeBlockViewFactory.kt`, `TranscriptBlockViewFactory.kt`, `AcpPromptExecutor.kt`, `TranscriptBlockLabelBinder.kt`
- Notes: Font-metrics fallback when Editor lineHeight is 0; finalize stream after prompt collect; streaming fence normalization.

## [2026-08-03] update | ACP code block initial height

- Updated: [ACP client subsystem](subsystems/acp-client.md)
- Sources: `TranscriptCodeBlockViewFactory.kt`, `TranscriptAgentFenceNormalizer.kt`
- Notes: Editor-based code blocks now measure height at creation; normalizer auto-closes trailing unclosed fences.

## [2026-08-03] update | ACP merged opening fence normalization

- Updated: [ACP client subsystem](subsystems/acp-client.md)
- Sources: `TranscriptAgentFenceNormalizer.kt`, `TranscriptMarkdownRenderer.kt`
- Notes: Documented opening-fence split when language tag merges with first code line.

## [2026-08-03] update | ACP inline closing fence normalization

- Updated: [ACP client subsystem](subsystems/acp-client.md)
- Sources: `TranscriptAgentFenceNormalizer.kt`, `TranscriptMarkdownRenderer.kt`
- Notes: Documented `normalizeAgentFences` for agent markdown with mid-line closing fences.

## [2026-08-03] update | ACP fenced code block resize reflow

- Updated: [ACP client subsystem](subsystems/acp-client.md)
- Sources: `TranscriptCodeBlockViewFactory.kt`, `TranscriptColumnSizing.kt`
- Notes: Documented Editor-backed fenced code reflow on transcript column resize.

## [2026-08-03] update | Worktree pending-launch handoff module

- Updated: [Worktree subsystem](subsystems/worktree.md)
- Sources: `WorktreePendingLaunchHandoff.kt`, `docs/features/worktree-pending-launch-handoff/prd.md`
- Notes: Documented schedule/consume lifecycle and handoff entry points.

## [2026-08-03] skip | Merge STATUS.md into FEATURES.md

- Reason: Planning-doc consolidation only; no runtime or architectural wiki facts changed.

## [2026-08-03] update | Quality gate wiki notes daemon JVM pin for detekt

- Updated: [Quality gate & release workflow](workflows/quality-gate.md)
- Sources: `gradle/gradle-daemon-jvm.properties`, `docs/development.md`
- Notes: Documented `gradle-daemon-jvm.properties` JDK 21 daemon criteria so detekt 1.23.x works when the host default JDK is 25+.

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

## [2026-07-28] update | ACP client session operations deep module

- Updated: [ACP client subsystem](subsystems/acp-client.md)
- Sources: `docs/features/acp-client-operations-wiring/prd.md`
- Notes: Session operations section now references `SessionFilesystemOperations` deep module, `AcpClientSessionOperationsFactory` composition root, and `ScopedFileSystemAccess` VFS seam. Test surface at deep module interface documented.

## [2026-08-04] skip | ACP transcript block-view decomposition

- Sources: `docs/features/acp-transcript-block-view-decomposition/prd.md`, tickets 01–05
- Notes: Wiki already updated during tickets 01–04 (ADR 0005/0006, row adapter glossary, coordinator shape). No further wiki changes for ticket 05 (coordinator cleanup and verification).
