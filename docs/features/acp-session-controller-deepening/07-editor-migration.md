# 07 — Editor migration to start()

**Parent:** `prd.md`

**What to build:** `AcpAgentEditor` startup simplifies to a single `sessionController.start(AcpSessionStartRequest(...))` call. Build launch plan and editor context as today, inject `EdtSessionPicker`, append `result.statusMessage` to the transcript, persist `result.sessionId` to worktree state, and enable the prompt bar on success. Remove `openSessionFromResumePlan`, `pickSessionOrStartFresh`, `pickSessionFromCandidates`, and `persistCurrentSessionId` from the editor. Keep `persistBoundSessionId` — worktree persistence is an editor concern. `prompt`, `cancelPrompt`, and `dispose` remain one-liners. `sessionControllerFactory` injection point is preserved with the new interface.

**Blocked by:** 06 — Shrink public interface and compose start()

**Status:** done

- [x] `launchAndConnect` calls `start()` once; no direct `connect` / `newSession` / `loadSession` / `listSessions` / `currentSessionId` usage remains in the editor.
- [x] Resume flows (stored session load, picker, start fresh) produce the same transcript messages and session outcomes as before refactoring.
- [x] Worktree session ID is persisted from `result.sessionId` after successful start; `AgentWorktreeStateService` calls stay in the editor.
- [x] Error handling on start failure appends error and disables prompt bar — unchanged UX.
- [x] `./gradlew qualityGate` passes; CHANGELOG updated under `### Changed`.
