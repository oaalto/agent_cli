## Status

draft

## Problem Statement

ACP Client mode binds each **Worktree** to an `acpSessionId` so a user can resume the same agent session when reopening an isolated worktree filesystem (ADR 0001). The product intent is clear: **Launch Mode** selection and **Agent configuration** determine how resume works — PTY Passthrough uses CLI flags; ACP Client uses `session/load`, `listSessions`, and persisted IDs via `AgentWorktreeStateService`.

The implementation splits resume across three modules with weak **locality** and a shallow strategy that does not earn its depth:

### Where logic lives today

| Concern | Module | Location | Lines (approx.) |
| --- | --- | --- | --- |
| Plan selection (which resume path?) | `AcpResumeStrategy` | `worktree/resume/AcpResumeStrategy.kt` | 18 |
| Attach plan to launch | `WorktreeLaunchCoordinator` | `worktree/WorktreeLaunchCoordinator.kt` | `buildLaunchContext` (23–46), `toLaunchContext` (74–92) |
| Plan execution (load / list / pick / persist) | `AcpAgentEditor` | `acp/AcpAgentEditor.kt` | 283–369 (~87) |
| Session ID persistence | `AgentWorktreeStateService` | `worktree/AgentWorktreeStateService.kt` | `setAcpSessionId` (163–179) |
| ACP protocol ops | `AcpSessionController` / `AcpSessionControllerImpl` | `acp/AcpSessionController.kt`, `AcpSessionControllerImpl.kt` | `newSession`, `loadSession`, `listSessions`, `currentSessionId` |

**~90 lines** of resume/load/picker/persist orchestration are scattered: ~18 lines decide intent; ~87 lines in the editor execute it. The editor is a **Transcript** UI host that should not own worktree binding rules.

### Shallow strategy, missing execution locality

`AcpResumeStrategy.prepareLaunch` (`AcpResumeStrategy.kt:4–17`) maps `ResumeContext` to a `LaunchResumePlan` variant:

- `resume == false` or configuration mismatch → `AcpNewSession` (lines 6–10)
- stored `acpSessionId` present → `AcpLoad(storedId)` (lines 12–14)
- resume with no stored ID → `AcpPickSession(emptyList())` (line 15)

The `ResumeStrategy` interface (`ResumeStrategy.kt:3–5`) exposes only `prepareLaunch(context): LaunchResumePlan` — a pure data transform. **Actual session open behavior** happens later in `AcpAgentEditor.openSessionFromResumePlan` (`AcpAgentEditor.kt:283–314`), which branches on the same plan variants and calls `sessionController.loadSession`, `newSession`, `pickSessionOrStartFresh`, and `persistBoundSessionId`.

**Deletion test on `AcpResumeStrategy`:** Deleting it moves ~18 lines of branching into `WorktreeLaunchCoordinator` or `AcpAgentEditor`. The ~87 lines of load/pick/persist/fallback logic in the editor **remain**. The strategy is a pass-through for intent labels, not a deep module — its **interface** is nearly as complex as its **implementation**, and callers still need full knowledge of what each `LaunchResumePlan` variant implies at runtime.

**Deletion test on `AcpAgentEditor` resume methods:** Deleting `openSessionFromResumePlan`, `pickSessionOrStartFresh`, `pickSessionFromCandidates`, and `persistBoundSessionId` would force the same branching, error recovery, and persistence calls to reappear wherever ACP sessions open — likely duplicated across future entry points. That complexity is real and should live in one deep module.

### Misleading plan shape and dead field

`LaunchResumePlan.AcpPickSession` (`LaunchResumePlan.kt:12–14`) carries `candidates: List<SessionSummary>`, but `AcpResumeStrategy` always passes `emptyList()` (`AcpResumeStrategy.kt:15`). Candidates are fetched at execution time in `pickSessionOrStartFresh` (`AcpAgentEditor.kt:316–327`) via `sessionController.listSessions(sessionWorkingDirectory)`. The plan type suggests pre-resolved candidates; the runtime never uses pre-populated values. This is a **seam leak** — the data model documents behavior the strategy does not perform.

### Duplicated fallback paths and weak error recovery coverage

`AcpAgentEditor` encodes the same outcomes in multiple places:

1. **`AcpLoad` failure** (lines 286–296): log, transcript message, fall through to `pickSessionOrStartFresh`.
2. **`AcpPickSession` with empty candidates** (lines 298–306): transcript message, `pickSessionOrStartFresh`.
3. **`pickSessionFromCandidates` empty list** (lines 331–335): `newSession` + `persistCurrentSessionId`.
4. **Picker returns null (Start fresh)** (lines 341–345): same as (3).
5. **Picker selection load failure** (lines 351–358): error transcript, then `newSession` + persist.

Each path must correctly call `sessionController` and `AgentWorktreeStateService.setAcpSessionId` (`AcpAgentEditor.kt:366–368`). There is no single place documenting the invariant: *after any successful session open, persist the session ID to the bound worktree record when `worktreeId` is present*.

### Cross-slice coupling

`AcpAgentEditor` (ACP / **Transcript** slice) directly imports and calls `AgentWorktreeStateService.getInstance().setAcpSessionId` (`AcpAgentEditor.kt:30`, `368`). Per vertical-slice boundaries, the `acp/` slice should not reach into worktree persistence internals — it should call a **public entry** or injected **adapter** at a **seam**.

`WorktreeLaunchCoordinator` correctly stays mode-agnostic for PTY vs ACP plan selection (`WorktreeLaunchCoordinator.kt:17–21`), but ACP execution asymmetry (PTY resume completes at plan time via CLI args; ACP resume needs a connected client) is unacknowledged in module boundaries.

### Test coverage gap

| Area | Tests | Gap |
| --- | --- | --- |
| Plan selection | `AcpResumeStrategyTest` (4 cases) | No execution |
| Coordinator wiring | `WorktreeLaunchCoordinatorTest` (ACP load plan case) | No editor integration |
| Persistence | `AgentWorktreeStateServiceTest` (`setAcpSessionId`, config change, delete) | No bind-after-open flow |
| Session controller | `AcpSessionControllerTest` (dispose only) | No resume ops |
| Editor orchestration | **None** | All fallback branches untested |

The **interface** of resume orchestration is not a test surface today because orchestration is private methods inside a Swing `FileEditor`. Bugs in load-fail → picker → new-session chains have high surface area and no automated guard.

### Contrast with PTY resume (healthy depth)

`PtyResumeStrategy` (`PtyResumeStrategy.kt:7–26`) is deep for its slice: `prepareLaunch` fully resolves resume into `LaunchResumePlan.Pty(extraArgs)` including probe logic. No second execution phase exists. ACP resume is intentionally two-phase (plan at launch, execute after `connect`), but the second phase was placed in the UI editor instead of a dedicated module — sacrificing **leverage** (callers cannot open a bound ACP session without dragging in transcript and picker wiring).

## Solution

Consolidate ACP session resume **execution** into one **deep module** behind a small **interface**, with **adapters** at the **seams** for ACP protocol, worktree binding, and session picker UI.

### Target shape

Introduce `AcpSessionResumeOrchestrator` in `worktree/resume/` — the slice that already owns `LaunchResumePlan`, `ResumeStrategy`, and worktree session ID semantics per ADR 0001.

**Interface (high leverage, low surface):**

```kotlin
suspend fun openSession(
    plan: LaunchResumePlan,
    sessionWorkingDirectory: String,
    worktreeRecordId: String?,
): AcpSessionOpenResult
```

**Ports (injected adapters — two adapters = real seam):**

| Port | Responsibility | Default adapter location |
| --- | --- | --- |
| `AcpSessionOperations` | `newSession`, `loadSession`, `listSessions`, `currentSessionId` | `acp/` — wraps `AcpSessionController` |
| `WorktreeSessionBinder` | `persistSessionId(recordId, sessionId)` | `worktree/` — wraps `AgentWorktreeStateService` |
| `SessionPicker` | `pickSession(candidates): String?` (`null` = start fresh) | `acp/ui/` — wraps `SessionPickerDialog` |
| `ResumeNotifier` (optional) | User-visible status lines (load failed, started fresh, resumed) | `acp/` — writes to transcript or no-op in tests |

The orchestrator owns the full decision tree currently in `AcpAgentEditor.kt:283–369`:

1. Interpret `LaunchResumePlan` (reject `Pty` with clear error — same as line 312).
2. `AcpLoad` → try `loadSession` → on failure, list + pick.
3. `AcpPickSession` → list if candidates empty → pick.
4. `AcpNewSession` / `null` → `newSession`.
5. After any successful open, persist via `WorktreeSessionBinder` when `worktreeRecordId != null`.
6. Return structured `AcpSessionOpenResult` (opened session ID, whether picker was shown, fallback reason) for the editor to render **Transcript** messages if desired.

`AcpAgentEditor.launchAndConnect` (`AcpAgentEditor.kt:267–268`) becomes: `connect` → `orchestrator.openSession(file.launchContext.resumePlan, …)` → enable prompt. Resume private methods are deleted from the editor.

Keep `AcpResumeStrategy` as a pure plan-selection function **or** fold its 18 lines into a private `resolvePlan(context)` inside the orchestrator's companion/factory — either is acceptable; the PRD preference is **keep strategy separate** for symmetry with `PtyResumeStrategy` and `WorktreeLaunchCoordinator.strategyFor`, but **remove `AcpPickSession.candidates`** or stop using the field (replace with `AcpPickSession` object or `AcpResolveSession`) so the plan type matches runtime behavior.

### What callers gain (leverage + locality)

- **Leverage:** Any future ACP entry point (headless reopen, batch resume, tests) calls one method after `connect`.
- **Locality:** Load-fail recovery, picker fallback, and persist-invariant live in one file; editor only adapts UI notifications.
- **Deletion test:** Removing the orchestrator would scatter ~90 lines across callers; keeping it concentrates real complexity.

## User Stories

1. As a user resuming a **Worktree** with a stored `acpSessionId`, I want the plugin to load that ACP session automatically after connect, so that my prior agent context is restored without manual steps.
2. As a user whose stored session ID is stale or invalid, I want a clear message and a session picker, so that I can choose another session or start fresh without a broken editor tab.
3. As a user opening a worktree with no stored session ID but `resume=true`, I want to pick from available ACP sessions for that working directory, so that I can attach the correct session to this worktree.
4. As a user choosing "Start a new session" in the picker, I want a new ACP session created and bound to the worktree, so that future resumes use the new ID.
5. As a user who changed the **Agent configuration** on a worktree record, I want resume to start a new session instead of loading an ID from the previous configuration, so that sessions do not leak across agents.
6. As a developer fixing a resume bug, I want all ACP load/list/pick/persist branches in one orchestrator module, so that I do not hunt across `AcpAgentEditor` and `AcpResumeStrategy`.
7. As a developer adding resume behavior, I want to test the orchestrator with fake ports (no Swing, no live ACP process), so that regressions are caught in CI.
8. As a user opening an ACP editor without a worktree binding (`worktreeId == null`), I want the session to open normally without persistence errors, so that non-worktree launches still work.

## Implementation Decisions

### Ownership / slice

| Layer | Slice | Role |
| --- | --- | --- |
| `AcpSessionResumeOrchestrator` + port interfaces | `worktree/resume/` | Deep module — resume execution + binding invariant |
| `AcpSessionController` adapter | `acp/` | Protocol **adapter** implementing `AcpSessionOperations` |
| `SessionPickerDialog` adapter | `acp/ui/` | UI **adapter** implementing `SessionPicker` |
| `AgentWorktreeStateService` adapter | `worktree/` | Persistence **adapter** implementing `WorktreeSessionBinder` |
| `AcpAgentEditor` | `acp/` | Thin **adapter** — connect, delegate to orchestrator, map `AcpSessionOpenResult` to **Transcript** lines |

**No new cross-slice imports:** `worktree/resume/` defines port interfaces; `acp/` depends on `worktree/resume/` ports (same direction as today via `LaunchResumePlan` on `AgentLaunchContext`). `worktree/` must not import `acp/` types.

### Modules to create

- `worktree/resume/AcpSessionResumeOrchestrator.kt` — orchestration implementation
- `worktree/resume/AcpSessionOperations.kt` — port interface (or colocate in orchestrator file)
- `worktree/resume/WorktreeSessionBinder.kt` — port interface
- `worktree/resume/SessionPicker.kt` — port interface
- `worktree/resume/AcpSessionOpenResult.kt` — sealed result (success with sessionId, events/messages for UI)
- `acp/AcpSessionOperationsAdapter.kt` — `AcpSessionController` → port
- `acp/ui/SessionPickerAdapter.kt` — `SessionPickerDialog` → port
- `worktree/WorktreeSessionBinderImpl.kt` — `AgentWorktreeStateService` → port

### Modules to modify

- `acp/AcpAgentEditor.kt` — remove `openSessionFromResumePlan`, `pickSessionOrStartFresh`, `pickSessionFromCandidates`, `persistCurrentSessionId`, `persistBoundSessionId`; inject orchestrator (or factory) via constructor default for testability
- `worktree/resume/LaunchResumePlan.kt` — simplify `AcpPickSession` (drop unused `candidates` or rename to `AcpResolveSession`)
- `worktree/resume/AcpResumeStrategy.kt` — update return type if plan variant renamed; logic unchanged

### Modules unchanged (behavior preserved)

- `AgentWorktreeStateService.kt` — persistence API stays; orchestrator calls via binder
- `AcpSessionController.kt` / `AcpSessionControllerImpl.kt` — protocol layer unchanged
- `WorktreeLaunchCoordinator.kt` — still builds `AgentLaunchContext.resumePlan` via `ResumeStrategy`
- `SessionPickerDialog.kt` — UI unchanged; only called through adapter
- `PtyResumeStrategy.kt` / PTY launch path — out of scope

### Seam design

```
WorktreeLaunchCoordinator
  └─ AcpResumeStrategy.prepareLaunch → LaunchResumePlan on AgentLaunchContext

AcpAgentEditor.launchAndConnect
  └─ sessionController.connect(...)
  └─ AcpSessionResumeOrchestrator.openSession(plan, cwd, worktreeId)
        ├─ AcpSessionOperations (adapter → AcpSessionController)
        ├─ WorktreeSessionBinder (adapter → AgentWorktreeStateService)
        └─ SessionPicker (adapter → SessionPickerDialog)
```

Orchestrator is constructed per editor session with ports wired to live controller + project. Constructor injection on `AcpAgentEditor` (with defaults) enables unit tests without IntelliJ UI.

### ADR alignment

- **ADR 0001** (custom ACP client, worktree `acpSessionId` binding): Deepening strengthens the documented worktree ↔ ACP session link; no delegation to JetBrains AI Chat; no UX model change (same picker, same transcript messages).
- **ADR 0002** (Kotlin ACP SDK): Orchestrator calls existing `AcpSessionController` methods; no SDK surface change.
- **ADR 0003** (per-project agent selection): Configuration mismatch rule in `AcpResumeStrategy` (lines 7–8) preserved at plan stage; orchestrator does not re-check configuration — plan already encodes intent.

### Plan type cleanup

Replace `AcpPickSession(candidates: List<SessionSummary>)` with `AcpResolveSession` (no list field) **or** `data object AcpPickSession` to reflect that listing always happens post-connect. Update `AcpResumeStrategy.kt:15`, `AcpResumeStrategyTest.kt:43`, and `WorktreeLaunchCoordinatorTest` if referenced.

## Testing Decisions

### What to test

- **External behavior of the orchestrator** via port fakes — the orchestrator **interface** is the test surface.
- All branches currently in `AcpAgentEditor.kt:283–369`:
  - `AcpLoad` success → persist called
  - `AcpLoad` failure → list + picker invoked
  - `AcpPickSession` / resolve → list when needed
  - Empty session list → `newSession` + persist
  - Picker returns session ID → load + persist
  - Picker returns null → `newSession` + persist
  - Picker selection load fails → `newSession` + persist
  - `AcpNewSession` / null plan → `newSession` + persist
  - `worktreeRecordId == null` → no persist call
  - `LaunchResumePlan.Pty` → clear error (not valid for ACP orchestrator)
- Keep existing tests: `AcpResumeStrategyTest`, `AgentWorktreeStateServiceTest`, `WorktreeLaunchCoordinatorTest`, `AcpSessionControllerTest`.

### Modules to test

- **New:** `src/test/kotlin/com/oaalto/agent/worktree/resume/AcpSessionResumeOrchestratorTest.kt` — fake `AcpSessionOperations`, `WorktreeSessionBinder`, `SessionPicker`, optional `ResumeNotifier`.
- **Optional:** thin `AcpAgentEditor` integration test with mocked orchestrator factory (lower priority than orchestrator unit tests).

### Prior art

- `AcpResumeStrategyTest` — context builder pattern for resume scenarios; reuse for orchestrator tests where plan is input.
- `AgentWorktreeStateServiceTest` — persistence assertions; binder fake records calls for orchestrator tests.
- `AcpSessionControllerTest` — `RecordingSessionController` pattern for fake session ops.

### Verification

`./gradlew qualityGate` passes after implementation.

## Out of Scope

- PTY Passthrough resume (`PtyResumeStrategy`, CLI `--continue` / probe logic).
- Changes to `SessionPickerDialog` layout, copy, or **Transcript** rendering beyond mapping orchestrator results to existing messages.
- ACP protocol, transport, auth, permission, or MCP changes in `AcpSessionControllerImpl`.
- Pre-fetching session candidates at plan time in `WorktreeLaunchCoordinator` (requires connected client; architecturally wrong pre-connect).
- Koog / custom agent implementation (ADR 0001 deferred to 4.0).
- Worktree creation, deletion, or path-mapping logic in `AgentWorktreePathMapper`.
- Persisting session IDs for non-worktree ACP launches (current `worktreeId == null` early-return behavior preserved).
- Wiki ingest automation — manual `docs/wiki/subsystems/worktree.md` update optional post-ship.

## Further Notes

- **Root cause:** ACP resume is two-phase (plan at launch, execute after `connect`) but phase two was embedded in `AcpAgentEditor` instead of a deep `worktree/resume/` module. `AcpResumeStrategy` is shallow relative to total resume complexity.
- **Line anchors for implementers:**
  - Plan selection: `AcpResumeStrategy.kt:4–17`
  - Execution to extract: `AcpAgentEditor.kt:283–369`
  - Persist call site: `AcpAgentEditor.kt:366–368`
  - Coordinator attachment: `WorktreeLaunchCoordinator.kt:39–40`, `90`
  - Persistence API: `AgentWorktreeStateService.kt:163–179`
  - Session ops: `AcpSessionControllerImpl.kt:116–151`
- **Invariant to document in orchestrator:** When `worktreeRecordId` is non-null and a session is successfully opened (load or new), call `WorktreeSessionBinder.persistSessionId` with `currentSessionId()`.
- Implementers should update `CHANGELOG.md` under `### Changed` when code ships.
- Related docs: [CONTEXT.md](../../../CONTEXT.md) (Worktree, Launch Mode, ACP, Agent configuration), [ADR 0001](../../../docs/adr/0001-custom-acp-client-in-plugin.md), [worktree subsystem wiki](../../../docs/wiki/subsystems/worktree.md) — extend with orchestrator ownership after implementation.
- `docs/features/` is gitignored in this repo; this PRD is a planning artifact for the deepening track.
