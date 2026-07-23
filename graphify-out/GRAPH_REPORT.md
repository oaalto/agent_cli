# Graph Report - .  (2026-07-23)

## Corpus Check
- cluster-only mode — file stats not available

## Summary
- 2611 nodes · 3911 edges · 243 communities (117 shown, 126 thin omitted)
- Extraction: 95% EXTRACTED · 5% INFERRED · 0% AMBIGUOUS · INFERRED: 205 edges (avg confidence: 0.8)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `ef863ee8`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- PlanEntry
- ASTNode
- featureIds
- StructuredUpdate
- TranscriptPanel
- TranscriptBodyPart
- TranscriptColorProvider
- McpServer
- agent-cli-overview.md
- AgentWorktreeStateService
- PermissionCoordinator
- AcpAgentEditor
- AuthPromptPanel
- AcpClientSessionOperationsImpl
- TranscriptModel
- Your task (in order)
- Your task (in order)
- TranscriptHtmlAppender
- CursorResumeProbeLogic
- TranscriptMarkdownRendererTest
- TranscriptRendererTest
- TranscriptToolCallContentRendererTest
- contentTemplateHashes
- PromptInputBar
- wiki
- skills
- PtyAgentEditor
- rules
- TranscriptFooter
- autoIncluded
- Full File Contents
- AgentTextRow
- AgentSettingsState
- AgentSettingsConfigurable
- AcpSessionControllerImpl
- TranscriptBlock
- Changelog
- Implementation Steps (Easiest → Hardest)
- CollapsibleToolPanel
- Your task (in order)
- AgentConfigRow
- AgentWorktreeGitSupport
- AgentLaunchContext
- ScopedFileSystemOperations
- Agentic Development Configurator setup
- How pi-agent Handles Displaying User Input and Model Output
- AcpLaunchPlan
- IdeScopedFileSystemAccess
- AgentConfigurationResolutionInput
- install.sh
- AuthFlowCoordinator
- AgentConfigsTableModel
- AcpSessionController
- RecordingSessionController
- AnActionEvent
- RecordingCodeBlockViewFactory
- install.ps1
- triageLabels
- AgentWorktreeService
- Agent CLI
- AgentSettingsUiFactory
- EnvironmentVariablesTableModel
- skillSource
- AGENTS.md
- TranscriptToolCallDiffRenderer
- .createEditor
- .buildWslTerminalStartupRequest
- Engineering Wiki Schema
- ADR 0003: Per-project agent selection
- manifest.json
- definition-of-done
- restricted-operations
- AgentFileEditorProvider
- AgentVirtualFile
- ShellPaneHost
- LaunchResumePlan
- PtyResumeStrategyTest
- changelog
- CollapsibleToolPanel.kt
- SelectAgentConfigurationActionGroup
- DetailPanelBindings
- .buildWorktreeChildren
- Wiki
- Agent Commands
- Development Guide
- During the session
- RunAgentSplitButtonAction
- AcpJsonImportDraft
- WorktreeLaunchCoordinator
- WslPathResolver
- TranscriptFenceLanguageResolverTest
- TranscriptModelTest
- AcpJsonImporterExporterTest
- Test-Driven Development
- ADR 0001: Custom ACP client in the Agent CLI plugin
- ADR 0002: Kotlin ACP SDK for the plugin client
- Vertical slice boundaries
- wiki-lint.mjs
- TerminalSessionRegistry
- bindTranscriptBlock
- TranscriptPaneHtmlOps
- ProjectAgentSelectionState
- AgentWorktreePathMapper
- TranscriptStreamingCursorTest
- selection
- package.json
- Review
- AgentSettingsStateSupport
- AiAssistantPresence
- TranscriptSessionUpdateMapperTest
- headroom
- Process
- Functional Programming Principles
- Headroom Consultation
- AcpLaunchArguments
- PtyResumeStrategy
- AgentWorktreePathMapperTest
- AgentWorktreeServiceTest
- AcpResumeStrategyTest
- Vertical slice migration
- Domain Docs
- Issue tracker: Repo PRDs (`docs/prds/`)
- AgentEditorTabTitleProvider
- Warning Hygiene
- AccumulatedUsage
- AuthMethodSupport
- TranscriptBadgeStyle
- TranscriptStreamingCursor
- OpenAgentEditorAction
- .toAgentServerEntry
- SlashCommandMatcherTest
- LaunchModeTest
- custom
- Agentic Development Usage
- AcpClientCapabilities
- TranscriptFenceLanguageResolver
- SessionScopeResolverTest
- UserMcpConfigParserTest
- WorktreeLaunchCoordinatorTest
- gradlew
- Documentation
- Runtime Handoff
- SessionScopeResolver
- .invokeIdeAction
- .getUserData
- EnvironmentVariableText
- AcpClientCapabilitiesTest
- AgentEditorFactoryTest
- EnvironmentVariableTextTest
- API Design Basics
- Code Formatting
- Dependency Boundaries
- Logging Practices
- Result Handling
- Testing Guidelines
- generate_release_notes.sh
- TranscriptTextTruncation
- ExecutionTarget
- ResumeCapability
- TranscriptFencedAgentTextLimitsTest
- Engineering Wiki Log
- Triage Labels
- adr-discipline.md
- current-state.md
- domain-language.md
- install-git-hooks.sh
- pre-commit
- .getUserData
- AcpUiMetrics.kt
- AgentConfigsTableColumns.kt
- AbstractTableModel
- ActionGroup
- ActionUpdateThread
- AnAction
- AnActionEvent
- Any
- Array
- AuthMethod
- BackgroundEditorHighlighter
- Boolean
- Class
- Color
- com
- CompletableDeferred
- Component
- ComponentEvent
- ContentBlock
- DateTimeFormatter
- Disposable
- DumbAware
- DumbAwareAction
- FileEditor
- FileEditorLocation
- FileEditorState
- Font
- GitRepository
- Int
- IntArray
- javax
- JBLabel
- JBTable
- JCheckBox
- JComponent
- JEditorPane
- JLabel
- JPanel
- JsonObject
- Key
- KeyEvent
- LinkedHashMap
- List
- Long
- Map
- McpServer
- MutableList
- MutableMap
- Pair
- Path
- PermissionOption
- PersistentStateComponent
- Process
- Project
- PropertyChangeListener
- RequestPermissionOutcome
- RequestPermissionResponse
- Result
- SessionUpdate
- Set
- ShellTerminalWidget
- String
- StringBuilder
- StructureViewBuilder
- T
- ToolCallContent
- ToolCallStatus
- ToolKind
- Unit
- VirtualFile
- AcpEditorContext

## God Nodes (most connected - your core abstractions)
1. `featureIds` - 78 edges
2. `AgentSettingsState` - 66 edges
3. `TranscriptModel` - 48 edges
4. `StructuredUpdate` - 42 edges
5. `PlanEntry` - 42 edges
6. `TranscriptBlock` - 38 edges
7. `PlanPanel` - 37 edges
8. `RenderedBlock` - 36 edges
9. `AgentWorktreeStateService` - 34 edges
10. `PtyAgentEditor` - 32 edges

## Surprising Connections (you probably didn't know these)
- `defaultSessionController()` --calls--> `AcpSessionControllerImpl`  [INFERRED]
  src/main/kotlin/com/oaalto/agent/acp/AcpAgentEditor.kt → src/main/kotlin/com/oaalto/agent/acp/AcpSessionControllerImpl.kt
- `create()` --calls--> `IdeScopedFileSystemAccess`  [INFERRED]
  src/main/kotlin/com/oaalto/agent/acp/AcpClientSessionOperationsImpl.kt → src/main/kotlin/com/oaalto/agent/acp/filesystem/IdeScopedFileSystemAccess.kt
- `create()` --calls--> `TranscriptBlockViewFactory`  [INFERRED]
  src/main/kotlin/com/oaalto/agent/acp/TranscriptPanel.kt → src/main/kotlin/com/oaalto/agent/acp/TranscriptBlockViewFactory.kt
- `applyCursorResumeFallbackForLocal()` --calls--> `CursorResumeProbeRequest`  [INFERRED]
  src/main/kotlin/com/oaalto/agent/pty/PtyEditorSupport.kt → src/main/kotlin/com/oaalto/agent/worktree/resume/CursorResumeProbe.kt
- `applyCursorResumeFallbackForWsl()` --calls--> `CursorResumeProbeRequest`  [INFERRED]
  src/main/kotlin/com/oaalto/agent/pty/PtyEditorSupport.kt → src/main/kotlin/com/oaalto/agent/worktree/resume/CursorResumeProbe.kt

## Import Cycles
- None detected.

## Communities (243 total, 126 thin omitted)

### Community 0 - "PlanEntry"
Cohesion: 0.05
Nodes (24): Color, Font, JLabel, JPanel, PlanPanel, PlanPanelRenderer, File, Items (+16 more)

### Community 1 - "ASTNode"
Cohesion: 0.07
Nodes (30): ASTNode, IElementType, TranscriptHtmlBuilder, BlockHandlers, BlockQuote, CodeBlock, CustomHtml, Image (+22 more)

### Community 2 - "featureIds"
Cohesion: 0.03
Nodes (78): featureIds, agentSetup.domainLayout.single-context, agentSetup.issueTracker.repo-prd, contextOptimization.headroom.piExtension, contextOptimization.headroom.rule, graphify.enabled, install.skillSource.upstream, rule.adr-discipline (+70 more)

### Community 3 - "StructuredUpdate"
Cohesion: 0.05
Nodes (25): AcpPromptEventDispatcher, SessionUpdate, AcpSessionListener, Create, PlanTranscriptRegistry, PlanUpdateResult, Update, PlanUpdateMapper (+17 more)

### Community 4 - "TranscriptPanel"
Cohesion: 0.06
Nodes (23): DialogWrapper, JBScrollPane, AcpEditorLayout, EditorLayoutComponents, JPanel, EditorFactoryTranscriptCodeBlockViewFactory, JComponent, PlainMonospaceTranscriptCodeBlockViewFactory (+15 more)

### Community 5 - "TranscriptBodyPart"
Cohesion: 0.07
Nodes (17): EmbeddedResourceResource, HighlightedCounter, TranscriptBlockConverter, Code, Html, TranscriptBodyPart, ContentBlock, SessionUpdate (+9 more)

### Community 6 - "TranscriptColorProvider"
Cohesion: 0.08
Nodes (10): DefaultTranscriptColorProvider, Color, ToolCallStatus, TranscriptColorProvider, Color, TranscriptPalette, ToolCallStatus, ToolKind (+2 more)

### Community 7 - "McpServer"
Cohesion: 0.09
Nodes (17): EnvVariable, createDefault(), DefaultMcpCapabilityBridge, McpServer, McpCapabilityBridge, IdeaMcpServerSource, JsonObject, McpServer (+9 more)

### Community 8 - "agent-cli-overview.md"
Cohesion: 0.05
Nodes (39): Agent Synthesis, Open Questions, Related, Summary, Verified Facts, Agent Synthesis, Domain context & ACP transcript model, Open Questions (+31 more)

### Community 9 - "AgentWorktreeStateService"
Cohesion: 0.08
Nodes (12): getActiveRecordsForConfiguration(), touch(), AgentWorktreeStateService, getInstance(), PersistentStateComponent, ManagedWorktreeRecord, PendingLaunch, StoredPendingLaunch (+4 more)

### Community 10 - "PermissionCoordinator"
Cohesion: 0.09
Nodes (19): defaultPermissionOptions(), isAllow(), PermissionOption, RequestPermissionOutcome, RequestPermissionResponse, SessionUpdate, ToolKind, PermissionCoordinator (+11 more)

### Community 11 - "AcpAgentEditor"
Cohesion: 0.09
Nodes (10): AcpAgentEditor, BackgroundEditorHighlighter, Disposable, FileEditor, FileEditorLocation, FileEditorState, JComponent, PropertyChangeListener (+2 more)

### Community 12 - "AuthPromptPanel"
Cohesion: 0.12
Nodes (15): JButton, PermissionOptionKind, AuthPromptResult, AuthPromptUi, Cancelled, Continue, AuthPromptPanel, CompletableDeferred (+7 more)

### Community 13 - "AcpClientSessionOperationsImpl"
Cohesion: 0.10
Nodes (19): ClientSessionOperations, CreateTerminalResponse, JsonElement, JsonRpcException, KillTerminalCommandResponse, ReadTextFileResponse, ReleaseTerminalResponse, AcpClientSessionOperationsImpl (+11 more)

### Community 14 - "TranscriptModel"
Cohesion: 0.10
Nodes (3): TranscriptModel, TranscriptModelPlanTest, TranscriptModelUsageAccumulationTest

### Community 15 - "Your task (in order)"
Cohesion: 0.07
Nodes (29): Agent setup, Catalog tailoring, Commands & tooling, Complete agent setup — existing repository, Constraints (mandatory), Definition of done wiring, Domain terms, Duplicate content audit (+21 more)

### Community 16 - "Your task (in order)"
Cohesion: 0.07
Nodes (29): Agent setup, Catalog tailoring, Commands & tooling, Complete agent setup — update existing ADC installation, Constraints (mandatory), Definition of done wiring, Domain terms, Duplicate content audit (+21 more)

### Community 17 - "TranscriptHtmlAppender"
Cohesion: 0.13
Nodes (3): TranscriptHtmlAppender, JEditorPane, TranscriptHtmlAppenderStreamingTest

### Community 18 - "CursorResumeProbeLogic"
Cohesion: 0.11
Nodes (6): AgentCommandBuilder, AgentWslCommandRequest, CursorResumeProbe, CursorResumeProbeLogic, CursorResumeProbeRequest, AgentCommandBuilderTest

### Community 22 - "contentTemplateHashes"
Cohesion: 0.07
Nodes (27): contentTemplateHashes, AGENTS.md, .agents/rules/adr-discipline.md, .agents/rules/api-design-basics.md, .agents/rules/code-format.md, .agents/rules/current-state.md, .agents/rules/dependency-boundaries.md, .agents/rules/documentation.md (+19 more)

### Community 23 - "PromptInputBar"
Cohesion: 0.12
Nodes (8): AvailableCommand, JBList, JBPopup, SlashCommand, JComponent, KeyEvent, PromptInputBar, SlashCommandMatcher

### Community 24 - "wiki"
Cohesion: 0.08
Nodes (26): index, log, schema, concepts, debugging, known-traps, source-notes, subsystems (+18 more)

### Community 25 - "skills"
Cohesion: 0.08
Nodes (26): skills, code-review, codebase-design, diagnosing-bugs, domain-modeling, graphify, grill-with-docs, grilling (+18 more)

### Community 26 - "PtyAgentEditor"
Cohesion: 0.09
Nodes (10): BackgroundEditorHighlighter, Disposable, FileEditor, FileEditorLocation, FileEditorState, JComponent, PropertyChangeListener, StructureViewBuilder (+2 more)

### Community 27 - "rules"
Cohesion: 0.08
Nodes (25): rules, adr-discipline, api-design-basics, changelog, code-format, commit, current-state, decision-making (+17 more)

### Community 28 - "TranscriptFooter"
Cohesion: 0.16
Nodes (4): Cost, JPanel, TranscriptFooter, TranscriptFooterTest

### Community 29 - "autoIncluded"
Cohesion: 0.08
Nodes (24): autoIncluded, AGENTS.md, CONTEXT.md, docs/adr/, docs/agent-commands.md, docs/agents/domain.md, docs/agents/issue-tracker.md, docs/agents/triage-labels.md (+16 more)

### Community 30 - "Full File Contents"
Cohesion: 0.08
Nodes (23): 1. TranscriptHtmlAppender.kt, 2. AcpAgentEditor.kt, 3. TranscriptStreamingCursor.kt, 4. AcpPromptEventDispatcher.kt, 5. AcpEditorLayout.kt, 6. AcpClientSessionOperationsImpl.kt, 7. TranscriptHtmlAppenderStreamingTest.kt (includes AcpPromptEventDispatcherTest), 8. TranscriptStreamingCursorTest.kt (+15 more)

### Community 31 - "AgentTextRow"
Cohesion: 0.14
Nodes (17): EmptyBorder, JTextPane, AgentTextRow, applyStyleToRun(), componentResized(), createHtmlPane(), createThematicBreak(), escapeHtml() (+9 more)

### Community 32 - "AgentSettingsState"
Cohesion: 0.12
Nodes (6): AgentCliConfiguration, AgentSettingsState, getInstance(), PersistentStateComponent, State, AgentSettingsStateTest

### Community 33 - "AgentSettingsConfigurable"
Cohesion: 0.15
Nodes (8): SearchableConfigurable, AgentSettingsConfigurable, com, JBLabel, JBTable, JCheckBox, JComponent, JPanel

### Community 34 - "AcpSessionControllerImpl"
Cohesion: 0.15
Nodes (8): AgentInfo, Client, ClientSession, Event, Job, Protocol, AcpSessionControllerImpl, CompletableDeferred

### Community 35 - "TranscriptBlock"
Cohesion: 0.18
Nodes (12): AuthFailureLine, ErrorLine, FinalAgentText, PlainLine, PlanBlock, StreamingAgentText, Thought, ToolCallBlock (+4 more)

### Community 36 - "Changelog"
Cohesion: 0.28
Nodes (21): 2026-02-19, 2026-02-20, 2026-02-23, 2026-04-02, 2026-04-03, 2026-04-26, 2026-05-25, 2026-06-16 (+13 more)

### Community 37 - "Implementation Steps (Easiest → Hardest)"
Cohesion: 0.10
Nodes (20): ACP Client Transcript Output: Implementation Roadmap, ACP Update Types (from `SessionUpdate`), Architectural Notes, Current event flow, Current State, Implementation Steps (Easiest → Hardest), Step 1: Separate transcript into a structured component (huge payoff, low effort), Step 2: Color-code status badges for tool calls (small effort, clear UX win) (+12 more)

### Community 38 - "CollapsibleToolPanel"
Cohesion: 0.28
Nodes (4): Dimension, CollapsibleToolPanel, JEditorPane, JPanel

### Community 39 - "Your task (in order)"
Cohesion: 0.11
Nodes (18): Catalog tailoring, Complete agent setup — greenfield install, Constraints (mandatory), Definition of done wiring, Duplicate content audit, Graphify automation, How to work with the human, Initial graphify (+10 more)

### Community 40 - "AgentConfigRow"
Cohesion: 0.17
Nodes (6): AgentConfigRow, normalizeExecutionTarget(), normalizeLaunchMode(), toRow(), AgentConfigRowColumns, AgentSettingsValidation

### Community 41 - "AgentWorktreeGitSupport"
Cohesion: 0.20
Nodes (8): FilePath, Git, GitCommandResult, AgentWorktreeGitSupport, GitRepository, Project, Result, ParsedWorktree

### Community 42 - "AgentLaunchContext"
Cohesion: 0.19
Nodes (5): AgentProjectContext, toAgentProjectContext(), AgentLaunchContext, AcpLaunchArgumentsTest, AcpProcessLauncherTest

### Community 43 - "ScopedFileSystemOperations"
Cohesion: 0.22
Nodes (6): create(), InScope, OutOfScope, ScopedFileSystemOperations, ScopeResult, ScopedFileSystemOperationsTest

### Community 44 - "Agentic Development Configurator setup"
Cohesion: 0.12
Nodes (16): Agentic Development Configurator setup, Auto-detection, Cleaning up, Curated backend paths, First graph build, Graphify extraction backend, Headroom (context optimization), Install steps (+8 more)

### Community 45 - "How pi-agent Handles Displaying User Input and Model Output"
Cohesion: 0.12
Nodes (16): 1. Overview, 2.1 The Editor, 2.2 Editor Border, 2.3 Message Queue Display, 2. User Input Display, 3.1 Event Pipeline, 3.2 Assistant Message Rendering, 3.3 Tool Call and Execution Display (+8 more)

### Community 46 - "AcpLaunchPlan"
Cohesion: 0.19
Nodes (6): AcpLaunchPlan, McpServer, AcpLaunchRequest, AcpProcessLauncher, Result, AcpLaunchPlanTest

### Community 47 - "IdeScopedFileSystemAccess"
Cohesion: 0.29
Nodes (6): AccessResult, Failure, IdeScopedFileSystemAccess, T, VirtualFile, Success

### Community 48 - "AgentConfigurationResolutionInput"
Cohesion: 0.17
Nodes (6): AgentConfigurationResolution, AgentConfigurationResolutionInput, AgentConfigurationResolutionResult, AgentConfigurationSelector, Project, AgentConfigurationResolutionTest

### Community 49 - "install.sh"
Cohesion: 0.25
Nodes (13): collect_required_tools(), ensure_graphify_path(), ensure_headroom_path(), has_graphify_extraction_backend_env(), have(), plan_includes_graphify(), print_tool_help(), prior_adc_in_head() (+5 more)

### Community 50 - "AuthFlowCoordinator"
Cohesion: 0.27
Nodes (5): AuthMethodId, AuthFlowCoordinator, AuthMethod, Result, AuthMethodSupportTest

### Community 52 - "AcpSessionController"
Cohesion: 0.13
Nodes (3): defaultSessionController(), AcpSessionController, SessionSummary

### Community 54 - "AnActionEvent"
Cohesion: 0.22
Nodes (7): DeleteWorktreeAction, AnActionEvent, DumbAwareAction, ManageAgentSettingsAction, OpenOrResumeWorktreeAction, RunAgentInCurrentProjectAction, RunAgentInNewWorktreeAction

### Community 55 - "RecordingCodeBlockViewFactory"
Cohesion: 0.18
Nodes (5): javax, JPanel, RecordingCodeBlockViewFactory, TranscriptBlockViewFactoryTest, TranscriptTextTruncationTest

### Community 56 - "install.ps1"
Cohesion: 0.23
Nodes (10): Get-RequiredTools(), Invoke-EnvCheck(), Show-GraphifyExtractionBackendWarning(), Show-HeadroomWindowsBuildHelp(), Show-ToolHelp(), Test-GraphifyExtractionBackendEnv(), Test-Have(), Test-MsvcLinkAvailable() (+2 more)

### Community 57 - "triageLabels"
Cohesion: 0.18
Nodes (14): agentSetup, domainLayout, issueTracker, required, triageLabels, gitRemoteUrl, otherDescription, type (+6 more)

### Community 58 - "AgentWorktreeService"
Cohesion: 0.29
Nodes (8): R, AgentWorktreeService, bind(), CreatedWorktree, GitRepository, Result, ManagedWorktree, resumeArgumentsForConfiguration()

### Community 59 - "Agent CLI"
Cohesion: 0.14
Nodes (13): Agent CLI, Developer documentation, How to use, Installation, License, Requirements and notes, Running in WSL2 on Windows, Screenshots (+5 more)

### Community 60 - "AgentSettingsUiFactory"
Cohesion: 0.19
Nodes (7): DetailPanelHints, EnvironmentVariablesEditor, AgentSettingsUiFactory, JBTable, JCheckBox, JComponent, JPanel

### Community 61 - "EnvironmentVariablesTableModel"
Cohesion: 0.16
Nodes (3): EnvironmentVariableRow, EnvironmentVariablesTableModel, AbstractTableModel

### Community 62 - "skillSource"
Cohesion: 0.15
Nodes (13): acknowledgeUnsafeCommands, piPackages, skillSource, install, diagnosing-bugs, grill-with-docs, grilling, improve-codebase-architecture (+5 more)

### Community 63 - "AGENTS.md"
Cohesion: 0.17
Nodes (11): Before Committing, Commit Strategy, decision-making, Decision Making and User Guidance, Formatting guidance, role, Rules index, signature (+3 more)

### Community 64 - "TranscriptToolCallDiffRenderer"
Cohesion: 0.28
Nodes (3): IntArray, ToolCallContent, TranscriptToolCallDiffRenderer

### Community 65 - ".createEditor"
Cohesion: 0.19
Nodes (8): AgentEditorFactory, FileEditor, Project, from(), fromDisplayLabel(), LaunchMode, ACP_CLIENT, PTY_PASSTHROUGH

### Community 66 - ".buildWslTerminalStartupRequest"
Cohesion: 0.29
Nodes (5): TerminalStartupRequest, applyCursorResumeFallbackForLocal(), applyCursorResumeFallbackForWsl(), resolvePtyConfiguration(), resolvePtyExecutionTarget()

### Community 67 - "Engineering Wiki Schema"
Cohesion: 0.15
Nodes (12): Engineering Wiki Schema, Evidence Rules, Frontmatter, Links, Log Format, Mechanical Lint, Page Body, Page Types (+4 more)

### Community 68 - "ADR 0003: Per-project agent selection"
Cohesion: 0.17
Nodes (11): ADR 0003: Per-project agent selection, Alternatives considered, Consequences, Context, Decision, Migration, Negative, Neutral (+3 more)

### Community 69 - "manifest.json"
Cohesion: 0.17
Nodes (11): agenticConfigVersion, bundleOnlyRules, generatedAt, installPlan, skillsAgentFlag, steps, targetAgent, version (+3 more)

### Community 70 - "definition-of-done"
Cohesion: 0.17
Nodes (12): 1. Strict Output Rules, 2. Format Expectations, 3. Context and Scope, 4. Execution and Verification, definition-of-done, Documentation, ponytail, Refactoring (+4 more)

### Community 71 - "restricted-operations"
Cohesion: 0.17
Nodes (12): Agent skills, Always allowed (no permission needed), Domain docs, Issue tracker, Triage labels, Permission request format, Policy, Ponytail, lazy senior dev mode (+4 more)

### Community 72 - "AgentFileEditorProvider"
Cohesion: 0.20
Nodes (7): FileEditorPolicy, FileEditorProvider, AgentFileEditorProvider, DumbAware, FileEditor, Project, VirtualFile

### Community 73 - "AgentVirtualFile"
Cohesion: 0.17
Nodes (5): LightVirtualFile, AgentVirtualFile, AgentPendingLaunchStartupActivity, Project, StartupActivity

### Community 74 - "ShellPaneHost"
Cohesion: 0.32
Nodes (4): ShellStartupOptions, JComponent, ShellTerminalWidget, ShellPaneHost

### Community 75 - "LaunchResumePlan"
Cohesion: 0.18
Nodes (7): AcpResumeStrategy, AcpLoad, AcpNewSession, AcpPickSession, LaunchResumePlan, Pty, ResumeStrategy

### Community 77 - "changelog"
Cohesion: 0.18
Nodes (11): Architecture and exploration questions, Both tracks, changelog, commit, How project rules apply, How to update, Narrative overview questions, Scoped rule loading policy (+3 more)

### Community 78 - "CollapsibleToolPanel.kt"
Cohesion: 0.29
Nodes (6): MouseEvent, componentResized(), keyPressed(), ComponentEvent, KeyEvent, mouseClicked()

### Community 79 - "SelectAgentConfigurationActionGroup"
Cohesion: 0.20
Nodes (6): ActionGroup, ActionUpdateThread, AnAction, AnActionEvent, DumbAware, SelectAgentConfigurationActionGroup

### Community 80 - "DetailPanelBindings"
Cohesion: 0.25
Nodes (4): DetailPanelBindings, AgentSettingsDetailPanelSupport, JBLabel, JCheckBox

### Community 81 - ".buildWorktreeChildren"
Cohesion: 0.33
Nodes (4): ActionGroup, AnAction, com, RunAgentSplitActionGroup

### Community 82 - "Wiki"
Cohesion: 0.18
Nodes (10): Before Commit, Operations, Output, Pre-task consultation, Purpose, Wiki, `/wiki-ingest`, `/wiki-lint` (+2 more)

### Community 83 - "Agent Commands"
Cohesion: 0.20
Nodes (9): Agent Commands, Build / Typecheck, Docs Checks, Format, Lint, Runtime-Restricted Checks, Test, To Complete (+1 more)

### Community 84 - "Development Guide"
Cohesion: 0.20
Nodes (9): Build plugin workflow, CI and release flow, Common Gradle tasks, Development Guide, IntelliJ Platform references, Local testing checklist, Prerequisites, Project overview (+1 more)

### Community 85 - "During the session"
Cohesion: 0.20
Nodes (9): Challenge against the glossary, Cross-reference with code, Discuss concrete scenarios, Domain awareness, During the session, File structure, Offer ADRs sparingly, Sharpen fuzzy language (+1 more)

### Community 86 - "RunAgentSplitButtonAction"
Cohesion: 0.20
Nodes (5): SplitButtonAction, ActionUpdateThread, DumbAware, RunAgentSplitButtonAction, RunAgentSplitButtonActionTest

### Community 87 - "AcpJsonImportDraft"
Cohesion: 0.31
Nodes (6): AcpJsonImportDraft, AcpJsonImporter, AcpJsonImportResult, copyOf(), JsonObject, Result

### Community 88 - "WorktreeLaunchCoordinator"
Cohesion: 0.33
Nodes (3): Project, WorktreeLaunchCoordinator, WslPaths

### Community 89 - "WslPathResolver"
Cohesion: 0.33
Nodes (3): ResolvedWslPath, WorkingDirectoryResolver, WslPathResolver

### Community 93 - "Test-Driven Development"
Cohesion: 0.20
Nodes (9): 1. Planning, 2. Tracer Bullet, 3. Incremental Loop, 4. Refactor, Anti-Pattern: Horizontal Slices, Checklist Per Cycle, Philosophy, Test-Driven Development (+1 more)

### Community 94 - "ADR 0001: Custom ACP client in the Agent CLI plugin"
Cohesion: 0.22
Nodes (8): ADR 0001: Custom ACP client in the Agent CLI plugin, Alternatives considered, Consequences, Context, Decision, Negative, Neutral, Positive

### Community 95 - "ADR 0002: Kotlin ACP SDK for the plugin client"
Cohesion: 0.22
Nodes (8): ADR 0002: Kotlin ACP SDK for the plugin client, Alternatives considered, Consequences, Context, Decision, Negative, Neutral, Positive

### Community 96 - "Vertical slice boundaries"
Cohesion: 0.22
Nodes (8): Adapters vs domain, Cross-slice calls, Duplicate over shared (default), Kernel and composition, Project overlay, Public entry only, Tests, Vertical slice boundaries

### Community 97 - "wiki-lint.mjs"
Cohesion: 0.22
Nodes (5): exclude, files, pathMapPath, root, wikiDir

### Community 98 - "TerminalSessionRegistry"
Cohesion: 0.28
Nodes (3): TerminalSession, ShellTerminalWidget, TerminalSessionRegistry

### Community 99 - "bindTranscriptBlock"
Cohesion: 0.42
Nodes (8): bindAuthFailureLine(), bindErrorLine(), bindFinalAgent(), bindPlainLine(), bindStreamingAgent(), bindThought(), bindTranscriptBlock(), bindUserEcho()

### Community 102 - "ProjectAgentSelectionState"
Cohesion: 0.28
Nodes (3): PersistentStateComponent, ProjectAgentSelectionState, State

### Community 105 - "selection"
Cohesion: 0.25
Nodes (8): enabled, selection, contextOptimization, graphify, targetAgent, version, targetAgent, id

### Community 106 - "package.json"
Cohesion: 0.25
Nodes (7): devDependencies, name, private, scripts, prepare, wiki-lint, version

### Community 107 - "Review"
Cohesion: 0.25
Nodes (7): Assumption, Output, Purpose, Read First, Review, Review Priorities, Rules

### Community 109 - "AiAssistantPresence"
Cohesion: 0.29
Nodes (3): AiAssistantPresence, fromPluginProbe(), AiAssistantPresenceTest

### Community 111 - "headroom"
Cohesion: 0.29
Nodes (7): headroom, enabled, guide, mcp, piExtension, rule, runtime

### Community 112 - "Process"
Cohesion: 0.29
Nodes (6): 1. Explore, 2. Present candidates as an HTML report, 3. Grilling loop, Glossary, Improve Codebase Architecture, Process

### Community 113 - "Functional Programming Principles"
Cohesion: 0.29
Nodes (6): Core vs. Shell, Data Transformation Style, Encapsulated Internal Mutation, Error Handling, Functional Programming Principles, Wrapping Impure Boundaries

### Community 114 - "Headroom Consultation"
Cohesion: 0.29
Nodes (6): Headroom Consultation, Operations, Pre-task consultation, Repository notes, Source hierarchy, When to consult

### Community 120 - "Vertical slice migration"
Cohesion: 0.29
Nodes (6): Deep module check, Mode EXISTING — Strangler migration, Mode NEW — Greenfield, Step 0 — Detect layout and mode, Vertical slice migration, Where does this code belong?

### Community 121 - "Domain Docs"
Cohesion: 0.33
Nodes (5): Before exploring, read these, Domain Docs, File structure, Flag ADR conflicts, Use the glossary's vocabulary

### Community 122 - "Issue tracker: Repo PRDs (`docs/prds/`)"
Cohesion: 0.33
Nodes (5): Conventions, Issue tracker: Repo PRDs (`docs/prds/`), Related configuration, When a skill says "fetch the relevant ticket", When a skill says "publish to the issue tracker"

### Community 123 - "AgentEditorTabTitleProvider"
Cohesion: 0.33
Nodes (4): EditorTabTitleProvider, AgentEditorTabTitleProvider, Project, VirtualFile

### Community 124 - "Warning Hygiene"
Cohesion: 0.33
Nodes (5): Enforcement, Migration (legacy code), Rationale, Warning Hygiene, Zero-Suppression Policy

### Community 129 - "OpenAgentEditorAction"
Cohesion: 0.40
Nodes (3): AnActionEvent, DumbAwareAction, OpenAgentEditorAction

### Community 133 - "custom"
Cohesion: 0.40
Nodes (5): piPackages, rules, shell, skills, custom

### Community 134 - "Agentic Development Usage"
Cohesion: 0.40
Nodes (4): Agentic Development Usage, Artifact Rules, Daily Skill Use, Human Review Checklist

### Community 135 - "AcpClientCapabilities"
Cohesion: 0.50
Nodes (3): ClientCapabilities, AcpClientCapabilities, Support

### Community 140 - "gradlew"
Cohesion: 0.83
Nodes (3): gradlew script, die(), warn()

### Community 141 - "Documentation"
Cohesion: 0.50
Nodes (3): Context And Decisions, Documentation, Review Expectations

### Community 142 - "Runtime Handoff"
Cohesion: 0.50
Nodes (3): Handoff Format, Review Expectations, Runtime Handoff

### Community 158 - "ExecutionTarget"
Cohesion: 0.67
Nodes (3): ExecutionTarget, LOCAL, WSL

## Knowledge Gaps
- **633 isolated node(s):** `Prerequisites`, `Install the matching pip extra`, `Curated backend paths`, `First graph build`, `pipx alternative (operator-managed)` (+628 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **126 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `AgentSettingsState` connect `AgentSettingsState` to `.toAgentServerEntry`, `McpServer`, `PermissionCoordinator`, `AcpAgentEditor`, `ExecutionTarget`, `ResumeCapability`, `AgentSettingsConfigurable`, `AgentConfigRow`, `AcpLaunchPlan`, `AgentConfigurationResolutionInput`, `AnActionEvent`, `AgentWorktreeService`, `.buildWslTerminalStartupRequest`, `LaunchResumePlan`, `PtyResumeStrategyTest`, `.buildWorktreeChildren`, `AcpJsonImportDraft`, `WorktreeLaunchCoordinator`, `AcpJsonImporterExporterTest`, `AgentSettingsStateSupport`, `AcpLaunchArguments`, `PtyResumeStrategy`?**
  _High betweenness centrality (0.106) - this node is a cross-community bridge._
- **Why does `TranscriptBlock` connect `TranscriptBlock` to `PlanEntry`, `bindTranscriptBlock`, `StructuredUpdate`, `TranscriptPanel`, `CollapsibleToolPanel`, `TranscriptModel`, `AgentTextRow`?**
  _High betweenness centrality (0.082) - this node is a cross-community bridge._
- **Are the 6 inferred relationships involving `AgentSettingsState` (e.g. with `.`skips UI when allow always is remembered`()` and `.`persists allow always across store instances`()`) actually correct?**
  _`AgentSettingsState` has 6 INFERRED edges - model-reasoned connections that need verification._
- **Are the 32 inferred relationships involving `TranscriptModel` (e.g. with `.`creates new plan block on first StartOrUpdatePlan`()` and `.`creates separate blocks for different plan IDs`()`) actually correct?**
  _`TranscriptModel` has 32 INFERRED edges - model-reasoned connections that need verification._
- **Are the 20 inferred relationships involving `PlanEntry` (e.g. with `.`escapes HTML special characters in content`()` and `.`escapes plan ID in element ID attribute`()`) actually correct?**
  _`PlanEntry` has 20 INFERRED edges - model-reasoned connections that need verification._
- **What connects `Prerequisites`, `Install the matching pip extra`, `Curated backend paths` to the rest of the system?**
  _633 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `PlanEntry` be split into smaller, more focused modules?**
  _Cohesion score 0.05217757205975174 - nodes in this community are weakly interconnected._