# Changelog

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
