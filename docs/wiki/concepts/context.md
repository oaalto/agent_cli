---
title: Domain context & ACP transcript model
type: concept
status: draft
updated: 2026-06-24
sources:
  - CONTEXT.md
---

# Domain context & ACP transcript model

## Summary

`CONTEXT.md` is the repo's domain glossary and navigation layer for agents. Implementation detail for the ACP transcript stack lives in wiki pages and `agent/acp/` source — start at [`AcpAgentEditor.kt`](../../../src/main/kotlin/com/oaalto/agent/acp/AcpAgentEditor.kt) for transcript behavior changes.

## Verified Facts

- `CONTEXT.md` anchors agent work on the ACP transcript subsystem under `src/main/kotlin/com/oaalto/agent/acp/`.
- `AcpAgentEditor` is the UI entry point: it owns `TranscriptHtmlAppender`, implements `AcpSessionListener`, and wires `AcpEditorLayout` (transcript column + prompt/shell split).
- `AcpPromptEventDispatcher` routes prompt-scoped `SessionUpdate` events; it finalizes the active agent stream before non-chunk updates.
- `AcpClientSessionOperationsImpl.notify()` renders agent-initiated updates via `TranscriptRenderer` → `onTranscriptHtml`; the editor listener finalizes streams before appending HTML.
- `TranscriptHtmlAppender` keeps `committedBodyHtml` as finalized body state; live chunks accumulate in `streamingPlainText` with an inline cursor (`TranscriptStreamingCursor`).
- Layout split: ~72% transcript column (scroll + auth north + permission south) / ~28% bottom (20% prompt input / 80% shell) per `AcpEditorLayout.buildRootPanel`.
- Streaming finalize triggers include: non-chunk `SessionUpdate`, `PromptResponseEvent`, user prompt send, cancel/dispose/errors, and `onTranscriptHtml` / `onTranscriptPlainLine` / `onError` listener paths.

## Agent Synthesis

- When changing transcript behavior, start at `AcpAgentEditor.kt` and trace both paths: prompt events through `AcpPromptEventDispatcher` and out-of-band `notify()` through `AcpClientSessionOperationsImpl`.
- `CONTEXT.md` is glossary + pointers; durable implementation detail belongs in this page, subsystem wiki pages, and source.

## Open Questions

- Should `notify()` route through `AcpPromptEventDispatcher` for consistent finalize semantics?

## Related

- [Architecture decisions map](../subsystems/architecture.md)
- [Agent CLI overview](agent-cli-overview.md)
- [Worktree subsystem](../subsystems/worktree.md)
