# 05 — Worktree resume context aligned with kernel resolver

**Parent:** `prd.md`

**What to build:** `WorktreeLaunchCoordinator` replaces `resolveWorkingDirectory`, `resolveWslPaths`, and `resolveExecutionTarget` with kernel `AgentLaunchResolver` calls inside `buildResumeContext`. `ResumeContext` WSL fields (`wslWorkingDirectory`, `wslDistribution`, `hostWorkingDirectory`) match what PTY and ACP launch adapters use for the same configuration. `PtyResumeStrategy` stops parsing execution target independently — it consumes the resolved target from `ResumeContext` or the shared parser from ticket 01. The silent `null` WSL failure in `resolveWslPaths` is replaced by explicit propagation of the kernel `Result.failure` contract.

**Blocked by:** 02 — Kernel `AgentLaunchResolver` with unit tests

**Status:** ready-for-agent

- [ ] `buildResumeContext` calls `resolveLaunchInputs` with worktree path as the working-directory override
- [ ] WSL `ResumeContext` fields are populated from `ResolvedLaunchInputs.Wsl`; local resume uses `ResolvedLaunchInputs.Local` working directory
- [ ] Mapping failures no longer silently return `null` — coordinator propagates or maps kernel failures explicitly (document the choice in code)
- [ ] `PtyResumeStrategy` deletes its private execution-target parser; resume probing receives consistent WSL path inputs matching editor launch paths
- [ ] `WorktreeLaunchCoordinatorTest` verifies `ResumeContext` WSL fields match kernel output for WSL configurations
- [ ] `CHANGELOG.md` updated under `### Changed`; `docs/wiki/log.md` receives a `skip` or `update` entry per PRD notes
- [ ] `./gradlew qualityGate` passes
