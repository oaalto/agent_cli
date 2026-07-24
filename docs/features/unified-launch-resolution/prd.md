## Status

draft

## Problem Statement

Launch path resolution — working directory precedence, WSL path mapping, execution-target dispatch, and pre-launch validation — is implemented independently in three call sites across three vertical slices. The slices share low-level helpers (`WorkingDirectoryResolver`, `WslPathResolver`, `AgentCommandBuilder`) but each re-implements the **orchestration** that wires those helpers together. The result is shallow modules at the slice boundary: callers must know override precedence, WSL failure modes, and distribution inference, and any fix must be applied in multiple places.

### Specific duplication points

| Concern | `AcpProcessLauncher` (`acp/`) | `PtyAgentEditor` (`pty/`) | `WorktreeLaunchCoordinator` (`worktree/`) |
| --- | --- | --- | --- |
| Execution target parsing | `resolveExecutionTarget` (private) | `resolvePtyExecutionTarget` in `PtyEditorSupport.kt` | `resolveExecutionTarget` (private) |
| Local working directory | `WorkingDirectoryResolver.resolve` + directory-exists check + executable check | Same trio in `buildLocalTerminalStartupRequest` | `resolveWorkingDirectory` → `WorkingDirectoryResolver.resolve` |
| WSL raw-path precedence (override → configured → project base) | Inline in `buildWslPlan` (lines 98–104) | Inline in `buildWslTerminalStartupRequest` (lines 262–271) | Inline in `resolveWslPaths` (lines 116–124) |
| WSL map validation (`mapToWslPath == null` → error) | `buildWslPlan` (lines 105–111) | `buildWslTerminalStartupRequest` (lines 272–282) | Silent `return null` in `resolveWslPaths` |
| WSL distribution resolution | `wslDistribution` or `inferredDistribution` | Same | Same |
| Host working directory for terminal widget | `WslPathResolver.resolveHostWorkingDirectory` | Same | Same |
| WSL Linux path | `WslPathResolver.resolveWslWorkingDirectory` | Same | `mapToWslPath` directly with `/home` fallback |

Additional copies of `resolveExecutionTarget` exist in `PtyResumeStrategy` (`worktree/resume/`), bringing the count to **four** identical three-line parsers.

`AgentCommandBuilder` and `WslPathResolver`/`WorkingDirectoryResolver` already live in the kernel package (`com.oaalto.agent`) and are correctly shared. The missing piece is a **deep module** that owns the orchestration seam between configuration + launch context → resolved launch inputs (paths, target, validation outcomes).

### Deletion test

- **Delete `WorkingDirectoryResolver` or `WslPathResolver`:** complexity reappears in every launch call site — they earn their keep.
- **Delete `AgentCommandBuilder`:** WSL command assembly and node-shell wrapping reappear in `acp/` and `pty/` — it earns its keep.
- **Delete the inline WSL precedence + validation blocks in `AcpProcessLauncher`, `PtyAgentEditor`, and `WorktreeLaunchCoordinator`:** the same ~30-line logic would need to be re-authored in all three — **this duplication does not earn its keep** and is the target of this PRD.
- **Delete any one of the four `resolveExecutionTarget` copies:** the other three still compile — shallow pass-through with no locality.

### Impact

- WSL path bugs (e.g. UNC vs drive-letter mapping, override precedence) must be fixed in up to three places; `WorktreeLaunchCoordinator.resolveWslPaths` already diverges slightly (silent `null` vs explicit `IllegalStateException`).
- Resume probing in `worktree/` resolves WSL paths through a different code path than the editors that actually launch processes, risking inconsistent `CursorResumeProbeRequest` inputs.
- New execution targets or working-directory rules require touching multiple slices, violating ADR 0001's mode-agnostic worktree orchestration intent.

## Solution

Introduce a **kernel deep module** — `AgentLaunchResolver` (name TBD during implementation) — at `com.oaalto.agent` alongside `AgentCommandBuilder` and `WslPathResolver`. This is not a new vertical slice; it is shared launch plumbing that two or more slices genuinely need (per vertical-slice-boundaries: promote to kernel when plumbing is shared).

### Interface (high leverage, small surface)

A single entry function resolves launch inputs from mode-agnostic inputs:

```
resolveLaunchInputs(
  configuration: AgentCliConfiguration,
  projectBasePath: String?,
  workingDirectoryOverride: String?,
) → Result<ResolvedLaunchInputs>
```

`ResolvedLaunchInputs` is a sealed hierarchy (or equivalent) covering:

- **Local:** `workingDirectory` (validated: exists, executable check delegated to caller or included as `ValidationOutcome`)
- **Wsl:** `linuxPath`, `distribution`, `hostWorkingDirectory`, plus enough metadata for session scope (`sessionWorkingDirectory` semantics)

The module owns:

1. Execution-target dispatch (`LOCAL` / `WSL`).
2. Working-directory precedence (override → configured → project base → fallback).
3. WSL path mapping and validation (single error message contract).
4. Distribution inference (`wslDistribution` config or `inferredDistribution` from mapped path).
5. Host working-directory resolution for the terminal widget / process CWD split.

The module does **not** own mode-specific concerns (see Out of Scope).

### Adapter thinning

After consolidation, slice code becomes thin adapters:

| Slice | Retains | Delegates to kernel |
| --- | --- | --- |
| `acp/` (`AcpProcessLauncher`) | `AcpLaunchArguments`, MCP bridge, `AcpLaunchPlan` assembly, `Result` → transcript error | Path/target resolution, WSL validation |
| `pty/` (`PtyAgentEditor`) | `TerminalStartupRequest`, cursor resume fallback, terminal widget launch, `showError` UI | Path/target resolution, WSL validation |
| `worktree/` (`WorktreeLaunchCoordinator`) | `AgentLaunchContext` / resume plan wiring | `resolveWslPaths`, `resolveExecutionTarget`, `resolveWorkingDirectory` |

`AgentCommandBuilder` remains the command-building seam; adapters call it with resolved inputs plus mode-specific arguments.

### Locality and leverage

- **Locality:** one place to change WSL precedence, validation messages, and execution-target parsing.
- **Leverage:** callers pass three inputs and receive a complete path resolution; they no longer re-implement the override chain.
- **Seam:** `ResolvedLaunchInputs` is the test surface; slice adapters are hypothetical seams today, real seams after this change.

## User Stories

1. As a user launching the same agent configuration in PTY Passthrough and ACP Client mode with WSL execution, I want both modes to resolve the same Linux working directory and host CWD, so that behavior is predictable regardless of launch mode.
2. As a user opening a worktree session on WSL, I want the worktree path override to map to the same WSL Linux path that the terminal or ACP subprocess uses, so that resume probing and actual launch agree.
3. As a user with a working directory that cannot be mapped to WSL, I want a clear, consistent error in both PTY and ACP modes, so that I know how to fix my path format.
4. As a developer fixing a WSL UNC-path mapping bug, I want to change one module and have PTY, ACP, and worktree resume all pick up the fix, so that I do not chase three copies of the same logic.
5. As a developer adding a new execution target (future), I want a single dispatch point in the kernel module, so that slice adapters only map resolved inputs to their transport-specific plans.
6. As a reviewer of launch changes, I want unit tests on `AgentLaunchResolver` that cover override precedence and WSL failure modes without spinning up the IDE terminal widget or ACP session loop, so that regressions are caught mechanically.
7. As a developer working in the `pty/` slice, I want `PtyAgentEditor` to focus on terminal UI and cursor-resume probing, not WSL path precedence, so that the editor file stays an adapter.
8. As a developer working in the `worktree/` slice, I want `WorktreeLaunchCoordinator` to consume the same resolved WSL paths as the editors, so that `ResumeContext` fields match what launch adapters use.

## Implementation Decisions

### Ownership

- **Kernel module (new):** `com.oaalto.agent.AgentLaunchResolver` (+ `ResolvedLaunchInputs` types) — lives at repo root agent package, **not** inside `pty/`, `acp/`, `worktree/`, or `settings/`.
- **Existing kernel helpers (extend, do not duplicate):** `WorkingDirectoryResolver`, `WslPathResolver`, `AgentCommandBuilder` in `com.oaalto.agent`.
- **Slice adapters (thin):** `AcpProcessLauncher`, `PtyAgentEditor`, `WorktreeLaunchCoordinator`.
- **Delete:** all private `resolveExecutionTarget` copies; consolidate into kernel (or a one-liner on `AgentSettingsState.ExecutionTarget` if preferred).

### Module shape

```
com.oaalto.agent/
  AgentLaunchResolver.kt      ← new deep module (interface + implementation)
  ResolvedLaunchInputs.kt     ← sealed result types (or co-located)
  AgentCommandBuilder.kt      ← unchanged seam for argv assembly
  WslPathResolver.kt          ← path primitives; resolver delegates here
```

### Resolution algorithm (canonical)

1. Parse `executionTarget` from `configuration.executionTarget` (default `LOCAL`).
2. Compute effective raw path: `workingDirectoryOverride` → `configuration.workingDirectory` → `projectBasePath` → empty.
3. **Local branch:** `WorkingDirectoryResolver.resolve(...)`; validate directory exists; surface `Result.failure` with path in message.
4. **WSL branch:**
   - Map raw path via `WslPathResolver.mapToWslPath`; if raw path is non-blank and mapping fails → `Result.failure` with the PTY-mode hint text (helpful examples list).
   - Linux path: mapped path or `resolveWslWorkingDirectory` fallback (`/home`).
   - Distribution: `configuration.wslDistribution.trim()` or `inferredDistribution`.
   - Host CWD: `WslPathResolver.resolveHostWorkingDirectory(projectBasePath)`.
5. Return `ResolvedLaunchInputs.Local` or `ResolvedLaunchInputs.Wsl`.

### Error contract unification

`WorktreeLaunchCoordinator.resolveWslPaths` currently returns `null` on mapping failure; editors throw/show errors. The kernel module must use **one** contract — prefer `Result.failure` with message — and `WorktreeLaunchCoordinator` propagates or maps to resume-context defaults explicitly (document the choice in implementation).

### Slice-specific assembly (stays in adapters)

- **`acp/`:** After `resolveLaunchInputs`, `AcpProcessLauncher` merges `AcpLaunchArguments.resolve(...)`, calls `AgentCommandBuilder.buildLocalCommand` / `buildWslCommand` (with env vars for WSL), wraps in `AcpLaunchPlan`. Executable-path check can remain in adapter or move to kernel — prefer kernel if identical in both modes.
- **`pty/`:** After resolution, apply `applyCursorResumeFallbackForLocal` / `applyCursorResumeFallbackForWsl`, then `AgentCommandBuilder`, then `TerminalStartupRequest`. Error display stays in `showError`.
- **`worktree/`:** Replace `resolveWorkingDirectory`, `resolveWslPaths`, `resolveExecutionTarget` with kernel calls inside `buildResumeContext`. `PtyResumeStrategy.resolveExecutionTarget` deleted; use resolved target from `ResumeContext` or kernel.

### ADR alignment

- **ADR 0001:** Reinforces mode-agnostic worktree orchestration and shared WSL/command plumbing; does not change Launch Mode UX or protocol responsibilities. Aligned.
- **ADR 0002:** No SDK interaction changes. Aligned.
- **ADR 0003:** No settings schema changes. Aligned.

### Vertical slice boundaries

- `settings/` exports `LaunchMode` and `AgentCliConfiguration`; kernel reads settings types but `settings/` does not import launch resolver.
- Cross-slice imports: slices import `AgentLaunchResolver` from kernel public entry only — no deep imports between `pty/`, `acp/`, `worktree/`.
- `AgentCommandBuilder` WSL/node-wrapper reuse (noted in ADR 0001) continues unchanged.

## Testing Decisions

### What to test

- **Interface is the test surface:** `AgentLaunchResolver.resolveLaunchInputs` — external behavior only (paths, target, success/failure messages).
- Override precedence: worktree override wins over configured, configured over project base.
- WSL mapping: Linux path, UNC path, Windows drive path; mapping failure when path is non-blank and unmapped.
- Distribution: explicit `wslDistribution` vs inferred from UNC.
- Host working directory: falls back to `user.home` when project base is not a valid directory.
- Execution target parsing: unknown value defaults to `LOCAL`.

### Modules to test

- **New:** `src/test/kotlin/com/oaalto/agent/AgentLaunchResolverTest.kt` (kernel — not inside a slice folder).
- **Update:** `AcpProcessLauncherTest` — retain integration-level command assertions; remove redundant path-resolution cases now covered by kernel tests.
- **Update:** `WorktreeLaunchCoordinatorTest` — verify `ResumeContext` WSL fields match kernel output for WSL configurations.

### Prior art

- `AcpProcessLauncherTest` already exercises end-to-end local and WSL command shapes; keep a minimal smoke test per mode after refactor.
- `WslPathResolver` primitives remain tested implicitly via resolver tests; add dedicated tests only if kernel exposes new combinations.

### Verification

`./gradlew qualityGate` passes after implementation.

## Out of Scope

- Changing `AgentCommandBuilder` argv assembly, node-shell wrapper, or `env` prefix logic.
- Cursor resume probe behavior (`CursorResumeProbe`, `PtyResumeStrategy` argument selection) — only path **inputs** to the probe are in scope.
- `AcpLaunchArguments` PTY-resume stripping and `acp` subcommand injection.
- MCP capability bridge and `AcpLaunchPlan` MCP fields.
- PTY terminal widget lifecycle, error panel UI, and keyboard navigation.
- ACP session loop, transcript, and `SessionScopeResolver` (consumes `sessionWorkingDirectory` but does not define resolution).
- New execution targets beyond `LOCAL` and `WSL`.
- `settings/` schema or `LaunchMode` enum changes.
- `AgentWorktreePathMapper` worktree filesystem layout (orthogonal path-mapping concern).

## Further Notes

- `WorkingDirectoryResolver` currently lives in `WslPathResolver.kt`; consider co-locating with `AgentLaunchResolver` or splitting to `WorkingDirectoryResolver.kt` during implementation — cosmetic only, not a blocker.
- The four `resolveExecutionTarget` copies are the clearest shallow-module signal: zero leverage, no locality. Fold into `AgentSettingsState.ExecutionTarget.from(raw: String)` or the new resolver.
- `PtyAgentEditor.buildWslTerminalStartupRequest` includes user-facing WSL path format hints in its error message; preserve that text in the kernel module's failure message so ACP mode gains the same guidance.
- After implementation, update `CHANGELOG.md` under `### Changed` and add a `skip` or `update` entry to `docs/wiki/log.md` if wiki subsystem pages for launch paths are added.
- Related: ADR 0001 notes `AgentCommandBuilder` WSL reuse for ACP — this PRD extends that pattern to **resolution**, not just command building.
- Deletion-test success criterion: removing `AgentLaunchResolver` would force re-copying orchestration into `acp/`, `pty/`, and `worktree/` — confirming the module earns its keep.
