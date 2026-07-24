# 03 — ACP launch adapter uses kernel resolver

**Parent:** `prd.md`

**What to build:** `AcpProcessLauncher` becomes a thin adapter: after binary-path validation and `AcpLaunchArguments` resolution, it delegates path and execution-target resolution to `AgentLaunchResolver`, then assembles `AcpLaunchPlan` via `AgentCommandBuilder` with the resolved inputs. Inline WSL precedence, mapping validation, and distribution logic are removed from the ACP slice.

**Blocked by:** 02 — Kernel `AgentLaunchResolver` with unit tests

**Status:** ready-for-agent

- [ ] `buildLocalPlan` and `buildWslPlan` consume `ResolvedLaunchInputs` from the kernel resolver instead of duplicating orchestration
- [ ] WSL mapping failures propagate as `Result.failure` with the kernel error message (ACP gains the same path-format guidance PTY already shows)
- [ ] `AcpLaunchPlan` command shape, MCP fields, and environment-variable handling are unchanged
- [ ] `AcpProcessLauncherTest` retains minimal smoke coverage per execution target; redundant path-resolution cases removed in favor of kernel tests
- [ ] `./gradlew qualityGate` passes
