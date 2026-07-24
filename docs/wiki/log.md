# Engineering Wiki Log

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
