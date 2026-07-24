---
title: Domain context & ACP transcript model
type: concept
status: current
updated: 2026-07-24
sources:
  - CONTEXT.md
  - docs/wiki/subsystems/acp-client.md
---

# Domain context & ACP transcript model

## Summary

`CONTEXT.md` is the repo's **domain glossary** and navigation layer for agents. Implementation detail for the ACP transcript stack lives in [ACP client subsystem](../subsystems/acp-client.md) and `agent/acp/` source — start at [`AcpAgentEditor.kt`](../../../src/main/kotlin/com/oaalto/agent/acp/AcpAgentEditor.kt) for transcript behavior changes.

## Verified Facts

- `CONTEXT.md` is glossary + pointers only (trimmed from a prior code dump); durable implementation detail belongs in wiki pages and source.
- `AcpAgentEditor` is the UI entry point: it owns `TranscriptHtmlAppender`, implements `AcpSessionListener`, and wires `AcpEditorLayout` (transcript column + prompt/shell split).
- `AcpPromptEventDispatcher` routes prompt-scoped `SessionUpdate` events; it finalizes the active agent stream before non-chunk updates.
- `TranscriptSessionUpdateMapper` maps ACP `SessionUpdate` events (chunks, tool calls, usage, plans, available commands) to `StructuredUpdate` values.
- `TranscriptColorProvider` supplies theme-aware colors for HTML and Swing transcript components.
- `TranscriptFooter` shows cumulative token usage and optional cost; turns orange above 80% context usage.
- `PlanPanel` / `PlanPanelRenderer` render plan checklists in the transcript with status icons and priority styling.
- `PromptInputBar` provides slash-command autocomplete from `AvailableCommandsUpdate` events.
- `TranscriptMarkdownRenderer` parses agent text via IntelliJ's GFM Markdown AST.
- `TranscriptHtmlAppender` keeps `committedBodyHtml` as finalized body state; live chunks accumulate in `streamingPlainText` with an inline cursor (`TranscriptStreamingCursor`).
- Layout split: ~72% transcript column (scroll + auth north + permission south) / ~28% bottom (20% prompt input / 80% shell) per `AcpEditorLayout.buildRootPanel`.
- Streaming finalize triggers include: non-chunk `SessionUpdate`, `PromptResponseEvent`, user prompt send, cancel/dispose/errors, and `onTranscriptHtml` / `onTranscriptPlainLine` / `onError` listener paths.

## Agent Synthesis

- When changing transcript behavior, start at `AcpAgentEditor.kt` and trace both paths: prompt events through `AcpPromptEventDispatcher` and out-of-band `notify()` through `AcpClientSessionOperationsImpl`.
- `CONTEXT.md` is glossary + pointers; durable implementation detail belongs in this page, subsystem wiki pages, and source.

## Open Questions

- Should `notify()` route through `AcpPromptEventDispatcher` for consistent finalize semantics?

## Related

- [ACP client subsystem](../subsystems/acp-client.md)
- [Architecture decisions map](../subsystems/architecture.md)
- [Agent CLI overview](agent-cli-overview.md)
- [Worktree subsystem](../subsystems/worktree.md)
