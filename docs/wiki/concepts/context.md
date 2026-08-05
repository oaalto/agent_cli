---
title: Domain context & ACP transcript model
type: concept
status: current
updated: 2026-08-05
sources:
  - CONTEXT.md
  - docs/adr/0005-transcript-row-adapter-registry.md
  - docs/adr/0006-transcript-simple-vs-agent-row-shells.md
  - docs/adr/0007-transcript-finalize-policy-orchestration-layer.md
  - docs/wiki/subsystems/acp-client.md
---

# Domain context & ACP transcript model

## Summary

`CONTEXT.md` is the repo's **domain glossary** and navigation layer for agents. Implementation detail for the ACP transcript stack lives in [ACP client subsystem](../subsystems/acp-client.md) and `agent/acp/transcript/` source (`model/`, `render/`, `view/`, `theme/`) — start at [`AcpAgentEditor.kt`](../../../src/main/kotlin/com/oaalto/agent/acp/AcpAgentEditor.kt) for transcript behavior changes.

## Verified Facts

- `CONTEXT.md` is glossary + pointers only (trimmed from a prior code dump); durable implementation detail belongs in wiki pages and source.
- `AcpAgentEditor` is the UI entry point: it owns `TranscriptViewController` and `TranscriptModel`, implements `AcpSessionListener`, and wires `AcpEditorLayout` (transcript column + prompt/shell split).
- `TranscriptEventIngestion` (`transcript/model/`) routes prompt-scoped `SessionUpdate` events and maps them to `StructuredUpdate` values; finalize prelude delegates to `TranscriptFinalizePolicy`. May import `transcript/render/` for tool `bodyParts` mapping only.
- `TranscriptFinalizePolicy` is the canonical source for when `FinalizeAgentStream` is emitted (non-chunk session updates, prompt start/complete/fail/interrupt). See [ADR 0007](../../../docs/adr/0007-transcript-finalize-policy-orchestration-layer.md).
- `TranscriptColorProvider` (`transcript/theme/`) supplies theme-aware colors for HTML and Swing transcript components.
- `TranscriptFooter` shows cumulative token usage and optional cost; turns orange above 80% context usage.
- `PlanPanel` (via `PlanRowAdapter`) renders plan checklists in the transcript with status icons and priority styling.
- `PromptInputBar` provides slash-command autocomplete from `AvailableCommandsUpdate` events.
- `TranscriptContentRenderer.renderMarkdownText` (`transcript/render/`) is the canonical seam: markdown/plain agent or tool text → `List<TranscriptBodyPart>`. `TranscriptMarkdownRenderer` (GFM AST walk → internal `RenderedBlock` shapes) is an implementation detail behind that module.

- Layout split: ~72% transcript column (scroll + auth north + permission south) / ~28% bottom (20% prompt input / 80% shell) per `AcpEditorLayout.buildRootPanel`.
- Streaming finalize triggers are owned by `TranscriptFinalizePolicy` — see [ACP client subsystem](../subsystems/acp-client.md#finalize-policy-transcriptfinalizepolicy).

## Agent Synthesis

- When changing transcript behavior, start at `AcpAgentEditor.kt` and trace: ACP events enter via `TranscriptEventIngestion` (`transcript/model/`, finalize prelude from `TranscriptFinalizePolicy`, or out-of-band `notify()` via `AcpClientSessionOperationsImpl`), map to `StructuredUpdate`, then flow through `TranscriptViewController` → `TranscriptModel` → `TranscriptPanel` (`transcript/view/`, sync + `blockId` reuse) → `TranscriptBlockViewFactory` → row adapter (`transcript/view/rows/`). Text bodies route through `TranscriptContentRenderer` (`transcript/render/`) before widget mapping (not direct `TranscriptMarkdownRenderer` / `TranscriptBlockConverter` calls).
- `CONTEXT.md` is glossary + pointers; durable implementation detail belongs in this page, subsystem wiki pages, and source.

## Related

- [ACP client subsystem](../subsystems/acp-client.md)
- [Architecture decisions map](../subsystems/architecture.md)
- [Agent CLI overview](agent-cli-overview.md)
- [Worktree subsystem](../subsystems/worktree.md)
