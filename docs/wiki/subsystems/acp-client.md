---
title: ACP client subsystem
type: subsystem
status: current
updated: 2026-07-23
sources:
  - src/main/kotlin/com/oaalto/agent/acp/AcpAgentEditor.kt
  - src/main/kotlin/com/oaalto/agent/acp/AcpClientSessionOperationsImpl.kt
  - src/main/kotlin/com/oaalto/agent/acp/AcpEditorLayout.kt
  - src/main/kotlin/com/oaalto/agent/acp/TranscriptViewController.kt
  - src/main/kotlin/com/oaalto/agent/acp/TranscriptHtmlAppender.kt
  - src/main/kotlin/com/oaalto/agent/acp/AcpPromptEventDispatcher.kt
  - src/main/kotlin/com/oaalto/agent/acp/StructuredUpdate.kt
  - src/main/kotlin/com/oaalto/agent/acp/AcpSessionListener.kt
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
│   ├── transcriptArea (scrollable JEditorPane, CENTER)
│   ├── permissionPromptPanel (SOUTH)
│   └── authPromptPanel (NORTH)
└── bottomSplitter (right, 28%)
    ├── promptInputBar (top, PROMPT_SPLIT_RATIO = 0.2f)
    └── shellPaneHost (bottom, 80%)
transcriptFooter (SOUTH of mainSplitter)
```

- Layout split: ~72% transcript column / ~28% bottom (20% prompt input / 80% shell) per `AcpEditorLayout.buildRootPanel`.

### Transcript rendering stack

1. **`TranscriptViewController`** — EDT-safe bridge between `TranscriptModel` and `TranscriptPanel`. Public API: `apply(StructuredUpdate)`, `appendPlainLine()`, `appendError()`, `finalizeAgentStream()`.
2. **`TranscriptModel`** — holds `List<TranscriptBlock>`; emits blocks on updates.
3. **`TranscriptPanel`** — renders blocks via `TranscriptBlockConverter` and `TranscriptBlockViewFactory` implementations.
4. **`TranscriptHtmlAppender`** — manages appending content into a `JEditorPane` in `text/html` mode. Maintains `committedBodyHtml` as the source of truth for finalized content; the active stream block is spliced at the document tail on each chunk.
5. **`TranscriptRenderer`** — converts `TranscriptBlock` to HTML fragments.

### Prompt event routing (`AcpPromptEventDispatcher`)

- `dispatchSessionUpdate(update, listener)` routes `SessionUpdate` events to the listener.
- **Agent message chunks** (`SessionUpdate.AgentMessageChunk`) go directly through `TranscriptSessionUpdateMapper.mapAgentChunk()`.
- **Non-chunk updates** trigger `FinalizeAgentStream` first, then map the update through `TranscriptSessionUpdateMapper.mapUpdate()`.
- `dispatchPromptCompleted(listener)` finalizes the active agent stream.

### Session operations (`AcpClientSessionOperationsImpl`)

Implements `com.agentclientprotocol.common.ClientSessionOperations`:

- **`notify(notification, _meta)`** — routes `SessionUpdate` events through `AcpPromptEventDispatcher.dispatchSessionUpdate()`.
- **`requestPermissions()`** — delegates to `PermissionCoordinator.requestSessionPermission()`.
- **`fsReadTextFile()`** — resolves scope via `ScopedFileSystemOperations`, reads via `IdeScopedFileSystemAccess`.
- **`fsWriteTextFile()`** — resolves scope, checks write permission via `PermissionCoordinator`, writes via `IdeScopedFileSystemAccess`.
- **`terminalCreate()` / `terminalOutput()` / `terminalRelease()`** — manage terminal sessions via `TerminalSessionRegistry` and `ShellPaneHost`.

### Structured updates (`StructuredUpdate`)

The transcript uses a sealed hierarchy of `StructuredUpdate` variants:

- `AppendPlainLine(line, isUserPrompt)` — plain text line (user prompts start with `> `).
- `AppendError(message)` — error message.
- `FinalizeAgentStream` — finalizes the active agent stream.
- `AvailableCommands(commands)` — updates prompt input bar commands.
- Tool call variants (tool call start, delta, end).
- Plan variants (plan start, step updates).

### Threading

- All transcript mutations are marshalled onto the EDT (Event Dispatch Thread) via `runOnEdt` callback or `SwingUtilities.invokeLater()`.
- `TranscriptViewController` accepts an optional `runOnEdt` parameter; defaults to `SwingUtilities.invokeLater` when not provided.
- The ACP agent subprocess communicates on IO threads; `ClientSessionOperations` methods are `suspend` functions that marshal results back to the EDT via the listener.

## Agent Synthesis

- When changing transcript behavior, start at `AcpAgentEditor.kt` and trace both paths: prompt events through `AcpPromptEventDispatcher` and out-of-band `notify()` through `AcpClientSessionOperationsImpl`.
- `TranscriptHtmlAppender` keeps `committedBodyHtml` as finalized body state; live chunks accumulate in `streamingPlainText` with an inline cursor (`TranscriptStreamingCursor`).
- Layout split is hard-coded: 72% transcript / 28% bottom, 20% prompt / 80% shell.
- The ACP client is in-process (not JetBrains AI Chat); the agent subprocess is a separate process communicating via stdio JSON-RPC.

## Open Questions

- Should `notify()` route through `AcpPromptEventDispatcher` for consistent finalize semantics? (Currently `notify()` calls `AcpPromptEventDispatcher.dispatchSessionUpdate()` directly.)

## Related

- [Domain context & ACP transcript model](../concepts/context.md)
- [Architecture decisions map](architecture.md)
- [Agent CLI overview](../concepts/agent-cli-overview.md)
- [Worktree subsystem](worktree.md)
