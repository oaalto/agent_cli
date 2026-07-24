# 03 — AcpSessionResumeOrchestrator with full decision tree and unit tests

**Parent:** `prd.md`

**What to build:** A deep **worktree/resume** module that owns all ACP session-open execution after connect: interpret `LaunchResumePlan`, run load/list/pick/new-session branches, enforce the worktree binding invariant, and return structured results. Extract the decision tree currently embedded in `AcpAgentEditor` without wiring the editor yet — the orchestrator is the test surface.

**Blocked by:** 02 — Orchestrator ports, open result, and seam adapters

**Status:** ready-for-agent

- [ ] `AcpSessionResumeOrchestrator.openSession(plan, sessionWorkingDirectory, worktreeRecordId)` implements the full branch tree: `AcpLoad` success, `AcpLoad` failure → list + pick, `AcpResolveSession`, `AcpNewSession` / null, and rejection of `LaunchResumePlan.Pty` with a clear error
- [ ] After any successful session open (load or new), when `worktreeRecordId` is non-null, `WorktreeSessionBinder.persistSessionId` is called with `currentSessionId()`; when `worktreeRecordId` is null, no persistence occurs
- [ ] Fallback paths match current behavior: empty session list → `newSession` + persist; picker returns null → `newSession` + persist; picker selection load fails → `newSession` + persist
- [ ] `AcpSessionResumeOrchestratorTest` uses fake ports (no Swing, no live ACP process) and covers all branches listed in the PRD Testing Decisions section
- [ ] Existing tests (`AcpResumeStrategyTest`, `AgentWorktreeStateServiceTest`, `WorktreeLaunchCoordinatorTest`, `AcpSessionControllerTest`) remain green; `./gradlew qualityGate` passes
