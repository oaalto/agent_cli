# 04 — ACP editor delegates session open to orchestrator

**Parent:** `prd.md`

**What to build:** Make `AcpAgentEditor` a thin **ACP Client** adapter: after `connect`, delegate session open to `AcpSessionResumeOrchestrator` and map `AcpSessionOpenResult` to existing **Transcript** messages. Remove resume orchestration and direct **Worktree** persistence calls from the editor so all load/list/pick/persist logic lives in one deep module.

**Blocked by:** 03 — AcpSessionResumeOrchestrator with full decision tree and unit tests

**Status:** ready-for-agent

- [ ] `AcpAgentEditor.launchAndConnect` calls `orchestrator.openSession(file.launchContext.resumePlan, sessionWorkingDirectory, file.launchContext.worktreeId)` after `sessionController.connect`
- [ ] Private methods `openSessionFromResumePlan`, `pickSessionOrStartFresh`, `pickSessionFromCandidates`, `persistCurrentSessionId`, and `persistBoundSessionId` are deleted from the editor
- [ ] The editor no longer imports or calls `AgentWorktreeStateService` directly for session binding
- [ ] Orchestrator is injectable via constructor (with sensible defaults for production) so tests can substitute a fake
- [ ] **Transcript** lines for load failure, picker prompt, started fresh, and resumed session match prior user-visible copy (no UX change)
- [ ] Manual verification: resuming a **Worktree** with stored `acpSessionId` in **ACP Client** **Launch Mode** still loads the session; stale ID shows picker; non-worktree launch (`worktreeId == null`) opens without persistence errors
- [ ] `CHANGELOG.md` updated under `### Changed`; `./gradlew qualityGate` passes
