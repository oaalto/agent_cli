# 02 — Kernel `AgentLaunchResolver` with unit tests

**Parent:** `prd.md`

**What to build:** A kernel deep module at `com.oaalto.agent` that owns launch-input orchestration: execution-target dispatch, working-directory precedence (override → configured → project base), WSL path mapping and validation, distribution inference, and host working-directory resolution. Callers pass configuration plus optional override and project base; they receive `Result<ResolvedLaunchInputs>` covering `Local` and `Wsl` branches. The module delegates to existing `WorkingDirectoryResolver` and `WslPathResolver` primitives — no duplication of path-mapping logic.

**Blocked by:** 01 — Consolidate execution-target parsing

**Status:** done

- [x] `resolveLaunchInputs(configuration, projectBasePath, workingDirectoryOverride)` returns `Result<ResolvedLaunchInputs>` with sealed `Local` and `Wsl` variants carrying the fields slice adapters need (working directory, linux path, distribution, host CWD, session semantics)
- [x] **Local branch:** resolves via `WorkingDirectoryResolver`, validates directory exists, surfaces `Result.failure` with path in message
- [x] **WSL branch:** applies override → configured → project base precedence; non-blank unmapped paths fail with the PTY-mode hint text (Linux path, WSL UNC, Windows drive examples); blank raw path falls back via `resolveWslWorkingDirectory`; distribution uses explicit config or inferred; host CWD via `resolveHostWorkingDirectory`
- [x] Unknown `executionTarget` values default to `LOCAL` (via ticket 01 parser)
- [x] `AgentLaunchResolverTest` covers override precedence, WSL mapping success (Linux, UNC, drive letter), mapping failure, distribution explicit vs inferred, host CWD fallback, and execution-target defaulting — without IDE terminal or ACP session infrastructure
- [x] `./gradlew qualityGate` passes with kernel module and tests only; slice adapters not yet migrated
