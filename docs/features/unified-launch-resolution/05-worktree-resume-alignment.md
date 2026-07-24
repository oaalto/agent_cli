# 05 — Worktree resume context aligned with kernel resolver

**Parent:** `prd.md`

**What to build:** `WorktreeLaunchCoordinator` replaces `resolveWorkingDirectory`, `resolveWslPaths`, and `resolveExecutionTarget` with kernel `AgentLaunchResolver` calls inside `buildResumeContext`. `ResumeContext` WSL fields (`wslWorkingDirectory`, `wslDistribution`, `hostWorkingDirectory`) match what PTY and ACP launch adapters use for the same configuration. `PtyResumeStrategy` stops parsing execution target independently — it consumes the resolved target from `ResumeContext` or the shared parser from ticket 01. The silent `null` WSL failure in `resolveWslPaths` is replaced by explicit propagation of the kernel `Result.failure` contract.

**Blocked by:** 02 — Kernel `AgentLaunchResolver` with unit tests

**Status:** done

- [x] `buildResumeContext` calls `resolveLaunchInputs` with worktree path as the working-directory override
- [x] WSL `ResumeContext` fields are populated from `ResolvedLaunchInputs.Wsl`; local resume uses `ResolvedLaunchInputs.Local` working directory
- [x] Mapping failures no longer silently return `null` — coordinator propagates kernel failures via `Result<ResumeContext>` (documented in code)
- [x] `PtyResumeStrategy` deletes its private execution-target parser; resume probing receives consistent WSL path inputs matching editor launch paths
- [x] `WorktreeLaunchCoordinatorTest` verifies `ResumeContext` WSL fields match kernel output for WSL configurations
- [x] `CHANGELOG.md` updated under `### Changed`
- [x] `./gradlew qualityGate` passes
