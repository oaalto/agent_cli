## Status

ready-for-agent

## Problem Statement

`unified-launch-resolution` consolidated **path and execution-target** resolution into kernel `AgentLaunchResolver` — working directory precedence, WSL mapping, distribution inference, and validation. **Launch Mode** adapters (`AcpProcessLauncher`, `PtyAgentEditor`, `CursorResumeProbe`) still own separate **argument resolution** and command-assembly orchestration:

| Concern | ACP Client | PTY Passthrough | Worktree probe |
| --- | --- | --- | --- |
| Resume/extra CLI args | `AcpLaunchArguments` | `PtyResumeStrategy` + cursor fallback in editor | `CursorResumeProbe` builds commands independently |
| Executable validation | `AcpProcessLauncher.buildLocalPlan` | `PtyAgentEditor.buildLocalTerminalStartupRequest` | — |
| WSL environment variables | Passed via `AgentWslCommandRequest` | Omitted in PTY WSL path | Partial / divergent |
| MCP bridge args | ACP-only in `AcpLaunchArguments` | N/A | N/A |

Fixing a WSL env-var or executable-check bug still requires touching multiple slices. `AgentCommandBuilder` is deep; the **argument + validation glue** above it is shallow and duplicated — interface nearly as complex as copying the same checks into each launcher.

**Deletion test:** Removing the duplicated validation blocks from two launchers would not eliminate complexity — it would move to a third caller. A kernel-level **launch argument resolver** (or extension of launch resolution) would concentrate it.

## Solution

Introduce a kernel deep module — `LaunchArgumentResolver` (name TBD) — that sits after `AgentLaunchResolver.resolveLaunchInputs` and before mode-specific plan assembly. It owns shared concerns both **Launch Modes** need:

1. Executable binary exists and is executable (local and WSL).
2. WSL command environment variable parity (shared contract for PTY and ACP).
3. Resume argument resolution dispatch by `LaunchResumePlan` variant (PTY extra args vs ACP agent flags) — mode-specific tails stay in slice adapters.
4. Optional: shared hook for `CursorResumeProbe` to use the same resolved command base.

Mode adapters become thin:

- **ACP:** `AcpLaunchArguments` retains MCP bridge and `AcpLaunchPlan` assembly only.
- **PTY:** `PtyAgentEditor` retains `TerminalStartupRequest` and terminal widget launch only.
- **Worktree:** `CursorResumeProbe` delegates command base to kernel.

### Interface (conceptual)

```
resolveLaunchArguments(
  configuration: AgentCliConfiguration,
  resolvedInputs: ResolvedLaunchInputs,
  resumePlan: LaunchResumePlan?,
  launchMode: LaunchMode,
) → Result<ResolvedLaunchArguments>
```

`ResolvedLaunchArguments` carries mode-agnostic validated executable path, env map, and mode-specific argument lists (sealed tail or paired PTY/ACP fields).

## User Stories

1. As a user launching an agent with a missing executable, I want the same clear error in PTY and ACP modes, so that validation messages do not diverge by **Launch Mode**.
2. As a user launching via WSL in PTY mode, I want environment variables from configuration applied consistently with ACP mode, so that agent behaviour matches across modes.
3. As a developer fixing executable validation, I want one kernel function to update, so that ACP and PTY launchers stay in sync.
4. As a developer adding a new execution target, I want argument resolution extended in one module, so that three call sites do not each grow new branches.
5. As a user resuming a Cursor session in a worktree, I want resume probe commands built with the same path resolution as the actual launch, so that probe results match runtime.
6. As a developer reading launch code, I want `AcpProcessLauncher` and `PtyAgentEditor` to show mode-specific assembly only, so that navigation matches ADR 0001 slice boundaries.
7. As a user with MCP toggles enabled (ACP only), I want MCP args still assembled in the ACP adapter, so that kernel module does not absorb MCP bridging.
8. As a developer, I want `AgentLaunchResolverTest` patterns extended for argument resolution tests, so that CI guards the new seam.
9. As a user launching with `LaunchResumePlan.Pty` extra args, I want PTY resume args resolved through the shared resume dispatch, so that coordinator plan semantics stay mode-agnostic at the kernel boundary.
10. As a user launching with `LaunchResumePlan.AcpLoad`, I want ACP-specific agent flags resolved in the ACP tail after kernel validation, so that protocol resume stays in the ACP slice.
11. As a developer maintaining `WorktreeLaunchCoordinator`, I want it to call kernel resolution only, so that worktree remains mode-agnostic for paths and shared validation.
12. As a user on Windows WSL, I want distribution and Linux path errors identical whether I use PTY or ACP, so that `AgentLaunchResolver` + argument resolver form one story.

## Implementation Decisions

### Ownership

| Module | Package | Role |
| --- | --- | --- |
| `LaunchArgumentResolver` | `com.oaalto.agent` (kernel) | Deep module — shared validation + resume arg dispatch |
| `AcpProcessLauncher` | `acp/` | Thin — MCP, `AcpLaunchPlan`, transport spawn |
| `PtyAgentEditor` / `PtyEditorSupport` | `pty/` | Thin — terminal widget, cursor fallback UI |
| `CursorResumeProbe` | `worktree/resume/` | Thin — probe invocation using kernel command base |

### Relationship to `AgentLaunchResolver`

Sequential pipeline:

```
AgentLaunchResolver.resolveLaunchInputs → LaunchArgumentResolver.resolveLaunchArguments → mode adapter.buildPlan/startupRequest
```

Do not merge into one mega-resolver unless line count stays lower — prefer two deep modules over one shallow god function.

### Modules to create

- `LaunchArgumentResolver.kt`
- `ResolvedLaunchArguments.kt` (sealed hierarchy or data classes)
- `LaunchArgumentResolverTest.kt`

### Modules to modify

- `AcpProcessLauncher` — remove duplicate executable checks; call kernel.
- `PtyAgentEditor` — remove duplicate executable checks; add WSL env parity.
- `CursorResumeProbe` — use kernel for command base where applicable.
- `PtyResumeStrategy` — unchanged plan selection; may consume kernel for arg tail.

### ADR alignment

- **ADR 0001:** Mode-agnostic worktree orchestration + slice-thin adapters preserved; kernel promotion justified (shared by ≥2 slices).

## Testing Decisions

### What to test

- Executable missing / not executable — same `Result` failure for LOCAL and WSL across modes.
- WSL env map present in PTY path when configured (parity test with ACP).
- Resume plan dispatch: `Pty(extraArgs)` vs ACP plan variants produce correct argument tails without duplicating validation.
- `CursorResumeProbe` uses same resolved executable path as launchers (integration-style unit test with fixtures).

### Modules to test

- **New:** `LaunchArgumentResolverTest`
- **Extend:** `AcpLaunchPlanTest`, `AgentEditorFactoryTest` if behaviour assertions move.

### Prior art

- `AgentLaunchResolverTest` — context builders, LOCAL/WSL matrix.
- `AgentCommandBuilderTest` — command assembly after resolution.
- `AcpLaunchArgumentsTest` — ACP-specific tail tests remain.

### Verification

`./gradlew qualityGate` passes.

## Out of Scope

- Merging `AgentLaunchResolver` and `LaunchArgumentResolver` into one type (unless implementation proves smaller).
- MCP bridge implementation details (stay in ACP adapter).
- ACP protocol session resume (`AcpSessionResumeOrchestrator`).
- Terminal widget UX, transcript UI, worktree creation/deletion.
- New execution targets beyond LOCAL/WSL.
- Changes to `agentSettings.xml` schema.

## Further Notes

- **Recommendation strength:** Strong — highest cross-slice leverage from architecture review candidate #3.
- **Builds on:** completed `unified-launch-resolution` feature.
- **Risk:** WSL env parity may surface latent PTY bugs — treat as bug fix, not regression.
