# 04 — PTY launch adapter uses kernel resolver

**Parent:** `prd.md`

**What to build:** `PtyAgentEditor` (and related PTY launch helpers) become thin adapters: path and execution-target resolution delegates to `AgentLaunchResolver`; cursor-resume fallback, `AgentCommandBuilder`, `TerminalStartupRequest` assembly, and `showError` UI remain in the PTY slice. Inline WSL precedence and mapping validation blocks are deleted.

**Blocked by:** 02 — Kernel `AgentLaunchResolver` with unit tests

**Status:** done

- [x] Local and WSL terminal startup paths call `resolveLaunchInputs` and branch on `ResolvedLaunchInputs.Local` / `.Wsl`
- [x] WSL mapping failures surface through `showError` using the kernel failure message (preserving user-facing path-format hints)
- [x] `applyCursorResumeFallbackForLocal` / `applyCursorResumeFallbackForWsl` still run after resolution with the resolved paths and distribution
- [x] PTY terminal widget lifecycle, keyboard navigation, and error panel behavior are unchanged
- [x] `./gradlew qualityGate` passes
