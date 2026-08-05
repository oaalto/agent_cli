# 01 — Executor-only finalize on prompt submit

**Parent:** `prd.md`

**What to build:** User-initiated prompts finalize the agent stream through `AcpPromptExecutor` only. The prompt bar callback in `AcpAgentEditor` echoes the user line and calls `sessionController.prompt`; it no longer invokes `TranscriptFinalizePolicy.onPromptStarting()`. Streaming, tool updates, and error finalize behaviour stay identical to today.

**Blocked by:** None — can start immediately (`worktree-orchestrator-production-wiring` prerequisite is shipped)

**Status:** ready-for-agent

- [ ] `AcpAgentEditor` prompt submit does not call `TranscriptFinalizePolicy.onPromptStarting()`; user echo lines (`> text`) remain in the editor
- [ ] `AcpPromptExecutor` remains the single orchestration owner of `onPromptStarting()` for user prompts; document in a brief comment if the editor error path must still call `onPromptFailed()` for pre-executor failures
- [ ] Existing `AcpEditorLayoutTest`, `AcpSessionControllerTest`, and transcript panel harness tests pass unchanged
- [ ] `./gradlew qualityGate` passes; manual smoke: submit a prompt after a streaming reply and confirm finalize ordering matches prior behaviour
