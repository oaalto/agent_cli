# 01 — Kernel `LaunchArgumentResolver` with unit tests

**Parent:** `prd.md`

**What to build:** A kernel deep module at `com.oaalto.agent` that sits after `AgentLaunchResolver.resolveLaunchInputs` and before **Launch Mode** plan assembly. It owns shared executable validation (local path contains `/` and is executable; WSL uses the same rules on the resolved binary), WSL command environment-variable construction (shared contract both PTY and ACP will consume), and resume-plan argument dispatch keyed by `LaunchResumePlan` variant — merging PTY extra args into the base argument list while leaving ACP-specific agent flags for the ACP adapter tail. Callers pass configuration, `ResolvedLaunchInputs`, optional `LaunchResumePlan`, and `LaunchMode`; they receive `Result<ResolvedLaunchArguments>` carrying validated executable path, env map, and mode-appropriate argument lists. Unit tests follow `AgentLaunchResolverTest` patterns (LOCAL/WSL matrix, context builders, no IDE infrastructure).

**Blocked by:** None — can start immediately (builds on completed `unified-launch-resolution`)

**Status:** ready-for-agent

- [ ] `resolveLaunchArguments(configuration, resolvedInputs, resumePlan?, launchMode)` returns `Result<ResolvedLaunchArguments>` with fields slice adapters need for command assembly
- [ ] Missing or non-executable binary fails with the same error message shape for LOCAL and WSL inputs (parity with current ACP/PTY messages)
- [ ] WSL branch produces an environment-variable map from configuration when present; LOCAL branch carries configuration env vars without WSL-only omissions
- [ ] `LaunchResumePlan.Pty(extraArgs)` merges extra args into the resolved base list; ACP resume plan variants expose a hook or field the ACP adapter uses for protocol-specific tails without re-running executable validation
- [ ] `LaunchArgumentResolverTest` covers executable failure, WSL env map presence, and resume-plan dispatch (`Pty` vs ACP variants) without duplicating `AgentLaunchResolverTest` path-resolution cases
- [ ] `./gradlew qualityGate` passes with kernel module and tests only; slice adapters not yet migrated
