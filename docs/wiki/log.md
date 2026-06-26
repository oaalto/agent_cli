# Engineering Wiki Log

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
