# Engineering Wiki Log

## [YYYY-MM-DD] operation | Title

- Updated: [Page](path/to/page.md)
- Sources: `path-or-url`
- Notes: {short summary}

## [2026-06-16] ingest | Initial wiki pages created

- Updated: [Agent CLI overview](concepts/agent-cli-overview.md)
- Updated: [Worktree subsystem](subsystems/worktree.md)
- Sources: `src/main/kotlin/com/oaalto/agent/`, `build.gradle.kts`
- Notes: Created initial wiki pages from verified repository sources as part of agent bundle install.

## [2026-06-19] update | ACP transcript HTML rendering PRD

- Updated: `docs/prd/acp-transcript-html-rendering.md`, `docs/acp-output-rendering-roadmap.md`
- Sources: `docs/acp-output-rendering-roadmap.md`, `src/main/kotlin/com/oaalto/agent/acp/`
- Notes: PRD for Step 1 of output rendering roadmap — replace JBTextArea with JEditorPane for color-coded sources. Roadmap status updated to link PRD.

## [2026-06-16] skip | 3.0 ACP ADRs recorded in docs/adr

- Updated: —
- Sources: `docs/adr/0001-custom-acp-client-in-plugin.md`, `docs/adr/0002-kotlin-acp-sdk.md`, `CONTEXT.md`
- Notes: Design decisions captured as ADRs; wiki subsystem pages not updated until implementation lands.

## [2026-06-24] ingest | Path-map required wiki pages

- Updated: [Domain context & ACP transcript model](concepts/context.md)
- Updated: [Architecture decisions map](subsystems/architecture.md)
- Updated: [Engineering Wiki Index](index.md)
- Sources: `CONTEXT.md`, `docs/adr/0001-custom-acp-client-in-plugin.md`, `docs/adr/0002-kotlin-acp-sdk.md`, `docs/adr/0003-per-project-agent-selection.md`
- Notes: Created required path-map pages so `scripts/wiki-lint.mjs` passes in CI.

## [2026-06-24] update | Replace Qodana with expanded detekt

- Updated: [Quality gate & release workflow](workflows/quality-gate.md)
- Sources: `build.gradle.kts`, `detekt.yml`, `.agents/rules/workflow-gates.md`
- Notes: Removed Qodana workflow; detekt type-resolution tasks are now the primary CI static-analysis gate.
