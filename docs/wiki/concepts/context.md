---
title: Domain context & ACP transcript model
type: concept
status: current
updated: 2026-08-04
sources:
  - CONTEXT.md
  - docs/adr/0005-transcript-row-adapter-registry.md
  - docs/adr/0006-transcript-simple-vs-agent-row-shells.md
  - docs/wiki/subsystems/acp-client.md
---

# Domain context & ACP transcript model

## Summary

`CONTEXT.md` is the repo's **domain glossary** and navigation layer for agents. Implementation detail for the ACP transcript stack lives in [ACP client subsystem](../subsystems/acp-client.md) and `agent/acp/` source — start at [`AcpAgentEditor.kt`](../../../src/main/kotlin/com/oaalto/agent/acp/AcpAgentEditor.kt) for transcript behavior changes.

## Verified Facts

- `CONTEXT.md` is glossary + pointers only (trimmed from a prior code dump); durable implementation detail belongs in wiki pages and source.
- `AcpAgentEditor` is the UI entry point: it owns `TranscriptViewController` and `TranscriptModel`, implements `AcpSessionListener`, and wires `AcpEditorLayout` (transcript column + prompt/shell split).
- `TranscriptEventIngestion` routes prompt-scoped `SessionUpdate` events and maps them to `StructuredUpdate` values; it finalizes the active agent stream before non-chunk updates.
- `TranscriptColorProvider` supplies theme-aware colors for HTML and Swing transcript components.
- `TranscriptFooter` shows cumulative token usage and optional cost; turns orange above 80% context usage.
- `PlanPanel` / `PlanPanelRenderer` render plan checklists in the transcript with status icons and priority styling.
- `PromptInputBar` provides slash-command autocomplete from `AvailableCommandsUpdate` events.
- `TranscriptContentRenderer.renderMarkdownText` is the canonical seam: markdown/plain agent or tool text → `List<TranscriptBodyPart>`. `TranscriptMarkdownRenderer` (GFM AST walk → internal `RenderedBlock` shapes) is an implementation detail behind that module.

- Layout split: ~72% transcript column (scroll + auth north + permission south) / ~28% bottom (20% prompt input / 80% shell) per `AcpEditorLayout.buildRootPanel`.
- Streaming finalize triggers include: non-chunk `SessionUpdate`, `PromptResponseEvent`, user prompt send, cancel/dispose/errors, and `onError` listener path.

## Agent Synthesis

- When changing transcript behavior, start at `AcpAgentEditor.kt` and trace: ACP events enter via `TranscriptEventIngestion` (or out-of-band `notify()` via `AcpClientSessionOperationsImpl`), map to `StructuredUpdate`, then flow through `TranscriptViewController` → `TranscriptModel` → `TranscriptPanel` (sync + `blockId` reuse) → `TranscriptBlockViewFactory` (adapter dispatch) → row adapter. Text bodies route through `TranscriptContentRenderer` before widget mapping (not direct `TranscriptMarkdownRenderer` / `TranscriptBlockConverter` calls).
- `CONTEXT.md` is glossary + pointers; durable implementation detail belongs in this page, subsystem wiki pages, and source.

## Open Questions

- (Consolidation closed this: `TranscriptEventIngestion` absorbs both routing paths into one seam.)

## Related

- [ACP client subsystem](../subsystems/acp-client.md)
- [Architecture decisions map](../subsystems/architecture.md)
- [Agent CLI overview](agent-cli-overview.md)
- [Worktree subsystem](../subsystems/worktree.md)
