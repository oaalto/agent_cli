---
title: ACP client subsystem
type: subsystem
status: current
updated: 2026-07-24
sources:
  - src/main/kotlin/com/oaalto/agent/acp/AcpAgentEditor.kt
  - src/main/kotlin/com/oaalto/agent/acp/AcpClientSessionOperationsFactory.kt
  - src/main/kotlin/com/oaalto/agent/acp/AcpClientSessionOperationsImpl.kt
  - src/main/kotlin/com/oaalto/agent/acp/AcpSessionControllerImpl.kt
  - src/main/kotlin/com/oaalto/agent/acp/filesystem/ScopedFileSystemAccess.kt
  - src/main/kotlin/com/oaalto/agent/acp/filesystem/SessionFilesystemOperations.kt
  - src/main/kotlin/com/oaalto/agent/acp/AcpEditorLayout.kt
  - src/main/kotlin/com/oaalto/agent/acp/TranscriptViewController.kt
  - src/main/kotlin/com/oaalto/agent/acp/TranscriptModel.kt
  - src/main/kotlin/com/oaalto/agent/acp/TranscriptPanel.kt
  - src/main/kotlin/com/oaalto/agent/acp/TranscriptBlockViewFactory.kt
  - src/main/kotlin/com/oaalto/agent/acp/TranscriptBlockLabelBinder.kt
  - src/main/kotlin/com/oaalto/agent/acp/StructuredUpdate.kt
  - src/main/kotlin/com/oaalto/agent/acp/TranscriptEventIngestion.kt
  - src/main/kotlin/com/oaalto/agent/acp/TranscriptColorProvider.kt
  - src/main/kotlin/com/oaalto/agent/acp/TranscriptFooter.kt
  - src/main/kotlin/com/oaalto/agent/acp/TranscriptMarkdownRenderer.kt
  - src/main/kotlin/com/oaalto/agent/acp/ui/PromptInputBar.kt
  - src/main/kotlin/com/oaalto/agent/acp/ui/SlashCommandMatcher.kt
  - src/main/kotlin/com/oaalto/agent/acp/plan/PlanPanel.kt
  - src/main/kotlin/com/oaalto/agent/acp/plan/PlanPanelRenderer.kt
  - docs/adr/0001-custom-acp-client-in-plugin.md
---

# ACP client subsystem

## Summary

The ACP client slice implements the Agent Client Protocol **client** in-process within the IntelliJ plugin. It owns the split UI (transcript column + prompt/shell bottom), the transcript rendering stack, session lifecycle, and the `ClientSessionOperations` bridge to the ACP agent subprocess. Entry point: `AcpAgentEditor`.

## Verified Facts

### Architecture

- `AcpAgentEditor` implements `FileEditor` and `Disposable`; it is the UI entry point for ACP mode.
- The plugin implements the ACP **client** in-process — it does **not** delegate sessions to JetBrains AI Chat (see ADR 0001).
- The ACP agent subprocess communicates via stdio JSON-RPC; the plugin is the client side.

### UI layout (`AcpEditorLayout`)

The root panel is a nested `Splitter`:

```
mainSplitter (TRANSCRIPT_SPLIT_RATIO = 0.72f)
├── transcriptColumn (left, 72%)
│   ├── transcriptArea (TranscriptPanel wrapped in JBScrollPane, CENTER)
│   ├── permissionPromptPanel (SOUTH)
│   └── authPromptPanel (NORTH)
└── bottomSplitter (right, 28%)
    ├── promptInputBar (top, PROMPT_SPLIT_RATIO = 0.2f)
    └── shellPaneHost (bottom, 80%)
transcriptFooter (SOUTH of mainSplitter)
```

- Layout split: ~72% transcript column / ~28% bottom (20% prompt input / 80% shell) per `AcpEditorLayout.buildRootPanel`.

### Transcript rendering stack

The transcript renders via a vertical `BoxLayout` column of block rows inside a `JBScrollPane`. Each `StructuredUpdate` flows through a fixed chain:

1. **`TranscriptViewController`** — EDT-safe bridge between `TranscriptModel` and `TranscriptPanel`. Public API: `apply(StructuredUpdate)`, `appendPlainLine()`, `appendError()`, `finalizeAgentStream()`.
2. **`TranscriptModel`** — holds `List<TranscriptBlock>`; emits blocks on updates.
3. **`TranscriptPanel`** — renders blocks via `TranscriptBlockViewFactory` (Swing block views, `JTextPane` / nested `JEditorPane` fragments per block row).
4. **`TranscriptBlockLabelBinder`** — applies styling and the `CURSOR_CHAR` streaming indicator to `StreamingAgentText` blocks.
5. **`TranscriptRenderer`** — text extraction and plain-text formatters (not Swing view rendering); consumed by ingestion and rendering helpers.

### Prompt event routing (`TranscriptEventIngestion`)

- `ingest(update)` returns `List<StructuredUpdate>`: non-chunk updates finalize the active agent stream first (`FinalizeAgentStream`), then map the update.
- `ingestPromptCompleted()` returns a single `FinalizeAgentStream`.
- Agent message chunks stream in place without finalization.

### Session operations (deep module: `SessionFilesystemOperations`)

Filesystem policy is owned by `SessionFilesystemOperations` — a single deep module that concentrates scope → permission → VFS → line-slicing logic behind one seam.

- **`fsReadTextFile()` / `fsWriteTextFile()`** — delegated to `SessionFilesystemOperations`. The SDK adapter (`AcpClientSessionOperationsImpl`) maps sealed results to `ReadTextFileResponse` / `WriteTextFileResponse` / `JsonRpcException`.
- **`notify(notification, _meta)`** — routes `SessionUpdate` events through `TranscriptEventIngestion.ingest()`.
- **`requestPermissions()`** — delegates to `PermissionCoordinator.requestSessionPermission()`.
- **`terminalCreate()` / `terminalOutput()` / `terminalRelease()`** — manage terminal sessions via `TerminalSessionRegistry` and `ShellPaneHost`.

#### Composition root (`AcpClientSessionOperationsFactory`)

`AcpClientSessionOperationsFactory` (implemented by `AcpDefaultClientSessionOperationsFactory`) is the visible composition root for all `ClientSessionOperations` dependencies. `AcpSessionControllerImpl.openSession()` delegates to this factory rather than constructing dependencies inline.

#### VFS seam (`ScopedFileSystemAccess`)

`IdeScopedFileSystemAccess` is the production adapter behind the `ScopedFileSystemAccess` interface. An in-memory test adapter (`InMemoryScopedFileSystemAccess`) enables policy tests without IntelliJ platform fixtures.

#### Test surface

The deep module interface is the primary test seam — `SessionFilesystemOperationsTest` covers in-scope reads, out-of-scope rejections, permission denials, VFS read-only/ignored blocks, and line/limit slicing with the in-memory adapter.

### Event ingestion (`TranscriptEventIngestion`)

Consolidates finalize policy and `SessionUpdate` → `StructuredUpdate` mapping:

- **Agent/user/thought chunks** → `AppendAgentText`, `AppendUserEcho`, `AppendThought`.
- **Tool calls** → tool call start/delta/end variants.
- **`UsageUpdate`** → `StructuredUpdate.Usage` (feeds `TranscriptFooter`).
- **`AvailableCommandsUpdate`** → `StructuredUpdate.AvailableCommands` (feeds slash-command autocomplete).
- **Plan events** (`PlanUpdate`, `PlanUpdateV2`, `PlanRemoved`) → plan panel variants via `PlanUpdateMapper`.

### Structured updates (`StructuredUpdate`)

The transcript uses a sealed hierarchy of `StructuredUpdate` variants:

- `AppendPlainLine(line, isUserPrompt)` — plain text line (user prompts start with `> `).
- `AppendError(message)` — error message.
- `FinalizeAgentStream` — finalizes the active agent stream.
- `AvailableCommands(commands)` — updates `PromptInputBar` slash-command list.
- `Usage(used, size, cost)` — cumulative token usage and optional cost for the footer.
- Tool call variants (tool call start, delta, end).
- Plan variants (plan start, step updates, removal).

### Theme-aware colors (`TranscriptColorProvider`)

- Central color authority for transcript HTML and Swing components; adapts to IDE light/dark theme via `JBColor`.
- Registered as an IntelliJ service; `DefaultTranscriptColorProvider` is the fallback.
- Used by `TranscriptRenderer`, `PlanPanel`, badges, and tool cards for consistent theming.

### Markdown rendering (`TranscriptMarkdownRenderer`)

- Agent text and tool card bodies render via IntelliJ's `org.intellij.markdown` parser with GFM flavour.
- Produces `RenderedBlock` variants (inline text, code blocks, tables, blockquotes, images, thematic breaks).
- Replaces the retired `segmentFencedCodeBlocks` / `TextSegment` approach.

### Plan visualization (`PlanPanel`, `PlanPanelRenderer`)

- `PlanUpdate` / `PlanUpdateV2` render as in-transcript checklist panels keyed by plan ID.
- Status icons: pending `[ ]`, in-progress `[→]`, completed `[✓]` with gray/orange/green styling.
- Priority: HIGH (bold + red left border), LOW (muted gray).
- Progress summary: "N of M completed" (green when fully complete).
- `PlanRemoved` removes the panel; `PlanVariant.File` and `Markdown` have fallback rendering.
- In-place updates by plan ID via `TranscriptModel` plan tracking.

### Transcript footer (`TranscriptFooter`)

- Sticky status bar at the bottom of the transcript column (SOUTH of `mainSplitter`).
- Displays cumulative token usage (`used / size`) and optional cost from `UsageUpdate` events.
- Usage label turns orange when `used / size > 0.8`; cost label hidden when null.

### Slash-command autocomplete (`PromptInputBar`, `SlashCommandMatcher`)

- `AvailableCommandsUpdate` populates `SlashCommand` list on the prompt input bar.
- Typing `/` opens a popup (max 5 visible rows, width matches prompt field, positioned above input).
- Up/Down highlight commands while keeping focus in the prompt field; Tab completes selection.
- Filtering updates the list in place as the user types.

### Threading

- All transcript mutations are marshalled onto the EDT (Event Dispatch Thread) via `runOnEdt` callback or `SwingUtilities.invokeLater()`.
- `TranscriptViewController` accepts an optional `runOnEdt` parameter; defaults to `SwingUtilities.invokeLater` when not provided.
- The ACP agent subprocess communicates on IO threads; `ClientSessionOperations` methods are `suspend` functions that marshal results back to the EDT via the listener.

## Agent Synthesis

- When changing transcript behavior, start at `AcpAgentEditor.kt` and trace the flow: ACP events enter via `TranscriptEventIngestion` (or `notify()` via `AcpClientSessionOperationsImpl`), map to `StructuredUpdate`, then flow through `TranscriptViewController` → `TranscriptModel` → `TranscriptPanel`.
- Live agent streaming uses `TranscriptBlock.StreamingAgentText` with the `CURSOR_CHAR` indicator applied by `TranscriptBlockLabelBinder`; finalized agent text becomes `FinalAgentText`.
- Layout split is hard-coded: 72% transcript / 28% bottom, 20% prompt / 80% shell.
- The ACP client is in-process (not JetBrains AI Chat); the agent subprocess is a separate process communicating via stdio JSON-RPC.

## Open Questions

- (Consolidation closed this: `TranscriptEventIngestion` absorbs both routing paths into one seam.)

## Related

- [Domain context & ACP transcript model](../concepts/context.md)
- [Architecture decisions map](architecture.md)
- [Agent CLI overview](../concepts/agent-cli-overview.md)
- [Worktree subsystem](worktree.md)
