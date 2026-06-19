# ACP Client Transcript Output: Implementation Roadmap

**Status:** Active — Step 1 PRD (`docs/prd/acp-transcript-html-rendering.md`); Step 2 PRD (`docs/prd/acp-transcript-tool-status-badges.md`).

**Target:** `AcpAgentEditor` / `AcpSessionControllerImpl` (ACP Client launch mode of the Agent CLI plugin)
**Goal:** Replace the plain-text `JBTextArea` transcript with structured rendering of agent output (text, tool calls, tool results, thinking, etc.)
**No customization support needed** — no extension hooks, no pluggable renderers, no themes.

---

## ACP Update Types (from `SessionUpdate`)

The ACP protocol 0.24 defines these session update types relevant to output rendering:

| ACP `SessionUpdate` subtype | When | Content |
|---|---|---|
| `AgentMessageChunk` | Streaming | Text chunks of the model's response |
| `AgentThoughtChunk` | Streaming | Internal reasoning/rethinking chunks |
| `UserMessageChunk` | Streaming | Echo of user's prompt being streamed |
| `ToolCall` | Start of tool | Title, kind, status, content blocks |
| `ToolCallUpdate` | Tool progress | Same fields, updated status |
| `PlanUpdate` / `PlanUpdateV2` | Planning | Structured step list |
| `UsageUpdate` | Periodic | Token usage, cost |
| `AvailableCommandsUpdate` | Mode/state change | Available slash commands |
| `CurrentModeUpdate` | Mode change | Active session mode ID |
| `ConfigOptionUpdate` | Config change | Session configuration options |

---

## Current State

`AcpAgentEditor` uses a `JEditorPane` in `text/html` mode via `TranscriptHtmlAppender`. `TranscriptUpdateRenderer.render(update)` dispatches on `SessionUpdate` subtype and returns HTML `<span>` fragments:
- `AgentMessageChunk` → light gray monospace span
- `AgentThoughtChunk` → dim gray `[thought]` prefixed span
- `UserMessageChunk` → blue `&gt; ` prefixed span
- `ToolCall` / `ToolCallUpdate` → badge-first line: colored status badge (kind label with ✓/✗ for completed/failed) followed by muted title; `ToolCallUpdate` falls back to `toolCallId` when title is null
- All others → empty list (ignored)

HTML document wrapper declares UTF-8 charset. Tool lines no longer use bracketed `[kind]` syntax or parenthetical `(status)` labels.

---

## Implementation Steps (Easiest → Hardest)

### Step 1: Separate transcript into a structured component (huge payoff, low effort)

**Replace `JBTextArea` with a `JTextPane` or `JEditorPane` (HTML) that supports basic styling.**

This unlocks inline formatting without any rendering logic. Agent output chunks are appended as styled text blocks.

```kotlin
// transcriptArea = JEditorPane("text/html", "<html><body style='font-family: monospace; padding: 8px'></body></html>")
// On each update: append styled spans
fun appendAgentText(text: String) {
    val escaped = escapeHtml(text)
    appendHtml("<span style='color: #d4d4d4'>$escaped</span>")
}
```

**What changes:**
- `TranscriptUpdateRenderer` returns rendered HTML fragments instead of plain `List<String>`
- `AcpSessionListener` gets an `onTranscriptHtml(html: String)` method
- `AcpAgentEditor.appendTranscriptText()` appends HTML instead of plain text

**Input echo styling**: prefix with a distinct color (e.g., `color: #569cd6`)

**Agent text**: neutral foreground, standard monospace

**Why this is the first step:** Zero structural changes, just better visuals. Replaces the single biggest UX deficit — indistinguishable text from different sources.

**Effort:** ~1 day

---

### Step 2: Color-code status badges for tool calls (small effort, clear UX win)

In the HTML rendering, render tool call lines with inline colored badges instead of raw `[kind] title (status)` text.

| Status | Style |
|---|---|
| `PENDING` | dim gray badge |
| `IN_PROGRESS` | yellow/orange animated pulse (or static badge) |
| `COMPLETED` | green checkmark + badge |
| `FAILED` | red X + badge |

```kotlin
fun renderToolCall(title: String, kind: ToolKind?, status: ToolCallStatus?): String {
    val kindLabel = kind?.name?.lowercase()?.replace('_', ' ') ?: "tool"
    val badge = when (status) {
        ToolCallStatus.PENDING -> "<span style='background:#666; color:#fff; padding:0 6px'>$kindLabel</span>"
        ToolCallStatus.IN_PROGRESS -> "<span style='background:#d4a017; color:#fff; padding:0 6px'>$kindLabel ↵</span>"
        ToolCallStatus.COMPLETED -> "<span style='background:#2d8a4e; color:#fff; padding:0 6px'>✓ $kindLabel</span>"
        ToolCallStatus.FAILED -> "<span style='background:#c43c3c; color:#fff; padding:0 6px'>✗ $kindLabel</span>"
        null -> "<span style='background:#666; color:#fff; padding:0 6px'>$kindLabel</span>"
    }
    return "$badge <span style='color:#ccc'>$title</span>"
}
```

**Effort:** half a day

---

### Step 3: Add real-time streaming — inline cursor for agent text (small effort)

Currently, `AgentMessageChunk` events produce one `onTranscriptAppend` per chunk. The `JBTextArea` appends each chunk. With HTML rendering, use a **streaming marker** — a visible cursor at the end of the current agent message block.

```kotlin
private var streamingMarker: String = "<span id='cursor' style='animation: blink 1s step-end infinite'>▊</span>"

fun appendOrUpdateStreamingText(newText: String) {
    // Find the last agent-message block in the document, replace its content,
    // or append a new block if none exists
    appendHtml("<span>${escapeHtml(newText)}$streamingMarker</span>")
}
```

When a non-content update arrives (tool call, thought, usage), finalize the current streaming block (remove the cursor) before rendering the new element.

**Effort:** half a day

---

### Step 4: Render tool output content (moderate effort)

Currently, tool call updates arrive as `ToolCall` / `ToolCallUpdate` but their `content: List<ToolCallContent>` is ignored in `TranscriptUpdateRenderer`. This content carries:

- `ToolCallContent.Content(contentBlock)` — result text, images, resources
- `ToolCallContent.Diff(path, newText, oldText?)` — file diffs
- `ToolCallContent.Terminal(terminalId)` — reference to a terminal widget

**4a — Text result blocks**: When a tool reaches `COMPLETED` or `FAILED`, render its text content as an indented code block below the tool call header.

```kotlin
fun renderToolResult(content: ToolCallContent.Content): String {
    val text = (content.content as? ContentBlock.Text)?.text ?: return ""
    return "<pre style='margin-left:20px; color:#999; border-left:2px solid #444; padding-left:8px'>${escapeHtml(text)}</pre>"
}
```

**4b — Diff rendering**: For `ToolCallContent.Diff`, render with color-coded added/removed lines.

```kotlin
fun renderDiff(diff: ToolCallContent.Diff): String {
    val oldLines = diff.oldText?.lines() ?: emptyList()
    val newLines = diff.newText.lines()
    return buildString {
        append("<pre style='margin-left:20px'>")
        // Simple line-based diff: show removed in red, added in green
        // (A proper diff algorithm would give better results)
        append("<span style='color:#c43c3c'>- ${diff.path}</span>\n")
        append("<span style='color:#2d8a4e'>+ ${diff.path}</span>\n")
        append("</pre>")
    }
}
```

**Note:** This step is where you hit the ceiling of simple HTML rendering. Long diffs and streaming tool output produce large HTML documents that become slow to edit. If performance is acceptable, stop here. If not, proceed to Step 5.

**Effort:** 1–2 days

---

### Step 5: Collapsible tool call cards (moderate effort, best structural improvement)

Replace flat colored lines with expandable/collapsible tool call widgets. This is the point where plain HTML is no longer sufficient — you need Swing components.

**Architecture change:**
- The transcript scroll pane contains a `JPanel` with a `BoxLayout` (vertical)
- Each transcript entry (user echo, agent text block, tool call) is a separate `JComponent`
- Tool calls become collapsible panels (`CollapsibleToolPanel`)

```kotlin
class CollapsibleToolPanel(
    title: String,
    kind: ToolKind?,
    status: ToolCallStatus,
) : JPanel(BorderLayout()) {
    private val header: JPanel  // clickable, shows badge + title
    private val body: JPanel    // contains result text/diff, initially hidden
    
    init {
        header.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                body.isVisible = !body.isVisible
                revalidate()
            }
        })
        add(header, BorderLayout.NORTH)
        add(body, BorderLayout.CENTER)
        body.isVisible = false
    }
}
```

**For this step, you don't need Koog.** Standard Swing `JPanel` + `BoxLayout` + `JSplitPane` handles collapsible panels adequately.

**Effort:** 2–3 days

---

### Step 6: Inline code block syntax highlighting (moderate effort)

When the agent emits `ContentBlock.Text` containing code (fenced or indented), render it with syntax highlighting.

**Without Koog:** Use a `JEditorPane` with a custom syntax-highlighting kit, or use IntelliJ's bundled `LanguageHighlighting` (via `EditorFactory`):

```kotlin
val editor = EditorFactory.getInstance().createEditor(
    document = EditorFactory.getInstance().createDocument(code),
    project = project,
    fileType = guessFileType(language),
)
```

This gives you IntelliJ-level syntax coloring for free, but each editor instance has overhead.

**This step is where Koog becomes useful.** Koog's `CodeBlock` component handles:
- Syntax highlighting with language detection
- Line numbers
- Copy-to-clipboard button
- Dark/light theme adaptation
- Background styling consistent with the transcript

Without Koog: build from `EditorFactory` instances (higher complexity, lower polish).

**Effort:** 2 days without Koog, 0.5 days with Koog

---

### Step 7: Markdown rendering for agent output (moderate effort)

Agent message chunks arrive as plain text but may contain Markdown. Current code just escapes and emits raw text.

**Without Koog:** Parse Markdown → HTML with a library (e.g., `commonmark-java` or IntelliJ's bundled `org.intellij.markdown`), then render the HTML in the `JEditorPane`. This handles:
- **Bold**, *italic*, `` `code` ``
- Lists, headings
- Inline code

But tables, link handling, and image blocks need manual work.

**With Koog:** Koog's `MarkdownBlock` component handles all of this:
- Full CommonMark parsing
- Inline formatting
- Code blocks (syntax highlighted — covers Step 6 too)
- Image rendering
- Table rendering
- Theme-aware (dark/light)

Koog provides a single `addMarkdownContent(markdown: String)` method that does all of the above.

**Effort:** 3 days without Koog (Markdown parser + HTML rendering + edge cases for tables/images), 0.5 days with Koog

---

### Step 8: Footer status bar with token usage and cost (low effort)

`UsageUpdate` events carry token count and cost information. Display a status bar at the bottom of the transcript panel:

```kotlin
class TranscriptFooter : JPanel(FlowLayout(FlowLayout.RIGHT)) {
    private val usageLabel = JLabel()
    private val costLabel = JLabel()
    
    fun updateUsage(used: Long, size: Long, cost: Cost?) {
        usageLabel.text = "$used / $size tokens"
        costLabel.text = cost?.let { "${it.amount} ${it.currency}" } ?: ""
    }
}
```

Compare: pi-agent's `FooterComponent` shows `↑input / ↓output / R cache-read / W cache-write / cost / context %`. The ACP `UsageUpdate` only provides `used` and `size`, so the footer is simpler.

**Effort:** half a day

---

### Step 9: Plan visualization (moderate effort)

`PlanUpdate` contains a structured list of steps. Render them as a numbered checklist with completion states.

```kotlin
class PlanPanel(entries: List<PlanEntry>) : JPanel() {
    // Each entry becomes a row:
    // [✓] Step description (completed)
    // [→] Step description (in progress)
    // [ ] Step description (pending)
}
```

**Without Koog:** Build with standard Swing labels and icons. Simple but functional.

**Effort:** 1 day

---

### Summary

| Step | Description | Koog needed? | Effort |
|---|---|---|---|
| 1 | HTML `JEditorPane` transcript (color-coded sources) | No | 1d |
| 2 | Color-coded tool status badges | No | 0.5d |
| 3 | Streaming cursor indicator | No | 0.5d |
| 4 | Tool output content (text results, diffs) | No | 1–2d |
| 5 | Collapsible tool call cards | No | 2–3d |
| 6 | Syntax-highlighted code blocks | **Useful** | 2d (no) / 0.5d (Koog) |
| 7 | Markdown rendering | **Useful** | 3d (no) / 0.5d (Koog) |
| 8 | Footer status bar (tokens/cost) | No | 0.5d |
| 9 | Plan visualization | No | 1d |

---

## When Koog Becomes Worth It

**Koog** is a Kotlin UI component library for IntelliJ plugins that provides pre-built components: Markdown rendering, code blocks with syntax highlighting, image blocks, and theme-aware styling.

Koog is **not needed** for Steps 1–5 and 8–9. These can all be implemented with standard IntelliJ/Swing APIs:

- `JEditorPane` (text/html) — for colored text
- `JPanel` + `BoxLayout` — for collapsible tool cards
- `JLabel` + icons — for status badges
- `EditorFactory` — for syntax highlighting (heavier, needs `Document` management)
- `org.intellij.markdown` — for Markdown → HTML parsing (bundled in IntelliJ Platform)

**Koog becomes useful at Step 6 (code blocks) and Step 7 (Markdown).** The value proposition:

| Concern | Without Koog | With Koog |
|---|---|---|
| Code syntax highlighting | `EditorFactory.createEditor()` — one editor instance per code block, memory overhead, need to manage lifecycle | `CodeBlock` — lightweight, managed theme |
| Markdown parsing | `org.intellij.markdown` or external lib, must handle all edge cases | Built-in, handles tables/images |
| Theme adaptation | Manual `JBColor` wiring for every element | Automatic dark/light |
| Lines of code to render agent output | ~400–600 (Markdown + highlighting + images) | ~50 (`addMarkdownContent`) |

**Decision rule:** Add Koog as a dependency when implementing Step 6 or Step 7, whichever comes first in your schedule. Steps 1–5 are independent of Koog and can be done in parallel or before introducing it.

---

## Architectural Notes

### Current event flow
```
ACP stdio → Protocol → Client → ClientSession.prompt()
  → Event.SessionUpdateEvent → handlePromptEvent()
    → TranscriptUpdateRenderer.render(update) → List<String>
      → AcpAgentEditor.appendTranscriptLine(line)
```

### Target event flow (with structure)
```
ACP stdio → Protocol → Client → ClientSession.prompt()
  → Event.SessionUpdateEvent → handlePromptEvent()
    → TranscriptUpdateRenderer.renderStructured(update) → StructuredUpdate
      → TranscriptModel.apply(structure)
        → TranscriptPanel.rebuild()  // incremental
```

### TranscriptModel — tracking state

The current rendering is stateless (each update is rendered independently). To support collapsible tool panels and streaming cursor, the transcript needs state:

```kotlin
class TranscriptModel {
    private val blocks = mutableListOf<TranscriptBlock>()
    
    fun apply(update: StructuredUpdate) {
        when (update) {
            is StructuredUpdate.AgentText -> {
                blocks.lastOrNull { it is TranscriptBlock.StreamingText }?.let { streaming ->
                    (streaming as TranscriptBlock.StreamingText).append(update.text)
                } ?: blocks.add(TranscriptBlock.StreamingText(update.text))
            }
            is StructuredUpdate.ToolCallUpdate -> {
                val index = blocks.indexOfLast { it is TranscriptBlock.ToolCall && it.id == update.toolCallId }
                if (index >= 0) (blocks[index] as TranscriptBlock.ToolCall).updateStatus(update.status)
                else blocks.add(TranscriptBlock.ToolCall(update.toolCallId, update.title, update.kind, update.status))
            }
            is StructuredUpdate.ToolResult -> {
                val index = blocks.indexOfLast { it is TranscriptBlock.ToolCall && it.id == update.toolCallId }
                if (index >= 0) (blocks[index] as TranscriptBlock.ToolCall).setResult(update.content)
            }
            // ...
        }
    }
}

sealed class TranscriptBlock {
    class UserEcho(val text: String) : TranscriptBlock()
    class StreamingText(val buffer: StringBuilder) : TranscriptBlock()
    class FinalText(val text: String) : TranscriptBlock()
    class Thought(val text: String) : TranscriptBlock()
    class ToolCall(val id: String, val title: String, val kind: ToolKind?, var status: ToolCallStatus) : TranscriptBlock()
    class ToolResult(val id: String, val content: List<ToolCallContent>) : TranscriptBlock()
    class Plan(val entries: List<PlanEntry>) : TranscriptBlock()
    class Usage(val used: Long, val size: Long, val cost: Cost?) : TranscriptBlock()
}
```

---

## Summary of Key Changes

1. **`TranscriptUpdateRenderer`** graduates from a flat `List<String>` generator to a `StructuredUpdate` producer with state tracking
2. **`AcpSessionListener`** gains structured methods or accepts the `StructuredUpdate` type, replacing `onTranscriptAppend`/`onTranscriptLine`
3. **`AcpAgentEditor`** replaces the single `JBTextArea` with a composite panel (`TranscriptPanel`) containing collapsible cards, streaming agent text blocks, and a footer
4. **`CollapsibleToolPanel`** handles tool call lifecycle (pending → in_progress → completed/failed) with expandable body for results
5. **Koog** is added at Step 6 or 7 for Markdown rendering and syntax-highlighted code blocks
