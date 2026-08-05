# 03 — PTY launch adapter uses argument resolver with WSL env parity

**Parent:** `prd.md`

**What to build:** `PtyAgentEditor` (and related PTY launch helpers) become thin adapters: after `AgentLaunchResolver`, they delegate executable validation, resume arg merge, and WSL environment-variable construction to `LaunchArgumentResolver`, then assemble `TerminalStartupRequest` via `AgentCommandBuilder`. Duplicate executable checks in `buildLocalTerminalStartupRequest` are removed. The PTY WSL path gains environment-variable parity with ACP — configuration env vars flow through `AgentWslCommandRequest` the same way the ACP adapter does after ticket 02. Cursor resume fallback UI hooks (`applyCursorResumeFallbackForLocal` / `ForWsl`) and terminal widget lifecycle remain in the PTY slice.

**Blocked by:** 01 — Kernel `LaunchArgumentResolver` with unit tests

**Status:** ready-for-agent

- [ ] Local and WSL terminal startup paths call `LaunchArgumentResolver` after `resolveLaunchInputs`; inline executable checks are deleted
- [ ] WSL `AgentWslCommandRequest` includes kernel-resolved environment variables when configuration defines them (parity test with ACP path)
- [ ] `LaunchResumePlan.Pty(extraArgs)` extra args are applied through kernel resume dispatch, not re-parsed in the editor
- [ ] Cursor resume fallback and `showError` / `logTerminalFailure` UX are unchanged; failures still surface kernel error messages
- [ ] PTY terminal widget lifecycle, keyboard navigation, and error panel behaviour are unchanged
- [ ] `./gradlew qualityGate` passes
