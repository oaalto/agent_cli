# How pi-agent Handles Displaying User Input and Model Output

**Date:** 2026-06-19
**Source:** `@earendil-works/pi-coding-agent` (npm), pi-agent-core, pi-tui
**Type:** Reverse-engineering report

---

## 1. Overview

pi-agent runs in four modes (interactive/TUI, print, JSON, RPC). This report focuses on **interactive (TUI) mode**, which is the primary mode for human-in-the-loop use. The display pipeline can be decomposed into three concerns:

| Concern | Component | Package |
|---|---|---|
| Input editor | `Editor` (TUI) | `@earendil-works/pi-tui` |
| Message rendering | `UserMessageComponent`, `AssistantMessageComponent`, `ToolExecutionComponent`, etc. | pi-coding-agent (interactive mode) |
| Agent event stream | `agent-loop` → `AgentSession` → `InteractiveMode.handleEvent` | pi-agent-core, pi-coding-agent |

---

## 2. User Input Display

### 2.1 The Editor

User input is handled by the **`Editor`** component from `@earendil-works/pi-tui`. It is a full-featured terminal text editor with:

- **Multi-line editing** with word-wrap (visual lines, not logical lines)
- **Cursor management** with sticky-column logic for vertical movement
- **Autocomplete** for `@`-file references, `/commands`, and extension-provided completions
- **Undo** via a snapshot stack
- **Kill ring** (Emacs-style cut/copy/paste ring)
- **Paste detection** for bracketed paste mode
- **History** for navigating previous prompts with up/down arrows
- **Tab-to-space** normalization
- **External editor** support via `Ctrl+G` (uses `$VISUAL` or `$EDITOR`)
- **Image paste support** (`Ctrl+V` / `Alt+V` on Windows, drag-and-drop)

The editor instance is owned by `InteractiveMode`. A factory pattern via `ctx.ui.setEditorComponent()` allows extensions to replace the built-in editor with a custom one (e.g., a Vim-mode editor, see `examples/extensions/modal-editor.ts`). Custom editors extend `CustomEditor`, which inherits from the built-in `Editor` and adds app-level keybindings (escape to abort, model switching, etc.).

### 2.2 Editor Border

The editor's border color is updated dynamically to reflect the current **thinking level**:

| Thinking Level | Token |
|---|---|
| off | `thinkingOff` |
| minimal | `thinkingMinimal` |
| low | `thinkingLow` |
| medium | `thinkingMedium` |
| high | `thinkingHigh` |
| xhigh | `thinkingXhigh` |
| bash mode (`!`) | `bashMode` |

When the user types `!` at the start of a line, the editor switches to bash mode (border turns `bashMode` color, and the line is executed as a bash command instead of sent to the LLM).

### 2.3 Message Queue Display

While the agent is streaming, the user can queue messages:

- **Steering messages** (Enter while streaming) — delivered after the current assistant turn finishes its tool calls but before the next LLM call
- **Follow-up messages** (`Alt+Enter` while streaming) — delivered only after the agent finishes all work

A **`pendingMessagesContainer`** widget above the editor shows queued messages. Users can retrieve queued messages back to the editor with `Alt+Up`. The count and content of pending messages are displayed via `updatePendingMessagesDisplay()`.

---

## 3. Model Output Display

### 3.1 Event Pipeline

The agent output flows through a multi-stage pipeline:

```
LLM provider → agent-loop (agent-core)
  → emits AgentEvent (message_start, message_update, message_end, etc.)
  → AgentSession._handleAgentEvent
  → AgentSessionEventListener (InteractiveMode.handleEvent)
  → TUI rendering components
```

### 3.2 Assistant Message Rendering

**`AssistantMessageComponent`** (`dist/modes/interactive/components/assistant-message.ts`) renders the model's text response.

- Uses **`Markdown`** (from pi-tui) for rendering, which provides:
  - Syntax-highlighted code blocks (via highlight.js)
  - Heading, link, list, table, blockquote, horizontal-rule formatting
  - Bold, italic, strikethrough, underline inline styles
- A **thinking block** can be shown or hidden (toggled via `Ctrl+T`). When hidden, a compact label like `[thinking...]` is shown instead (configurable via `hiddenThinkingLabel`).
- Message content is **streamed in real time**: on each `message_update` event, `updateContent(message)` is called, which re-renders the Markdown component with the latest partial text.

```typescript
// InteractiveMode.handleEvent — message_start
case "message_start": {
  if (event.message.role === "assistant") {
    this.streamingComponent = new AssistantMessageComponent(
      undefined, // no initial message
      this.hideThinkingBlock,
      this.getMarkdownThemeWithSettings(),
      this.hiddenThinkingLabel
    );
    this.streamingMessage = event.message;
    this.chatContainer.addChild(this.streamingComponent);
    this.streamingComponent.updateContent(this.streamingMessage);
  }
}

// InteractiveMode.handleEvent — message_update
case "message_update": {
  if (this.streamingComponent && event.message.role === "assistant") {
    this.streamingMessage = event.message;
    this.streamingComponent.updateContent(this.streamingMessage);
  }
}
```

### 3.3 Tool Call and Execution Display

**`ToolExecutionComponent`** (`dist/modes/interactive/components/tool-execution.ts`) renders tool calls made by the model and their results.

Key features:
- **Collapsible** via `Ctrl+O` — toggles between expanded (shows full output) and collapsed (shows a summary)
- **Rendering modes**:
  - **Fallback**: plain text display with header, args, and output/error
  - **Custom renderers**: tools can register `renderCall` and `renderResult` functions for custom UI (e.g., the `edit` built-in tool renders diffs with color-coded added/removed lines)
- **Theme support**: backgrounds change color based on tool state:
  - `toolPendingBg` — while executing
  - `toolSuccessBg` — completed successfully
  - `toolErrorBg` — completed with error
- **Image output support**: tools can produce image content (base64 + MIME type), which is rendered inline using the Kitty image protocol or iTerm2 escape sequences
- **Streaming updates**: `updateResult(result, isPartial)` allows progress updates during long-running tool calls (e.g., bash command streaming output)

### 3.4 Bash Execution Display

**`BashExecutionComponent`** (`dist/modes/interactive/components/bash-execution.ts`) is a specialized component for `!`-prefixed bash commands executed directly by the user. Features:
- **Streaming output**: `appendOutput(chunk)` appends text as it arrives from the subprocess
- **Status indicator**: a `Loader` spinner while the command is running
- **Exit code display**: shows success (green) or failure (red) with the exit code
- **Collapsible** output
- **Truncation UI**: when output is truncated, shows a note with the path to the full output file

### 3.5 Other Message Types

| Message Type | Component | Display |
|---|---|---|
| User message | `UserMessageComponent` | Markdown-rendered text in a `userMessageBg` box |
| Custom (extension) | `CustomMessageComponent` | Distinct styling, `customMessageBg` background, `customMessageLabel` header. Extensions can provide custom `renderCall`/`renderResult` functions. |
| Compaction summary | `CompactionSummaryMessageComponent` | Box with `customMessageBg`, collapsed/expanded state |
| Branch summary | `BranchSummaryMessageComponent` | Box with `customMessageBg`, collapsed/expanded state |
| Skill invocation | `SkillInvocationMessageComponent` | Box with `customMessageBg`, shows skill name and content |

All message components are children of a `Container` (`chatContainer`) inside the TUI. When a session is loaded or compacted, `renderSessionContext()` rebuilds the visible message list by iterating all entries on the active branch.

---

## 4. The Footer

The **`FooterComponent`** (`dist/modes/interactive/components/footer.ts`) displays a status bar at the bottom of the terminal:

| Field | Description |
|---|---|
| Working directory | Shortened path |
| Session name | From `--name` or `/name` |
| Token stats | ↑input / ↓output / R cache-read / W cache-write |
| Cost | Total cost in dollars |
| Context usage | Percentage of context window used |
| Model | Current model ID |
| Git branch | From `FooterDataProvider` |
| Extension statuses | Custom status strings from extensions |

Extensions can replace the footer entirely via `ctx.ui.setFooter()`.

---

## 5. The Working Indicator

While the agent is streaming, pi shows an **animated spinner** (configurable via `ctx.ui.setWorkingIndicator()`):

- Default is a simple frame-based animation rendered inline
- Extensions can customize frames (any strings, with theme colors) and interval
- Can be hidden entirely with `setWorkingIndicator({ frames: [] })`

---

## 6. Customizable Rendering Points

Extensions have five hooks into the rendering pipeline:

1. **`renderCall(args, theme, context)`** on a tool definition — custom rendering of tool call arguments (e.g., showing a diff preview)
2. **`renderResult(result, options, theme, context)`** on a tool definition — custom rendering of tool results
3. **`message_end` event** — can replace the finalized message metadata (but not role or content shape)
4. **`tool_result` event** — can modify tool result content before it is rendered
5. **`message_update` event** — can observe streaming tokens

Additionally, extensions can:
- Replace the **editor** entirely (via `setEditorComponent`)
- Replace the **footer** (via `setFooter`)
- Add **widgets** above/below the editor (via `setWidget`)
- Show arbitrary **TUI overlays** (via `ctx.ui.custom()`)
- Set **status text** in the footer (via `setStatus`)

---

## 7. Summary

| Aspect | Mechanism |
|---|---|
| User input collection | `Editor` component (pi-tui) with autocomplete, paste, multi-line, undo |
| Input submission | Enter triggers `onSubmit`; bash mode on `!` prefix |
| Input queuing | Steering (Enter) / follow-up (Alt+Enter) while streaming |
| Model output rendering | `AssistantMessageComponent` with Markdown (pi-tui) |
| Streaming | `message_update` events → `updateContent()` on the live component |
| Tool output | `ToolExecutionComponent` with collapsible sections, custom renderers |
| Bash output | `BashExecutionComponent` with streaming, spinner, exit codes |
| Compaction/Summaries | Collapsible boxes with `customMessageBg` |
| Status line | `FooterComponent` with tokens, cost, model, git branch |
| Working indicator | Animated spinner, extensible |
| Extension UI hooks | `renderCall`/`renderResult`, `setEditorComponent`, `setFooter`, `setWidget`, `setStatus`, `ctx.ui.custom()` |
