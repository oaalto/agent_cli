# Changelog

## 2026-08-03

### Fixed

- **WSL resume context test on Linux** (`WorktreeLaunchCoordinatorTest`): Use a Windows drive-letter worktree path so kernel `/mnt/` mapping is asserted portably instead of a Linux temp path. made by: Olli Aalto. made with: Cursor. model: composer-2.5-fast

- **Detekt on JDK 25 hosts** (`gradle/gradle-daemon-jvm.properties`): Pin Gradle daemon to JDK 21 via `updateDaemonJvm` so detekt 1.23.x runs on a supported JVM when the system default is Java 25. made by: Olli Aalto. made with: Cursor. model: composer-2.5-fast

### Changed

- **Graphify AST-only re-index** (`graphify-out/`): Ran `graphify update .` (AST extraction only, no LLM backend). Graph now at commit `e7b0aa5e` — 3204 nodes, 4644 edges. made by: Olli Aalto. made with: Cursor. model: composer-2.5-fast

- **zoom-out upstream lock removed** (`skills-lock.json`): Dropped mattpocock/skills upstream entry; bundled copy at `.agents/skills/zoom-out/SKILL.md` is canonical. made by: Olli Aalto. made with: Cursor. model: composer-2.5-fast

### Documentation

- **ADC update tailoring** (`.agents/skills/`, `AGENTS.md`, `docs/agents/issue-tracker.md`): Re-tailored graphify, wiki, repo-navigation, and workflow skills for Kotlin/Gradle layout; added bundled `to-spec`/`to-tickets` skills using `docs/features/` co-located PRD + slice convention; added `grill-with-docs-batch` skill; restored issue-tracker paths and restricted-operations repo notes. made by: Olli Aalto. made with: Cursor. model: composer-2.5-fast

- **Feature backlog upkeep** (`.pi/skills/implement/`, `.pi/skills/to-tickets/`, `.pi/skills/to-spec/`, `docs/agents/issue-tracker.md`): Skills and issue-tracker docs now instruct agents to keep `docs/features/STATUS.md` current when tickets ship or new features are published. made by: Olli Aalto. made with: Cursor. model: composer-2.5-fast

- **Feature backlog master status** (`docs/features/STATUS.md`): Rollup of all ready-for-agent features (21 tickets across 5 features) in recommended implementation order, with resume-orchestration vs controller-deepening conflict guidance. made by: Olli Aalto. made with: Cursor. model: composer-2.5-fast

- **Transcript pipeline consolidation tickets** (`docs/features/transcript-pipeline-consolidation/`): Verified implementation of tickets 01–03 (dead HTML path deleted, wiki aligned, `TranscriptEventIngestion` merged) and marked all three as `done`. made by: Olli Aalto. made with: Cursor. model: composer-2.5-fast

## 2026-07-28

### Changed

- **ACP client session operations deepening** (`acp/`): Introduce `SessionFilesystemOperations` deep module owning scope → permission → VFS → line-slicing policy behind one seam. Extract `ScopedFileSystemAccess` VFS interface with `IdeScopedFileSystemAccess` as production adapter and `InMemoryScopedFileSystemAccess` test double. Extract `AcpClientSessionOperationsFactory` composition root replacing the inline companion `create()` in `AcpClientSessionOperationsImpl`. `AcpSessionControllerImpl` depends on the factory interface; `openSession()` invokes the injected factory. Tests at the deep module interface cover in-scope reads, out-of-scope rejections, permission denials, VFS read-only/ignored blocks, and line/limit slicing. Wiki updated. made by: Olli Aalto. made with: pi.

## 2026-07-27

### Changed

- **ACP start result parity** (`acp/`): Add `restoreTranscript` to `AcpSessionStartResult` so transcript restore/bind runs only on successful load/resume; track `activeSessionId` in `AcpAgentEditor` for post-start log context. made by: Olli Aalto. made with: Cursor. model: composer-2.5-fast

- **ACP session controller deepening (tickets 05–07)** (`acp/`): Move resume orchestration from `AcpAgentEditor` into `AcpSessionLifecycle` driven by injected `SessionPicker`; shrink `AcpSessionController` interface from eight methods (`connect`, `newSession`, `loadSession`, `listSessions`, `currentSessionId`, `prompt`, `cancelPrompt`, `dispose`) to four (`start`, `prompt`, `cancelPrompt`, `dispose`); introduce `AcpSessionStartRequest` and `AcpSessionStartResult` so editor startup is a single call; `AcpSessionControllerImpl` becomes a thin coordinator composing `AcpProcessTransport`, `AcpConnectionBootstrap`, `AcpSessionLifecycle`, and `AcpPromptExecutor`. `AcpAgentEditor.launchAndConnect` now calls `sessionController.start()` once; worktree session persistence stays in the editor. Delete `AcpSessionOperationsAdapter` (no longer needed). `RecordingSessionController` test fake updated to new interface. `./gradlew qualityGate` passes. made by: Olli Aalto. made with: pi.

### Documentation

- **Feature master list ordering** (`docs/features/FEATURES.md`, `docs/agents/issue-tracker.md`, `.pi/skills/`): Split FEATURES.md into Active (implementation order) and Implemented sections; skills now require agents to infer and maintain priority order from PRDs and dependencies rather than appending or asking the human. made by: Olli Aalto. made with: Cursor. model: composer-2.5-fast

- **acp-session-transcript-persistence status** (`docs/features/`): Mark PRD `implemented` and move feature from Active to Implemented in FEATURES.md — all slices were already done. made by: Olli Aalto. made with: Cursor. model: composer-2.5-fast

## 2026-07-24

### Added

- **Settings session logs and transcripts help panel** (`settings/`, `acp/`): Fixed **Session logs & transcripts** section in **Tools → Agent CLI** settings between table and detail panel; IDE log hint with platform-appropriate Explorer/Finder wording and `[agent-cli:…]` grep; **Open transcript folder** button that resolves directory via shared `TranscriptFileStore` companion, creates if missing, and reveals in file manager; focused project resolution for transcript path; unit test for directory resolver. made by: Olli Aalto. made with: pi.

- **Settings observability panel layout** (`settings/`): Move observability section below the agent configuration table (between table and per-row detail panel) to match spec; center area now stacks table + observability vertically. made by: Olli Aalto. made with: pi.

- **ACP session transcript persistence** (`acp/`): Workspace-local session transcript files under `.idea/agent-cli/transcripts/<acpSessionId>.txt`, plain-text serializer, debounced snapshot writer, plain-line restore on resumed sessions, correlation tokens on transcript errors, and **Copy Session Diagnostics** editor action. made by: Olli Aalto. made with: Cursor. model: composer-2.5-fast

- **AgentCliLog infrastructure** (`com.oaalto.agent`): Tiered session diagnostics helper with `AgentCliSessionContext`, env/registry gate functions (`AGENT_CLI_LOG`, `AGENT_CLI_DEBUG`, `agent_cli.log`, `agent_cli.debug`), lazy tier-2/3 evaluation, and secret redaction helpers. made by: Olli Aalto. made with: Cursor. model: composer-2.5-fast

### Changed

- **Logger migration to AgentCliLog** (`com.oaalto.agent`, `acp/`, `pty/`, `worktree/`): Migrate existing `Logger` call sites to `AgentCliLog` with session context; tier-2 `info` for session open, MCP resolution, and process exit; `PlanUpdateMapper` reflection failures moved from `warn` to tier-3 `debug`. made by: Olli Aalto. made with: Cursor. model: composer-2.5-fast

- **Dialog-only failure logging** (`worktree/`, `settings/`, `pty/`, `acp/`): Tier-1 `AgentCliLog` lines alongside existing error dialogs for worktree create/delete/open, settings import/export, PTY and ACP embedded-terminal launch failures, and degraded ACP resume paths. made by: Olli Aalto. made with: Cursor. model: composer-2.5-fast

- **ACP transcript pipeline consolidation — Phase 1** (`acp/`): Delete orphaned `TranscriptHtmlAppender`, `TranscriptPaneHtmlOps`, and their tests; trim `TranscriptStreamingCursor` to live-path `CURSOR_CHAR` only (drop `streamBlockHtml`, `finalizedBlockHtml`, `stripCursor`, `hasCursor`, `CURSOR_HTML`); drop dead `userPromptSpan`/`plainLineSpan` from `TranscriptRenderHelpers`. Wiki alignment and ingestion merge in follow-up tickets. made by: Olli Aalto. made with: Cursor. model: composer-2.5-fast

- **ACP transcript wiki alignment** (`docs/wiki/`): Remove remaining `onTranscriptHtml`/`onTranscriptPlainLine` dead listener references from context wiki; zero dead-path references remain across wiki. made by: Olli Aalto. made with: pi.

- **Unified launch resolution — slice adapter migration** (`worktree/`, `pty/`, `acp/`): `WorktreeLaunchCoordinator` delegates path and execution-target resolution to new kernel `AgentLaunchResolver`; `buildResumeContext` now returns `Result<ResumeContext>` propagating kernel failures; `buildLaunchContext` returns `Result<AgentLaunchContext>`; `AgentPendingLaunchStartupActivity` shows error dialog on resolution failure; `PtyResumeStrategy` deletes private `resolveExecutionTarget` and uses `ExecutionTarget.from`; `AcpProcessLauncher`, `PtyAgentEditor`, and `WorktreeLaunchCoordinator` no longer re-implement override precedence, WSL mapping, or distribution inference. made by: Olli Aalto. made with: pi.

- **Un-ignore `.pi-subagents/`** (`.gitignore`, `.piignore`): Stop excluding the `.pi-subagents/` directory from version control; agents need access to subagent artifacts. made by: Olli Aalto. made with: pi.

- **ACP session resume orchestration** (`worktree/resume/`, `acp/`): Extract resume orchestration from `AcpAgentEditor` into `AcpSessionResumeOrchestrator` in `worktree/resume/`. The editor is now a thin adapter: connect → orchestrator.openSession → map result to transcript lines. Port interfaces (`AcpSessionOperations`, `WorktreeSessionBinder`, `SessionPicker`) and adapters at each seam (`AcpSessionOperationsAdapter`, `WorktreeSessionBinderImpl`, `SessionPickerAdapter`) enable testable orchestration without Swing or live ACP. Plan variant `AcpPickSession` replaced with `AcpResolveSession` to match runtime behavior. Comprehensive orchestrator tests cover all branches with fake ports. made by: Olli Aalto. made with: pi.

### Removed

- **Orphaned HTML transcript rendering path** (`acp/`): Deleted `TranscriptHtmlAppender`, `TranscriptPaneHtmlOps`, `TranscriptHtmlAppenderStreamingTest`, `TranscriptStreamingCursorTest`; removed HTML cursor helpers (`streamBlockHtml`, `finalizedBlockHtml`, `stripCursor`, `hasCursor`, `CURSOR_HTML`) from `TranscriptStreamingCursor`; removed `userPromptSpan`/`plainLineSpan` helpers from `TranscriptRenderHelpers`. Live `TranscriptViewController` → `TranscriptModel` → `TranscriptPanel` path untouched. made by: Olli Aalto. made with: Cursor. model: composer-2.5-fast

- **Merge transcript event ingestion into single deep module** (`acp/`): Replace `AcpPromptEventDispatcher` + `TranscriptSessionUpdateMapper` with `TranscriptEventIngestion` exposing `ingest(SessionUpdate)` and `ingestPromptCompleted()`. Owns finalize-before-non-chunk policy, all `SessionUpdate` → `StructuredUpdate` mapping, and absorbed text-extraction helpers (`extractText`, `renderToolCallBodyParts`). `TranscriptRenderer` narrowed to pure formatting/utility. Migrated mapper tests; added finalize-on-non-chunk cases (`AgentMessageChunk` streams without finalize, all other events finalize-then-map). No behaviour change to live transcript rendering. made by: Olli Aalto. made with: pi.

- **TranscriptRenderer wrapper cleanup** (`acp/`): Removed `renderToolCallContentFragments` dead wrapper (zero production callers) from `TranscriptRenderer`; test calls now route directly to `TranscriptToolCallContentRenderer.renderContentFragments`. made by: Olli Aalto. made with: pi.

### Fixed

- **ACP editor tab close during connect** (`acp/AcpAgentEditor.kt`): Rethrow `CancellationException` instead of logging it as a session failure or showing a transcript error when the tab is closed while connect/resume is in flight. made by: Olli Aalto. made with: Cursor. model: composer-2.5-fast

- **ACP transcript block alignment** (`agent/acp/`): Update `preferredSize` for `JComponent` children in transcript row and tool card body resize paths so block content (code, tables, blockquotes) stays within the viewport instead of clipping right; shared sizing logic in `TranscriptColumnSizing.kt`. made by: Olli Aalto. made with: Cursor. model: composer-2.5-fast

### Documentation

- **Tiered session diagnostics ticket status** (`docs/features/tiered-session-diagnostics/`): Mark PR1 tickets 01–03 `done` and PRD `implemented`; optional migrated-component test in ticket 02 remains open. made by: Olli Aalto. made with: Cursor. model: composer-2.5-fast

- **Session observability ADR** (`docs/adr/0004-session-observability.md`, `CONTEXT.md`): Record split between transcript file (ACP, keyed by `acpSessionId`) and tiered IDE logging (`warn` always; `info`/`debug` via env or registry); two-PR delivery plan. made by: Olli Aalto. made with: Cursor. model: composer-2.5-fast

- **ACP transcript wiki alignment** (`docs/wiki/subsystems/acp-client.md`, `docs/wiki/concepts/context.md`): Remove dead `TranscriptHtmlAppender` references from rendering stack, sources, and agent synthesis; replace obsolete `committedBodyHtml`/`streamingPlainText` facts with live `StreamingAgentText`/`CURSOR_CHAR` description; update `transcriptArea` layout from `JEditorPane` to `TranscriptPanel` + `JBScrollPane`; attribute transcript ownership to `TranscriptViewController` in context wiki. made by: Olli Aalto. made with: pi.

- **Tiered session diagnostics PRD** (`docs/features/tiered-session-diagnostics/prd.md`): PR1 spec for `AgentCliLog`, three-tier IDE logging gates, migration of existing log sites, and dialog-only failure paths (`ready-for-agent`). made by: Olli Aalto. made with: Cursor. model: composer-2.5-fast

- **ACP session transcript persistence PRD** (`docs/features/acp-session-transcript-persistence/prd.md`): PR2 spec for workspace-local session transcript files, debounced snapshot writes, plain restore, correlation tokens, and copy diagnostics (`ready-for-agent`). made by: Olli Aalto. made with: Cursor. model: composer-2.5-fast

- **Tiered session diagnostics tickets** (`docs/features/tiered-session-diagnostics/01-03`): Tracer-bullet tickets for AgentCliLog gates, Logger migration, and dialog-only failure logging (`ready-for-agent`). made by: Olli Aalto. made with: Cursor. model: composer-2.5-fast

- **ACP session transcript persistence tickets** (`docs/features/acp-session-transcript-persistence/01-06`): Tracer-bullet tickets for serializer, file store, debounced writer, restore, correlation tokens, and copy diagnostics (`ready-for-agent`). made by: Olli Aalto. made with: Cursor. model: composer-2.5-fast

- **Architecture deepening tickets** (`docs/features/*/0*.md`): Split seven feature PRDs into 29 tracer-bullet implementation tickets via `/to-tickets` (same directory as each `prd.md`, `ready-for-agent`). made by: Olli Aalto. made with: Cursor. model: composer-2.5-fast

- **Planning artifacts path** (`docs/features/`, `.gitignore`, `.piignore`, `CONTEXT.md`, `AGENTS.md`, `docs/agents/issue-tracker.md`): Rename `docs/prds/` to `docs/features/` to align with `/to-spec` and `/to-tickets`. made by: Olli Aalto. made with: Cursor. model: composer-2.5-fast

- **Wayfinder skill** (`.pi/skills/wayfinder/SKILL.md`): Route spec/PRD production through `/to-spec` instead of ad-hoc drafting; document when to invoke it during map work and ticket resolution. made by: Olli Aalto. made with: Cursor. model: composer-2.5-fast

- **Wiki update** (`docs/wiki/`): Refresh ACP client, context, quality-gate, and agent-cli overview pages with transcript color provider, footer, plan visualization, slash-command autocomplete, Markdown rendering, and pre-commit workflow. made by: Olli Aalto. made with: Cursor. model: composer-2.5-fast

- **Worktree pending-launch handoff PRD** (`docs/features/worktree-pending-launch-handoff/prd.md`): Architecture deepening candidate documenting the cross-project enqueue → persist → consume → editor lifecycle and proposing a deep `WorktreePendingLaunchHandoff` module. made by: Olli Aalto. made with: Cursor. model: composer-2.5-fast

- **ACP session resume orchestration PRD** (`docs/features/acp-session-resume-orchestration/prd.md`): Deep analysis of scattered resume/load/picker/persist logic; proposes `AcpSessionResumeOrchestrator` with port adapters. made by: Olli Aalto. made with: Cursor. model: composer-2.5-fast

- **Unified launch resolution PRD** (`docs/features/unified-launch-resolution/prd.md`): Deep analysis of duplicated PTY/ACP/worktree launch path resolution; proposes kernel `AgentLaunchResolver` module. made by: Olli Aalto. made with: Cursor. model: composer-2.5-fast

- **Transcript pipeline consolidation PRD** (`docs/features/transcript-pipeline-consolidation/prd.md`): Deep analysis of dual rendering paths and shallow transcript modules; proposes dead-path deletion and ingestion deepening. made by: Olli Aalto. made with: Cursor. model: composer-2.5-fast

- **ACP client operations wiring PRD** (`docs/features/acp-client-operations-wiring/prd.md`): Deep analysis of shallow `AcpClientSessionOperationsImpl` and untested VFS access; proposes `SessionFilesystemOperations` seam. made by: Olli Aalto. made with: Cursor. model: composer-2.5-fast

- **ACP session controller deepening PRD** (`docs/features/acp-session-controller-deepening/prd.md`): Deep analysis of shallow eight-method controller interface; proposes four-method editor API and internal transport/lifecycle modules. made by: Olli Aalto. made with: Cursor. model: composer-2.5-fast

- **Implement skill "mark as done" fix** (`.pi/skills/implement/SKILL.md`): Replace ambiguous "mark the ticket(s) as done in the issue tracker" with actionable instruction to set `**Status:** done` and check off items. made by: Olli Aalto. made with: pi.

- **Unified launch resolution ticket statuses** (`docs/features/unified-launch-resolution/`): Mark tickets 01–05 `done`. made by: Olli Aalto. made with: pi.

## 2026-07-23

### Fixed

- **headroom-tool-install on Windows** (`.agentic-config/install-plan.json`): Force `ast-grep-cli` to build from source (`--no-binary-package ast-grep-cli`) so the pre-built `sg.exe` wheel is not used — it gets quarantined by Windows Defender as a false positive. Rust MSVC toolchain required. made by: Olli Aalto.

## 2026-06-26

### Documentation

- **implement skill** (`.pi/skills/implement/SKILL.md`): Run `qualityGate` then load `/review` from `.agents/skills/review/SKILL.md` before commit. made by: Olli Aalto. made with: Cursor. model: composer-2.5-fast

- **to-issues skill** (`.agents/skills/to-issues/SKILL.md`): Add Agent CLI project context (slices, ADRs, glossary, triage labels, quality gate) and save implementation slices to `docs/issues/<feature_name>/<slice-slug>.md`. made by: Olli Aalto. made with: Cursor. model: composer-2.5-fast

- **to-prd skill** (`.agents/skills/to-prd/SKILL.md`): Add Agent CLI project context (slices, ADRs, glossary, quality gate) and save PRDs to `docs/features/<feature_name>/prd.md`. made by: Olli Aalto. made with: Cursor. model: composer-2.5-fast

- **Agent commands cleanup** (`docs/agent-commands.md`): Remove obsolete To Complete checklist; gates confirmed from Gradle/CI. made by: Olli Aalto. made with: Cursor. model: composer-2.5-fast

- **Wiki log backfill** (`docs/wiki/log.md`): Ingest entries for five existing wiki pages; ADC post-update history repair. made by: Olli Aalto. made with: Cursor. model: composer-2.5-fast

- **CONTEXT glossary trim** (`CONTEXT.md`, `docs/wiki/concepts/context.md`): Replace 1.3k-line code dump with domain glossary and wiki/source pointers; fix stale `docs/prd/` path in agent-cli overview → `docs/features/`; add `qualityGate` one-liner to agent-commands. made by: Olli Aalto. made with: Cursor. model: composer-2.5-fast

### Changed

- **ADC 2026.06.26 update** (`AGENTS.md`, `.agents/rules/`, `.agentic-config/manifest.json`, `.graphifyignore`): Merge new bundle rules (graphify, ponytail, headroom, restricted-operations), restore tailored Gradle workflow gates, and record content hashes for post-install tracking. made by: Olli Aalto. made with: Cursor. model: composer-2.5-fast

- **Pre-commit graphify** (`scripts/pre-commit`, `.agents/rules/graphify-consultation.md`, `.agents/rules/workflow-gates.md`): Run `graphify update .` after ktlint in pre-commit so structural graph stays fresh. made by: Olli Aalto. made with: Cursor. model: composer-2.5-fast

### Fixed

- **Seed doc regression** (`CONTEXT.md`, `docs/agent-commands.md`, `docs/wiki/index.md`): Restore pre-bundle content overwritten by ADC update stubs. made by: Olli Aalto. made with: Cursor. model: composer-2.5-fast

### Removed

- **Obsolete diagnose stub** (`.agents/skills/diagnose/`): Dropped ADC placeholder; live skill is upstream `diagnosing-bugs` at `.pi/skills/diagnosing-bugs/`. made by: Olli Aalto. made with: Cursor. model: composer-2.5-fast

## 2026-06-24

### Documentation

- **Wiki path-map pages** (`docs/wiki/concepts/context.md`, `docs/wiki/subsystems/architecture.md`, `docs/wiki/index.md`, `docs/wiki/log.md`): Ingest required wiki pages from `CONTEXT.md` and ADRs so `scripts/wiki-lint.mjs` passes in CI. made by: Olli Aalto. made with: Cursor. model: composer-2.5-fast

### Changed

- **Static analysis** (`build.gradle.kts`, `detekt.yml`, `.github/workflows/qodana.yml`): Remove Qodana workflow; expand detekt with type-resolution `detektMain`/`detektTest` in `qualityGate`, stricter rule config, and source fixes for nullable/empty-string patterns. made by: Olli Aalto. made with: Cursor. model: composer-2.5-fast

### Fixed

- **CI test portability** (`src/test/kotlin/com/oaalto/agent/acp/AcpProcessLauncherTest.kt`, `src/main/kotlin/com/oaalto/agent/worktree/AgentWorktreePathMapper.kt`): Use temp directories instead of hardcoded Windows paths in launch-plan tests; skip host `Path` normalization for WSL UNC keys so Linux CI matches path-mapper expectations. made by: Olli Aalto. made with: Cursor. model: composer-2.5-fast

- **ACP slash commands** (`src/main/kotlin/com/oaalto/agent/acp/TranscriptSessionUpdateMapper.kt`, `src/main/kotlin/com/oaalto/agent/acp/ui/PromptInputBar.kt`, `src/main/kotlin/com/oaalto/agent/acp/AcpAgentEditor.kt`): Handle ACP `available_commands_update` notifications and wire slash-command autocomplete into the ACP prompt bar so users can discover and submit `/command` prompts per the ACP protocol. made by: Olli Aalto. made with: Cursor. model: composer-2.5-fast

- **ACP slash command popup sizing** (`src/main/kotlin/com/oaalto/agent/acp/ui/PromptInputBar.kt`): Cap the command picker at five visible rows, match popup width to the prompt field, and show it above the input so long command lists no longer dominate the layout. made by: Olli Aalto. made with: Cursor. model: composer-2.5-fast

- **ACP slash command popup focus** (`src/main/kotlin/com/oaalto/agent/acp/ui/PromptInputBar.kt`): Keep typing focus in the prompt field while the command picker is open; Up/Down adjust the highlighted command and filtering updates the list in place. made by: Olli Aalto. made with: Cursor. model: composer-2.5-fast

## 2026-06-22

### Added

- **ACP transcript plan visualization implementation (Step 9)** (`src/main/kotlin/com/oaalto/agent/acp/plan/PlanPanel.kt`, `src/main/kotlin/com/oaalto/agent/acp/plan/PlanPanelRenderer.kt`, `src/main/kotlin/com/oaalto/agent/acp/StructuredUpdate.kt`, `src/main/kotlin/com/oaalto/agent/acp/TranscriptBlock.kt`, `src/main/kotlin/com/oaalto/agent/acp/TranscriptModel.kt`, `src/main/kotlin/com/oaalto/agent/acp/TranscriptSessionUpdateMapper.kt`, `src/main/kotlin/com/oaalto/agent/acp/TranscriptBlockViewFactory.kt`): Render `PlanUpdate` / `PlanUpdateV2` / `PlanRemoved` as visual plan checklists in the transcript. Status icons (`[ ]` pending, `[→]` in-progress, `[✓]` completed) with colors (gray/orange/green). Priority indicators: HIGH bold with red left border, LOW muted gray. Progress summary "N of M completed" with green when fully complete. In-place updates by plan ID; `PlanRemoved` removes panel; `PlanVariant.File` and `Markdown` fallback rendering. made by: Olli Aalto. made with: Cursor. model: kimi-k2.5

- **ACP transcript plan visualization tests** (`src/test/kotlin/com/oaalto/agent/acp/plan/PlanPanelRendererTest.kt`, `src/test/kotlin/com/oaalto/agent/acp/plan/TranscriptModelPlanTest.kt`): Unit tests for plan rendering (status icons, priority styling, HTML escaping, progress summary, variant fallbacks) and `TranscriptModel` plan tracking (in-place updates, removal, completion counts, chronological ordering). made by: Olli Aalto. made with: Cursor. model: kimi-k2.5

- **Platform color migration test updates** (`src/test/kotlin/com/oaalto/agent/acp/TranscriptRendererTest.kt`, `src/test/kotlin/com/oaalto/agent/acp/plan/PlanPanelRendererTest.kt`): Updated tests to work with dynamic theme-aware colors from `TranscriptColorProvider`; assertions now check for hex color patterns instead of hardcoded values to accommodate light/dark theme variations. made by: Olli Aalto. made with: Cursor. model: kimi-k2.5

- **ACP transcript plan visualization PRD (Step 9)** (`docs/prd/acp-transcript-plan-visualization.md`, `docs/issues/acp-transcript-step9-plan-visualization.md`): PRD and `ready-for-agent` issue for Step 9 of output rendering — visual plan checklist for `PlanUpdate` / `PlanUpdateV2` with status icons (`[ ]` pending, `[→]` in-progress, `[✓]` completed), priority indicators, progress summary, and in-place update by plan ID. made by: Olli Aalto. made with: Cursor. model: kimi-k2.5
- **ACP transcript footer status bar implementation (Step 8)** (`src/main/kotlin/com/oaalto/agent/acp/TranscriptFooter.kt`, `src/main/kotlin/com/oaalto/agent/acp/AccumulatedUsage.kt`, `src/main/kotlin/com/oaalto/agent/acp/StructuredUpdate.kt`): Sticky footer status bar for ACP transcript panel displaying cumulative token usage (`used / size`) and optional cost from `UsageUpdate` events. Usage label turns orange when exceeding 80% of context window; cost label hidden when null. `JBColor` theming adapts to dark/light IDE themes. made by: Olli Aalto. made with: Cursor. model: kimi-k2.5
- **ACP transcript footer tests** (`src/test/kotlin/com/oaalto/agent/acp/TranscriptFooterTest.kt`, `src/test/kotlin/com/oaalto/agent/acp/TranscriptModelUsageAccumulationTest.kt`): Unit tests for footer component (usage/cost formatting, visibility, color thresholds) and `TranscriptModel` usage accumulation (cumulative tokens, cost summation, currency handling). made by: Olli Aalto. made with: Cursor. model: kimi-k2.5
- **ACP transcript footer status bar PRD (Step 8)** (`docs/prd/acp-transcript-footer-status-bar.md`, `docs/issues/acp-transcript-step8-footer-status-bar.md`): PRD and issue for footer status bar. made by: Olli Aalto. made with: Cursor. model: kimi-k2.5
- **Zero-suppression policy in agent rules** (`.agents/rules/warning-hygiene.md`, `.agents/rules/workflow-gates.md`): Enforce that no `@Suppress` annotations are permitted anywhere in the codebase; pre-commit hook blocks commits containing them. made by: Olli Aalto. made with: pi. model: claude-sonnet-4-20250514
- **ACP transcript Markdown rendering (Step 7)** (`src/main/kotlin/com/oaalto/agent/acp/TranscriptMarkdownRenderer.kt`, `src/main/kotlin/com/oaalto/agent/acp/TranscriptBlockViewFactory.kt`, `src/main/kotlin/com/oaalto/agent/acp/TranscriptToolCallTextBodyRenderer.kt`): Implement Markdown AST → `RenderedBlock` mapping using IntelliJ's `org.intellij.markdown` parser with GFM flavour descriptor. Agent text and tool card body content now renders via full Markdown parsing instead of the retired `segmentFencedCodeBlocks`/`TextSegment` approach. made by: Olli Aalto. made with: pi. model: claude-sonnet-4-20250514
- **TranscriptMarkdownRenderer tests** (`src/test/kotlin/com/oaalto/agent/acp/TranscriptMarkdownRendererTest.kt`): Coverage for inline formatting (bold, italic, code, link, strikethrough), headings, fenced code blocks, GFM tables, blockquotes, lists, thematic breaks, images, malformed input, and `likelyContainsMarkdown` heuristic. made by: Olli Aalto. made with: pi. model: claude-sonnet-4-20250514

### Changed

- **Ignore pi worker context** (`.gitignore`): Added `.pi/worker-context/` so local pi worker scratch files are not tracked in git. made by: Olli Aalto. made with: Cursor. model: composer-2.5-fast

- **Platform color migration** (`src/main/kotlin/com/oaalto/agent/acp/TranscriptColorProvider.kt`, `src/main/kotlin/com/oaalto/agent/acp/TranscriptPalette.kt`, `src/main/kotlin/com/oaalto/agent/acp/TranscriptRenderHelpers.kt`, `src/main/kotlin/com/oaalto/agent/acp/TranscriptBadgeStyle.kt`, `src/main/kotlin/com/oaalto/agent/acp/CollapsibleToolPanel.kt`, `src/main/kotlin/com/oaalto/agent/acp/TranscriptBlockViewFactory.kt`, `src/main/kotlin/com/oaalto/agent/acp/TranscriptBlockLabelBinder.kt`, `src/main/kotlin/com/oaalto/agent/acp/plan/PlanPanelRenderer.kt`, `src/main/resources/META-INF/plugin.xml`): Migrated all hardcoded RGB/hex color constants to theme-aware colors via new `TranscriptColorProvider` interface and `DefaultTranscriptColorProvider` implementation using IntelliJ `JBColor`. Colors now automatically adapt to IDE light/dark/high-contrast themes. `TranscriptPalette` converted to facade with computed properties delegating to provider; old RGB constants deprecated. Code blocks use `EditorColorsManager` for editor scheme integration. Service registered in `plugin.xml`. made by: Olli Aalto. made with: Cursor. model: kimi-k2.5

- **Plan visualization dismissed state** (`src/main/kotlin/com/oaalto/agent/acp/TranscriptBlock.kt`, `src/main/kotlin/com/oaalto/agent/acp/StructuredUpdate.kt`, `src/main/kotlin/com/oaalto/agent/acp/TranscriptModel.kt`, `src/main/kotlin/com/oaalto/agent/acp/plan/PlanTranscriptRegistry.kt`, `src/main/kotlin/com/oaalto/agent/acp/plan/PlanPanel.kt`, `src/main/kotlin/com/oaalto/agent/acp/plan/PlanPanelRenderer.kt`, `src/test/kotlin/com/oaalto/agent/acp/plan/TranscriptModelPlanTest.kt`): Changed plan removal behavior to use a "dismissed" state instead of fully removing plans from the transcript. Added `dismissed: Boolean` field to `PlanBlock`, `StartOrUpdatePlan`, and `RemovePlan`; dismissed plans show with greyed-out styling, "[dismissed]" indicator, and muted colors. Updated `PlanPanel` with `dispose()` method for API consistency. made by: Olli Aalto. made with: Cursor. model: kimi-k2.5

- **ACP transcript footer integration** (`src/main/kotlin/com/oaalto/agent/acp/AcpSessionListener.kt`, `src/main/kotlin/com/oaalto/agent/acp/AcpAgentEditor.kt`, `src/main/kotlin/com/oaalto/agent/acp/AcpEditorLayout.kt`, `src/main/kotlin/com/oaalto/agent/acp/TranscriptModel.kt`, `src/main/kotlin/com/oaalto/agent/acp/TranscriptSessionUpdateMapper.kt`): Added `onUsageUpdate` callback to `AcpSessionListener`; `AcpAgentEditor` wires footer to listener with EDT threading; `AcpEditorLayout` places footer at `BorderLayout.SOUTH`; `TranscriptModel` accumulates usage across updates with `AccumulatedUsage` state; `TranscriptSessionUpdateMapper` emits `StructuredUpdate.Usage` for `UsageUpdate` events. made by: Olli Aalto. made with: Cursor. model: kimi-k2.5
- **Agent text rendering** (`src/main/kotlin/com/oaalto/agent/acp/TranscriptBlockViewFactory.kt`): `AgentTextRow` now parses `FinalAgentText` through `TranscriptMarkdownRenderer`; code blocks use existing `TranscriptCodeBlockViewFactory`, headings/links via `JTextPane` `StyledDocument`, tables via `JEditorPane` HTML, blockquotes via bordered indentation, inline styles via `StyleConstants`. made by: Olli Aalto. made with: pi. model: claude-sonnet-4-20250514
- **Tool call body rendering** (`src/main/kotlin/com/oaalto/agent/acp/TranscriptToolCallTextBodyRenderer.kt`): Uses `TranscriptMarkdownRenderer.likelyContainsMarkdown()` heuristic and renders Markdown blocks to `TranscriptBodyPart.Html`/`Code`. made by: Olli Aalto. made with: pi. model: claude-sonnet-4-20250514
- **Fenced agent text limit test** (`src/test/kotlin/com/oaalto/agent/acp/TranscriptFencedAgentTextLimitsTest.kt`): Updated references from old `segmentFencedCodeBlocks` to new `RenderedBlock.CodeBlock`. made by: Olli Aalto. made with: pi. model: claude-sonnet-4-20250514

### Fixed

- **Agent dropdown displays icon instead of selected agent name** (`src/main/kotlin/com/oaalto/agent/SelectAgentConfigurationActionGroup.kt`): Restored `displayTextInToolbar()` method that was incorrectly removed in style cleanup commit `4ae3109`; the method is required to show the currently selected agent name in the toolbar instead of an icon-only button. made by: Olli Aalto. made with: Cursor. model: kimi-k2.5

- **Detekt and ktlint compliance for plan visualization** (`src/main/kotlin/com/oaalto/agent/acp/AcpEditorLayout.kt`, `src/main/kotlin/com/oaalto/agent/acp/TranscriptBlockViewFactory.kt`, `src/main/kotlin/com/oaalto/agent/acp/TranscriptFooter.kt`): Fixed `LongParameterList` in `buildRootPanel` by introducing `EditorLayoutComponents` data class; reduced `CyclomaticComplexMethod` in `TranscriptBlockViewFactory.update` by extracting helper methods (`isToolCallMatch`, `isPlanMatch`, `isTextRowMatch`, `isTypeMismatch`, `logTypeMismatch`, `logTextRowMismatch`); replaced magic numbers in `TranscriptFooter` with named constants (`HORIZONTAL_GAP`, `VERTICAL_GAP`, `HIGH_USAGE_THRESHOLD`). made by: Olli Aalto. made with: Cursor. model: kimi-k2.5

- **Tool-body highlight cap double-counting** (`src/main/kotlin/com/oaalto/agent/acp/TranscriptToolCallTextBodyRenderer.kt`): Removed duplicate counter increment in `blockQuoteToBodyParts` and `renderTextBodyParts` flatMap, ensuring `MAX_HIGHLIGHTED_CODE_BLOCKS` limits are enforced correctly. made by: Olli Aalto. made with: Cursor. model: kimi-k2.5
- **Tool-card Markdown inline formatting** (`src/main/kotlin/com/oaalto/agent/acp/TranscriptToolCallTextBodyRenderer.kt`): Added `renderStyledInlineHtml` with HTML span rendering for bold, italic, code, link, strikethrough styles in tool card bodies. made by: Olli Aalto. made with: Cursor. model: kimi-k2.5
- **List-item style offset** (`src/main/kotlin/com/oaalto/agent/acp/TranscriptBlockViewFactory.kt`): Offset styled run indices by list marker length (`${block.listMarker}.length`) so inline formatting aligns correctly in bullet/numbered list items. made by: Olli Aalto. made with: Cursor. model: kimi-k2.5
- **Styled runs truncation bounds** (`src/main/kotlin/com/oaalto/agent/acp/TranscriptBlockViewFactory.kt`): Filter runs to `start < text.length && end <= text.length` after truncation to prevent out-of-range character attributes. made by: Olli Aalto. made with: Cursor. model: kimi-k2.5
- **Markdown heuristic coverage** (`src/main/kotlin/com/oaalto/agent/acp/TranscriptMarkdownRenderer.kt`): Extended `likelyContainsMarkdown` with `-`, `>`, `|`, list patterns (`^-\\s+`, `^\\d+\\.\\s+`), blockquote pattern (`^>\\s+`), and table pattern for detecting more Markdown constructs in tool bodies. made by: Olli Aalto. made with: Cursor. model: kimi-k2.5
- **Parser fallback style** (`src/main/kotlin/com/oaalto/agent/acp/TranscriptMarkdownRenderer.kt`): Changed `combineStyles` default from `TextStyle.BOLD` to `TextStyle.ITALIC` for unrecognized style combinations, reducing visual intrusion on parser edge cases. made by: Olli Aalto. made with: Cursor. model: kimi-k2.5
- **Test assertions for Markdown headings** (`src/test/kotlin/com/oaalto/agent/acp/TranscriptSessionUpdateMapperTest.kt`): Updated assertions to expect heading text without `#` prefix since `TranscriptMarkdownRenderer` parses Markdown headings and strips the marker. made by: Olli Aalto. made with: Cursor. model: kimi-k2.5
- **Redundant IMAGE handling** (`src/main/kotlin/com/oaalto/agent/acp/TranscriptMarkdownRenderer.kt`): Removed unreachable `MarkdownElementTypes.IMAGE` check from `handleElse` (already handled in `dispatch` when block). made by: Olli Aalto. made with: Cursor. model: kimi-k2.5

### Removed

- **TextSegment.kt** (`src/main/kotlin/com/oaalto/agent/acp/TextSegment.kt`): Replaced by `TranscriptMarkdownRenderer` Markdown AST parsing. made by: Olli Aalto. made with: pi. model: claude-sonnet-4-20250514
- **TextSegmentTest.kt** (`src/test/kotlin/com/oaalto/agent/acp/TextSegmentTest.kt`): Removed with retired `TextSegment` API. made by: Olli Aalto. made with: pi. model: claude-sonnet-4-20250514

### Documentation

- **ACP output rendering roadmap** (`docs/acp-output-rendering-roadmap.md`): Link Step 7 PRD for Markdown rendering. made by: Olli Aalto. made with: Cursor. model: Composer

## 2026-06-21

### Added

- **ACP transcript syntax highlighting (Step 6)** (`acp/TextSegment.kt`, `acp/TranscriptBodyPart.kt`, `acp/TranscriptFenceLanguageResolver.kt`, `acp/TranscriptCodeBlockViewFactory.kt`, `acp/TranscriptToolCallTextBodyRenderer.kt`, `acp/TranscriptBlockViewFactory.kt`, `acp/CollapsibleToolPanel.kt`, `acp/TranscriptToolCallContentRenderer.kt`, `acp/TranscriptPanel.kt`, `acp/TranscriptViewController.kt`, tests): Fenced-code segmentation and EditorFactory-backed syntax highlighting in finalized agent text and completed/failed tool card bodies; mixed HTML and embedded code components in collapsible tool panels. made by: Olli Aalto. made with: Cursor. model: Composer

- **ACP transcript syntax highlighting PRD** (`docs/prd/acp-transcript-syntax-highlighting.md`, `docs/issues/acp-transcript-step6-syntax-highlighting.md`): PRD and `ready-for-agent` issue for roadmap Step 6 — fenced-code segmentation and language-aware highlighting in agent text and tool card bodies (Koog preferred, EditorFactory fallback). made by: Olli Aalto. made with: Cursor. model: Composer

### Changed

- **ACP transcript syntax highlighting hardening** (`acp/TranscriptTextTruncation.kt`, `acp/TranscriptRenderHelpers.kt`, `acp/CollapsibleToolPanel.kt`, `acp/TranscriptBlockViewFactory.kt`, `acp/TranscriptPanel.kt`, `acp/TranscriptViewController.kt`, `acp/AcpAgentEditor.kt`, tests): Lazy-build tool card bodies only when expanded; truncate and cap highlighted agent code blocks; dispose EditorFactory editors on editor close and row removal; remove unused `TranscriptUpdateRenderer` HTML update path. made by: Olli Aalto. made with: Cursor. model: Composer

### Fixed

- **ACP transcript syntax highlighting review fixes** (`acp/TranscriptBlockViewFactory.kt`, `acp/CollapsibleToolPanel.kt`, `acp/TranscriptViewController.kt`, tests): Update streaming agent rows in place instead of rebuilding every chunk; skip tool-card body rebuild when `bodyParts` are unchanged; align overflow code-block styling with tool `<pre>` bodies; dispose transcript editors synchronously on editor close. made by: Olli Aalto. made with: Cursor. model: Composer

### Documentation

- **ACP output rendering roadmap** (`docs/acp-output-rendering-roadmap.md`): Link Step 6 PRD for syntax-highlighted code blocks. made by: Olli Aalto. made with: Cursor. model: Composer

## 2026-06-20

### Added

- **ACP transcript collapsible tool cards PRD** (`docs/prd/acp-transcript-collapsible-tool-cards.md`, `docs/issues/acp-transcript-step5-collapsible-tool-cards.md`): PRD and `ready-for-agent` issue for roadmap Step 5 — structured Swing transcript with `TranscriptModel`, in-place tool cards, and collapsed Step 4 result bodies. made by: Olli Aalto. made with: Cursor. model: Composer

- **ACP transcript tool output content PRD** (`docs/prd/acp-transcript-tool-output-content.md`, `docs/issues/acp-transcript-step4-tool-output-content.md`): PRD and `ready-for-agent` issue for roadmap Step 4 — render `ToolCallContent` text, diffs, and terminal references below badge headers on completed/failed tools. made by: Olli Aalto. made with: Cursor. model: Composer

### Changed

- **ACP transcript collapsible tool cards (Step 5)** (`acp/TranscriptModel.kt`, `acp/TranscriptPanel.kt`, `acp/CollapsibleToolPanel.kt`, `acp/TranscriptViewController.kt`, `acp/TranscriptSessionUpdateMapper.kt`, `acp/AcpAgentEditor.kt`, tests): Replace monolithic HTML transcript with structured Swing panel — one collapsible card per `toolCallId`, in-place badge updates, collapsed Step 4 result bodies, and preserved agent streaming cursor. made by: Olli Aalto. made with: Cursor. model: Composer

- **ACP transcript tool output content (Step 4)** (`acp/TranscriptToolCallContentRenderer.kt`, `acp/TranscriptToolCallDiffRenderer.kt`, `acp/TranscriptUpdateRenderer.kt`, `acp/TranscriptRenderer.kt`, tests): Render `ToolCallContent` bodies (text, diff, terminal, resources) below completed/failed tool badges; extract line diff to `TranscriptToolCallDiffRenderer` for detekt/ktlint. made by: Olli Aalto. made with: Cursor. model: Composer

### Fixed

- **ACP transcript Step 5 review fixes** (`acp/StructuredUpdate.kt`, `acp/TranscriptBlockViewFactory.kt`, `acp/CollapsibleToolPanel.kt`, `acp/auth/AuthFlowCoordinator.kt`, tests): Restore distinct auth-failure copy, wrap long text rows, resize expanded tool bodies, and update streaming rows in place. made by: Olli Aalto. made with: Cursor. model: Composer

- **ACP transcript line diff accuracy** (`acp/TranscriptToolCallDiffRenderer.kt`, tests): Replace greedy line walk with LCS-backed matching so middle insertions and replacements no longer mis-report unchanged lines as remove-then-add pairs. made by: Olli Aalto. made with: Cursor. model: Composer

- **ACP transcript empty oldText diff** (`acp/TranscriptToolCallDiffRenderer.kt`, tests): Normalize empty `oldText` the same as empty `newText` so new-file diffs do not emit a spurious red removal line. made by: Olli Aalto. made with: Cursor. model: Composer

### Documentation

- **ACP output rendering roadmap** (`docs/acp-output-rendering-roadmap.md`): Link Step 4 PRD for tool output content rendering; link Step 5 PRD for collapsible tool call cards and structured transcript model. made by: Olli Aalto. made with: Cursor. model: Composer

## 2026-06-19

### Added

- **ACP transcript streaming cursor PRD** (`docs/acp-output-rendering-roadmap.md`): Roadmap Step 3 PRD and `ready-for-agent` issue for inline streaming cursor on agent text — finalize before tool/thought/status interrupts and on prompt completion. made by: Olli Aalto. made with: Cursor. model: Composer

- **ACP transcript tool status badges PRD** (`docs/prd/acp-transcript-tool-status-badges.md`, `docs/issues/acp-transcript-step2-tool-status-badges.md`): PRD and issue for Step 2 of output rendering — badge-first tool call lines with status-colored badges and ✓/✗ iconography, replacing Step 1's bracketed `(status)` format. made by: Olli Aalto. made with: Cursor. model: Composer

- **ACP transcript HTML rendering PRD** (`docs/prd/acp-transcript-html-rendering.md`, `docs/issues/acp-transcript-step1-html-rendering.md`): PRD and issue for Step 1 of output rendering — replace `JBTextArea` with HTML `JEditorPane` for color-coded source differentiation. made by: Olli Aalto. made with: Claude. model: claude-sonnet-4-20250514

### Changed

- **ACP transcript streaming cursor** (`acp/TranscriptStreamingCursor.kt`, `acp/TranscriptHtmlAppender.kt`, `acp/AcpPromptEventDispatcher.kt`, `acp/AcpSessionControllerImpl.kt`, `acp/AcpAgentEditor.kt`, `acp/AcpSessionListener.kt`, tests): Inline `▊` cursor on streaming agent text; finalize before tool/thought/status interrupts and on prompt completion; HTML-escaped chunks with second-burst stream cycles. made by: Olli Aalto. made with: Cursor. model: Composer

### Fixed

- **ACP transcript streaming review fixes** (`acp/TranscriptHtmlAppender.kt`, `acp/AcpAgentEditor.kt`, `acp/AcpClientSessionOperationsImpl.kt`, tests): Incremental `JEditorPane` splices for streaming chunks instead of full-document rewrites; marshal transcript mutations on the EDT; route `notify()` through `AcpPromptEventDispatcher`; drop redundant listener finalization; scroll only when the viewport is already at the bottom; add Swing round-trip and scroll-behavior tests. made by: Olli Aalto. made with: Cursor. model: Composer

- **ACP streaming cursor JEditorPane rendering** (`acp/TranscriptStreamingCursor.kt`): Replace literal unicode `\u258a` with HTML entity `&#9612;` so JEditorPane does not strip the cursor character during HTML round-trips; add `stripCursor` helper; update `hasCursor` to detect all entity forms. made by: Olli Aalto. made with: Cursor. model: Composer

- **ACP transcript tool status badge review fixes** (`acp/TranscriptRenderHelpers.kt`, `acp/TranscriptRenderer.kt`, `acp/TranscriptRendererTest.kt`, `docs/`): Structural legacy-format assertions on all tool-call tests; UTF-8 charset in `htmlDocumentStart` with icon round-trip test; `formatToolStatus` aligned to badge-first plain text; `ToolCallUpdate` null-title integration test; PRD marked implemented and roadmap current state refreshed. made by: Olli Aalto. made with: Cursor. model: Composer

- **ACP transcript tool status badges** (`acp/TranscriptRenderHelpers.kt`, `acp/TranscriptRendererTest.kt`): Replace Step 1 bracketed `[kind] title (status)` tool lines with badge-first HTML — colored status badge with ✓/✗ icons for completed/failed, muted title after the badge, no parenthetical status text. made by: Olli Aalto. made with: Cursor. model: Composer

- **ACP transcript HTML rendering** (`acp/TranscriptUpdateRenderer.kt`, `acp/TranscriptRenderer.kt`, `acp/AcpAgentEditor.kt`): Replace `JBTextArea` with `JEditorPane` (text/html); `TranscriptUpdateRenderer` produces color-coded HTML `<span>` fragments; `TranscriptRenderer` adds HTML helpers (`formatToolStatusHtml`, `formatErrorHtml`, `htmlDocumentStart`); `escapeHtml` prevents injection; extract `launchAndConnect` to keep `LongMethod` under threshold; add `thresholdInObjects`/`thresholdInClasses` to detekt config for new function count. made by: Olli Aalto. made with: pi (worker). model: 

- **ACP transcript HTML rendering tests** (`acp/TranscriptRendererTest.kt`): Add HTML fragment output tests for all `SessionUpdate` subtypes, HTML escaping, and HTML helper methods; update chronological order test for HTML output. made by: Olli Aalto. made with: pi (worker). model:

- **ACP roadmap status** (`docs/acp-output-rendering-roadmap.md`): Add status header linking to Step 1 PRD. made by: Olli Aalto. made with: Claude. model: claude-sonnet-4-20250514

- **ACP transcript HTML helper extraction** (`acp/TranscriptHtmlAppender.kt`, `acp/TranscriptRenderHelpers.kt`, `acp/TranscriptRenderer.kt`, `acp/AcpAgentEditor.kt`): Extract HTML append logic from `AcpAgentEditor` into `TranscriptHtmlAppender` and HTML formatting helpers from `TranscriptRenderer` into `TranscriptRenderHelpers`; restore original detekt `thresholdInClasses` (15) and use `thresholdInObjects: 12` instead of the previous `16`/`17` bumps. made by: Olli Aalto. made with: pi (worker). model:

- **ACP transcript line/stream separation and user echo** (`acp/AcpAgentEditor.kt`): Show "Connecting..." message before `sessionController.connect()`; make `appendTranscriptLine` append a trailing newline so subsequent streaming chunks don't merge onto the same line; insert a blank line before echoing the user prompt as `&gt; ` so the input is visually separated in the output. made by: Olli Aalto.

## 2026-06-17

### Fixed

- **ACP auth and transcript rendering** (`acp/auth/`, `acp/ui/AuthPromptPanel.kt`, `acp/TranscriptRenderer.kt`, `acp/AcpAgentEditor.kt`): Try silent `authenticate` before showing auth UI so already-logged-in Cursor sessions skip prompts; route credential-less Agent Auth to Shell-pane confirmation instead of an API-key field; render auth messages in a multiline read-only area; normalize transcript line endings and `<br>` tags. made by: Olli Aalto. made with: Cursor. model: Composer

- **ACP environment variable settings** (`settings/AgentSettingsConfigurable.kt`): Replace the `KEY=VALUE` textarea with a Name/Value table so each variable has a dedicated edit cell. made by: Olli Aalto. made with: Cursor. model: Composer

### Changed

- **Detekt step 5 thresholds** (`detekt.yml`, `acp/TranscriptRenderer.kt`, `settings/`): Tighten `ReturnCount` (max 3, guard-clause exclusion), `CyclomaticComplexMethod` (10), `CognitiveComplexMethod` (12), and `TooManyFunctions` (15); extract transcript/settings helpers to satisfy new limits. made by: Olli Aalto. made with: Cursor. model: Composer

- **Detekt strict compliance** (`detekt.yml`, `src/main/kotlin/`): Complete detekt burn-down for defaults — extract worktree/settings/MCP helpers, shared WSL path resolvers, `AgentWslCommandRequest`, UI metric constants, and `ignoreOverridden` for IntelliJ interface methods; `./gradlew qualityGate` passes clean. made by: Olli Aalto. made with: Cursor. model: Composer

- **ReturnCount compliance** (`src/main/kotlin/`): Refactor multi-return functions to `when` expressions, `Result.flatMap` chains, and shared `WslPathResolver` / `WorkingDirectoryResolver` helpers so detekt `ReturnCount` passes without changing launch behavior. made by: Olli Aalto. made with: Cursor. model: Composer

- **Detekt defaults** (`detekt.yml`): Drop relaxed overrides (long methods, wide parameter lists, disabled exception/style rules) and rely on detekt defaults; keep IntelliJ-friendly `MagicNumber` ignores and disable `MaxLineLength` (ktlint owns line length). made by: Olli Aalto. made with: Cursor. model: Composer

- **Per-project agent selection** (`settings/`, `SelectAgentConfigurationActionGroup.kt`, `RunAgentSplitButtonAction.kt`, `OpenAgentEditorAction.kt`): Split global default from per-project selected agent; toolbar and Run actions use `AgentConfigurationSelector` with workspace-scoped project state. made by: Olli Aalto. made with: Cursor. model: Composer

- **Settings UI structure** (`settings/`): Extract table models and UI factory helpers from `AgentSettingsConfigurable` to satisfy detekt size limits without behavior changes. made by: Olli Aalto. made with: Cursor. model: Composer

### Documentation

- **Per-project agent selection** (`CONTEXT.md`, `docs/adr/0003-per-project-agent-selection.md`): Glossary terms for **Selected agent** vs **Default agent configuration**; ADR records split persistence, migration, and `AgentConfigurationSelector` facade before implementation. made by: Olli Aalto. made with: Cursor. model: Composer

## 2026-06-16

### Added

- **Code quality tooling** (`build.gradle.kts`, `.editorconfig`, `detekt.yml`, `qodana.yml`, `.github/`): detekt static analysis, JaCoCo coverage reports, Kotlin `allWarningsAsErrors`, JDK 21 toolchain with Foojay auto-provisioning, Qodana workflow, Dependabot for Gradle/Actions/npm, and `package-lock.json`. CI now runs `qualityGate` and `verifyPlugin` before building artifacts; pre-commit runs `ktlintCheck` after wiki-lint. made by: Olli Aalto. made with: Cursor. model: Composer

- **ACP client session and UI** (`acp/`, `build.gradle.kts`): Kotlin ACP SDK 0.24.0; `AcpAgentEditor` with Transcript, Prompt, and idle Shell panes; `AcpSessionController` connect/newSession/prompt/dispose loop over stdio; WSL and node-wrapper launch via shared Command Builder; tool-call status lines in transcript. Implements PRD 3.0-02 issues 02-01 through 02-05. made by: Olli Aalto. made with: Cursor. model: Composer

- **3.0 launch slices and Launch Mode** (`settings/`, `pty/`, `acp/`, `AgentEditorFactory`): Per-configuration Launch Mode (Terminal / ACP) in settings with legacy migration to PTY Passthrough; PTY editor extracted to `pty` slice; ACP stub editor and factory routing by launch mode. Implements PRD 3.0-01 issues 01-01 through 01-03. made by: Olli Aalto. made with: Cursor. model: Composer

- **Worktree ACP session resume** (`worktree/`, `acp/`): Optional `acpSessionId` on managed worktree records; `ResumeStrategy` / `LaunchResumePlan` split PTY CLI resume from ACP `session/load` and `listSessions`; worktree launch coordinator; session picker fallback; bound-session UI indicator and pending-launch routing. Implements PRD 3.0-04 issues 04-01 through 04-06. made by: Olli Aalto. made with: Cursor. model: Composer

- **MCP exposure and acp.json portability** (`settings/`, `acp/mcp/`, `acp/AcpProcessLauncher.kt`, `acp/AcpSessionControllerImpl.kt`): Per-configuration `useIdeaMcp` / `useCustomMcp` toggles (default off) and ACP launch env vars persisted in `agentSettings.xml`; `McpCapabilityBridge` resolves IntelliJ and user MCP servers for ACP Client sessions; optional `com.intellij.mcpServer` dependency with runtime probe; manual import/export of `agent_servers` via Agent Settings toolbar. Implements PRD 3.0-05 issues 05-01 through 05-07. made by: Olli Aalto. made with: Cursor. model: Composer

### Fixed

- **MCP settings follow-up** (`settings/AiAssistantPresence.kt`, `settings/AgentSettingsConfigurable.kt`, `acp/AcpLaunchPlan.kt`, `pty/`, `worktree/resume/`, `CONTEXT.md`, `docs/adr/0001-custom-acp-client-in-plugin.md`): Enable IntelliJ MCP toggle when either AI Assistant or MCP Server plugin is present; hide MCP detail hints for PTY configs; pass explicit empty env maps at remaining `buildWslCommand` call sites; add `sessionMcpServers()` test coverage. made by: Olli Aalto. made with: Cursor. model: Composer

- **MCP and ACP launch review fixes** (`settings/`, `acp/`, `acp/mcp/`, `AgentCommandBuilder.kt`): Probe both JetBrains AI Assistant and MCP Server plugins; persist env vars on configuration row switch; inject WSL env via `env` prefix; gate session MCP servers with `exposeMcp`; skip invalid user MCP entries; avoid auto-starting IntelliJ MCP on launch; disable ACP-only detail controls for PTY; export WSL/node-wrapper metadata; warn before exporting plaintext env secrets. made by: Olli Aalto. made with: Cursor. model: Composer

- **ACP launch arguments** (`acp/AcpLaunchArguments.kt`): ACP launch mode auto-injects agent entry args (for example `acp` for `cursor-agent` / `agent`) and strips PTY resume flags from configured and worktree arguments. made by: Olli Aalto. made with: Cursor. model: Composer

- **ACP editor layout and session race** (`acp/`): Stack Transcript, Prompt, and Shell panes vertically; keep Prompt disabled until `session/new` completes so early submits no longer hit "ACP session is not open". made by: Olli Aalto. made with: Cursor. model: Composer

- **detekt findings** (`acp/AcpAgentEditor.kt`, `acp/ui/SessionPickerDialog.kt`): Remove unused session-picker parameter and use `const val` for start-fresh label. made by: Olli Aalto. made with: Cursor. model: Composer

### Changed

- **Local ACP scratch dirs** (`.gitignore`): Ignore `.tmp-acp-search/` and `.tmp-acp-sources/` so extracted ACP SDK sources stay out of version control. made by: Olli Aalto. made with: Cursor. model: Composer

- **CI quality gates** (`.github/workflows/build-plugin.yml`, `scripts/pre-commit`, `docs/development.md`): Build workflow runs wiki lint via `node scripts/wiki-lint.mjs`, `./gradlew qualityGate`, and `./gradlew verifyPlugin` before packaging; pre-commit adds ktlint. made by: Olli Aalto. made with: Cursor. model: Composer

- Established a `3.0` development baseline by setting the default plugin version to `3.0.0-SNAPSHOT` and aligning CI/docs release metadata with 3.0 tags, so the `3.0` branch is ready for the next development cycle.

- made by: Olli Aalto
- made with: Cursor
- model: Composer

### Documentation

- **Workflow gates rule** (`.agents/rules/workflow-gates.md`): Document `qualityGate`, detekt, `verifyPlugin`, CI/pre-commit/Qodana paths, and JaCoCo report-only status so agent workflow matches current tooling. made by: Olli Aalto. made with: Cursor. model: Composer

- **ACP client capabilities issues** (`docs/issues/03-01` through `03-08`): Eight vertical-slice implementation tickets for PRD 3.0-03 (scoped filesystem, permission memory, Shell PTY, auth flows, capability negotiation). made by: Olli Aalto. made with: Cursor. model: Composer

- **ACP client capabilities** (`acp/filesystem/`, `acp/permission/`, `acp/terminal/`, `acp/auth/`, `acp/`): Scoped filesystem read/write with IDE VFS and gitignore checks; inline Transcript permission prompts with allow/reject-always memory; Shell pane PTY wired to `terminal/create` (single-terminal replace policy); auth coordinator for no-auth, Terminal Auth, Agent Auth, and OAuth links; honest capability negotiation at `initialize`; WSL session cwd mapped to host scope for filesystem ops. Implements PRD 3.0-03 issues 03-01 through 03-08. made by: Olli Aalto. made with: Cursor. model: Composer

- Added agent setup and wiki bootstrap (`AGENTS.md`, `.agents/`, `CONTEXT.md`, `docs/agent-commands.md`, `docs/wiki/*`) to integrate the Pi agent bundle and initial wiki pages. made by: Olli Aalto. made with: Cursor. model: gpt-5-mini

- **3.0 ACP architecture ADRs** (`docs/adr/`): ADR 0001 (custom in-plugin ACP client, hybrid launch modes, vertical slices) and ADR 0002 (Kotlin ACP SDK). Updated `CONTEXT.md` with 3.0 glossary terms from design session. made by: Olli Aalto. made with: Cursor. model: Composer

- **Issue tracker conventions** (`docs/agents/issue-tracker.md`): Document `docs/issues/` as the path for implementation slices from `/to-issues`; PRDs stay under `docs/prd/`. made by: Olli Aalto. made with: Cursor. model: Composer

## 2026-05-25

### Changed
- Replaced the free-text `Node Wrapper` setting with a checkbox that runs npm/node-installed agents through `bash -ilc`, so nvm users no longer need to know the wrapper command.
- Migrated legacy text wrapper values to the checkbox on load.
- Upgraded the Gradle wrapper from 9.0.0 to 9.5.1.

### Added
- Added a per-agent `Node Wrapper` checkbox for npm/node-installed agent CLIs, allowing WSL launches such as Pi to run through `bash -ilc` so shell-managed paths are available.
- Added focused command-builder coverage for direct and wrapped local/WSL launches, including POSIX shell quoting for wrapper payloads.

### Changed
- Refactored agent launch command construction into a testable helper so wrapper behavior can be validated without starting an IDE terminal.
- Updated README setup guidance with a Pi/npm WSL example and wrapper command shape.

- made by: Olli Aalto
- made with: Cursor
- model: GPT-5.5

## 2026-04-26

### Added
- Added Gradle-based `ktlint` integration plus a `qualityGate` task to enforce ordered format/compile/lint/test checks, so contributor validation is consistent and reproducible.
- Added focused unit tests for WSL/host path mapping and normalization logic to prevent regressions in worktree path handling across Windows and WSL inputs.
- Added a regression test asserting the split-button default action remains `Run in Current Project`, guarding intended run behavior.
- Added OpenCode CLI resume support in managed worktree reopen flows (`opencode --continue`) so OpenCode sessions can be resumed from `Run Agent` like other supported agent CLIs.

### Changed
- Refactored worktree path conversion logic into a pure helper (`AgentWorktreePathMapper`) to isolate deterministic transformations from IDE-bound service code and make them directly testable.
- Updated contributor documentation to match current Run Agent behavior and to document `ktlint`/`qualityGate` commands so docs stay aligned with actual workflow gates.
- Updated resume mapping coverage and contributor checklist to include OpenCode resume behavior, reducing ambiguity when validating multi-CLI worktree session support.

### Fixed
- Fixed unresolved Gradle Kotlin stdlib conflict warning by disabling the default stdlib dependency in `gradle.properties`, reducing runtime/version mismatch risk in IntelliJ platform builds.
- Fixed ignored status returns in selection/delete flows by handling failed state updates with explicit logging and fallback behavior, improving diagnosability and correctness.

- made by: Olli Aalto
- made with: Cursor
- model: Codex 5.3

## 2026-04-03

### Changed
- Updated the worktree action icon in the toolbar for clearer run/worktree affordance and faster visual recognition during agent launches.
- made by: Olli Aalto

## 2026-04-02

### Added
- Added a split `Run Agent` workflow with a current-project main action and dropdown worktree actions to support isolated session flows.
- Added WSL execution targeting so Windows IDE sessions can launch Linux-installed agent binaries with Linux path semantics.
- Added WSL-related screenshot coverage and refreshed README imagery so setup guidance matches current UI behavior.
- made by: Olli Aalto

### Changed
- Refactored Cursor resume fallback to process-based probing so resume behavior can degrade gracefully when no prior chats exist.
- Refreshed README feature highlights with versioned capability notes to make user-facing changes easier to scan.
- Established a `2.0` development baseline to anchor subsequent worktree and multi-CLI session work.
- made by: Olli Aalto

### Fixed
- Fixed stale managed worktree entry handling and refreshed split-button state after delete operations to prevent outdated menu items.
- Fixed WSL project working-directory resolution for UNC paths to avoid incorrect launch directories on Windows + WSL setups.
- made by: Olli Aalto

## 2026-02-23

### Added
- Added repository-level ignore rules for `.cursor` artifacts to reduce accidental noise in source control.
- Added automated tag-based release-note generation for GitHub Releases and IDEA plugin update notes to reduce manual release overhead.
- made by: Olli Aalto

### Fixed
- Fixed configuration-cache serialization in plugin metadata configuration to keep Gradle config-cache runs stable.
- Fixed settings navigation and editor shortcut handling in the Agent tab to improve command flow reliability.
- made by: Olli Aalto

## 2026-02-20

### Fixed
- Fixed terminal focus behavior so input is correctly directed when the Agent tab is selected.
- Fixed multi-session opening behavior so concurrent/new Agent tab sessions open as intended.
- made by: Olli Aalto

## 2026-02-19

### Added
- Initial project commit establishing the Agent CLI plugin codebase and baseline structure.
- made by: Olli Aalto

### Fixed
- Fixed CI execution by granting execute permission to `gradlew` so automated builds can run consistently.
- made by: Olli Aalto
