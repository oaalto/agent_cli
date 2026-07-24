# 04 — PTY launch adapter uses kernel resolver

**Parent:** `prd.md`

**What to build:** `PtyAgentEditor` (and related PTY launch helpers) become thin adapters: path and execution-target resolution delegates to `AgentLaunchResolver`; cursor-resume fallback, `AgentCommandBuilder`, `TerminalStartupRequest` assembly, and `showError` UI remain in the PTY slice. Inline WSL precedence and mapping validation blocks are deleted.

**Blocked by:** 02 — Kernel `AgentLaunchResolver` with unit tests

**Status:** ready-for-agent

- [ ] Local and WSL terminal startup paths call `resolveLaunchInputs` and branch on `ResolvedLaunchInputs.Local` / `.Wsl`
- [ ] WSL mapping failures surface through `showError` using the kernel failure message (preserving user-facing path-format hints)
- [ ] `applyCursorResumeFallbackForLocal` / `applyCursorResumeFallbackForWsl` still run after resolution with the resolved paths and distribution
- [ ] PTY terminal widget lifecycle, keyboard navigation, and error panel behavior are unchanged
- [ ] `./gradlew qualityGate` passes
