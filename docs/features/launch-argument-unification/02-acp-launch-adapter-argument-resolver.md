# 02 — ACP launch adapter uses argument resolver

**Parent:** `prd.md`

**What to build:** `AcpProcessLauncher` becomes a thin adapter in the sequential pipeline: `AgentLaunchResolver` → `LaunchArgumentResolver` → `AcpLaunchPlan` assembly via `AgentCommandBuilder`. Duplicate executable checks and inline env handling in `buildLocalPlan` / `buildWslPlan` are removed; the adapter consumes `ResolvedLaunchArguments` from the kernel. `AcpLaunchArguments` retains MCP bridge resolution and ACP-specific resume/agent-flag tails only — MCP toggles and `AcpLaunchPlan` transport fields stay in the ACP slice.

**Blocked by:** 01 — Kernel `LaunchArgumentResolver` with unit tests

**Status:** ready-for-agent

- [ ] `buildLaunchPlan` calls `LaunchArgumentResolver` after `resolveLaunchInputs`; local and WSL plan builders no longer contain standalone executable-validation blocks
- [ ] WSL plans pass kernel-resolved environment variables into `AgentWslCommandRequest`; behaviour matches pre-migration ACP WSL launches for the same configuration
- [ ] `LaunchResumePlan.AcpLoad` / `AcpResolveSession` / `AcpNewSession` resume tails are assembled in the ACP adapter after kernel validation, not duplicated inside `AcpLaunchArguments.resolve`
- [ ] MCP server resolution and `exposeMcp` handling are unchanged and remain ACP-only
- [ ] `AcpLaunchPlanTest` and `AcpProcessLauncherTest` smoke coverage updated; redundant executable-validation cases removed in favour of kernel tests
- [ ] `./gradlew qualityGate` passes
