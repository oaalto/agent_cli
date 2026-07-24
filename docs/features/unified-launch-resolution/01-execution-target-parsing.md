# 01 — Consolidate execution-target parsing

**Parent:** `prd.md`

**What to build:** A single canonical way to parse `executionTarget` configuration strings into `AgentSettingsState.ExecutionTarget`, replacing the four identical private parsers scattered across `acp/`, `pty/`, and `worktree/`. Unknown or blank values default to `LOCAL`. All existing call sites delegate to the shared parser with no behavior change.

**Blocked by:** None — can start immediately

**Status:** done

- [x] `AgentSettingsState.ExecutionTarget` exposes a shared `from(raw: String)` that normalizes, matches enum entries, and defaults to `LOCAL`
- [x] `AcpProcessLauncher`, `PtyAgentEditor` / `PtyEditorSupport`, `WorktreeLaunchCoordinator`, and `PtyResumeStrategy` delete their private `resolveExecutionTarget` copies and call the shared parser
- [x] Existing launch and resume tests pass unchanged — parsing behavior is identical before the kernel module lands
