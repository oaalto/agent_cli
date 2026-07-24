# 02 — Orchestrator ports, open result, and seam adapters

**Parent:** `prd.md`

**What to build:** Introduce the port interfaces and structured result type that `AcpSessionResumeOrchestrator` will depend on, plus thin default adapters at the **ACP Client** and **Worktree** slice seams. The **worktree/resume** slice owns the port contracts; **acp/** and **worktree/** implement them without new cross-slice imports beyond the existing `LaunchResumePlan` direction.

**Blocked by:** 01 — Prefactor LaunchResumePlan ACP resolve variant

**Status:** ready-for-agent

- [ ] Port interfaces exist in **worktree/resume/**: `AcpSessionOperations` (`newSession`, `loadSession`, `listSessions`, `currentSessionId`), `WorktreeSessionBinder` (`persistSessionId`), `SessionPicker` (`pickSession` returning session ID or null for start-fresh)
- [ ] `AcpSessionOpenResult` sealed type captures opened session ID, whether the picker was shown, and user-visible status events or fallback reasons for **Transcript** mapping
- [ ] Optional `ResumeNotifier` port (or equivalent events on the result) allows tests to run without Swing
- [ ] `AcpSessionOperationsAdapter` in **acp/** delegates to `AcpSessionController` without changing protocol behavior
- [ ] `WorktreeSessionBinderImpl` in **worktree/** delegates to `AgentWorktreeStateService.setAcpSessionId`
- [ ] `SessionPickerAdapter` in **acp/ui/** delegates to `SessionPickerDialog` on the EDT
- [ ] **worktree/** does not import **acp/** types; **acp/** may depend on **worktree/resume/** port interfaces only
- [ ] Project compiles; existing tests still pass
