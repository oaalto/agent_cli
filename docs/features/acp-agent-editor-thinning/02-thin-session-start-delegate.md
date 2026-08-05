# 02 — Thin session start delegate

**Parent:** `prd.md`

**What to build:** Session open in `AcpAgentEditor` is a thin delegate to `AcpSessionController.start`: build `AcpLaunchPlan` and `AcpSessionStartRequest` (including `worktreeRecordId`, `resumePlan`, `sessionPicker`, `editorContext`), call `start` once, map `AcpSessionStartResult` to transcript status and prompt-bar enablement. Collapse or inline `startSession` / `launchAndConnect` / `applyStartResult` so the editor reads as layout + listener routing + start wiring — no resume orchestration or worktree persistence logic in the file.

**Blocked by:** None — can start immediately (`worktree-orchestrator-production-wiring` prerequisite is shipped)

**Status:** ready-for-agent

- [ ] Editor startup calls `sessionController.start(AcpSessionStartRequest(...))` once; no private resume/persist helpers remain
- [ ] `applyStartResult` behaviour is preserved: transcript restore when `restoreTranscript`, `sessionTranscript.bindSession`, status message append, "Connected to …" line, prompt bar enabled and focused
- [ ] `buildEditorContext` populates scope, permission/auth UI, and shell host on the request; worktree session binding stays in the lifecycle/orchestrator stack via `worktreeRecordId`
- [ ] `./gradlew qualityGate` passes; manual smoke: open ACP editor, resume a worktree session, confirm transcript restore and status messages match prior UX
