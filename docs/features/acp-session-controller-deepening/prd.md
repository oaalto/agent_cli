## Status

implemented

## Problem Statement

`AcpSessionController` is a shallow module: its interface exposes eight methods (`connect`, `newSession`, `loadSession`, `listSessions`, `currentSessionId`, `prompt`, `cancelPrompt`, `dispose`) that map one-to-one to responsibilities inside `AcpSessionControllerImpl` (~320 lines). The implementation bundles process lifecycle, stdio transport, protocol initialization, auth orchestration, prompt job management, stderr monitoring, session-ready synchronization, and `ClientOperationsFactory` wiring — yet callers see every internal step as a separate public operation.

Only one production adapter exists (`AcpSessionControllerImpl`). `AcpAgentEditor` injects a factory `(AcpSessionListener) -> AcpSessionController`, but tests use a hand-rolled `RecordingSessionController` that stubs all eight methods. This is a **hypothetical seam** (one adapter): the interface documents implementation structure rather than providing leverage.

Meanwhile, `AcpAgentEditor` re-implements session-resume orchestration (`openSessionFromResumePlan`, `pickSessionOrStartFresh`, `pickSessionFromCandidates`) on top of the granular controller API — logic that belongs with session lifecycle, not UI. The editor must understand protocol ordering (`connect` → `newSession`/`loadSession`/`listSessions`) and error-recovery paths that the controller already knows how to perform.

This shallow seam increases cognitive load for maintainers and AI navigators: understanding ACP startup requires reading both `AcpAgentEditor` and `AcpSessionControllerImpl`, with no deep module hiding transport/auth/session complexity behind a small editor-facing API.

## Solution

Deepen the `AcpSessionController` module by shrinking the editor-facing interface to match caller intent, extracting transport and lifecycle into internal sub-modules with real seams, and moving session-resume orchestration from `AcpAgentEditor` into the controller layer.

### Editor-facing interface (after)

Replace the eight-method surface with four operations aligned to `AcpAgentEditor`'s actual lifecycle:

| Method | Replaces | Caller need |
|--------|----------|-------------|
| `suspend fun start(request: AcpSessionStartRequest): AcpSessionStartResult` | `connect` + `newSession`/`loadSession`/`listSessions` + resume orchestration | Launch agent, authenticate, open or resume session in one call |
| `suspend fun prompt(text: String)` | `prompt` | Send user input |
| `suspend fun cancelPrompt()` | `cancelPrompt` | Cancel in-flight prompt on dispose |
| `fun dispose()` | `dispose` | Tear down process, transport, and session |

`AcpSessionStartRequest` bundles what `connect` already accepts plus the resume intent currently owned by the editor:

```kotlin
data class AcpSessionStartRequest(
    val launchPlan: AcpLaunchPlan,
    val editorContext: AcpEditorContext,
    val resumePlan: LaunchResumePlan?,
    val sessionPicker: SessionPicker,  // suspend (List<SessionSummary>) -> String?
)
```

`AcpSessionStartResult` returns the opened session id (replaces `currentSessionId()` polling) and a human-readable status line for the transcript:

```kotlin
data class AcpSessionStartResult(
    val sessionId: String,
    val statusMessage: String,  // e.g. "Connected.", "Resumed session abc.", "Started a new session."
)
```

`SessionPicker` is a functional seam for the EDT dialog (`SessionPickerDialog.show`) so the controller can call `listSessions` and present choices without importing Swing.

### What moves behind the smaller interface (internal implementation detail)

These concerns stay in the `acp/` slice but become **package-private** modules, not public interface methods:

| Concern | Current location | Proposed internal module |
|---------|------------------|--------------------------|
| Process spawn, stdio pipes, destroy | `AcpSessionControllerImpl.connect`, `disposeTransportOnly` | `AcpProcessTransport` |
| `Protocol` / `StdioTransport` lifecycle | `connect`, `disposeTransportOnly` | `AcpProcessTransport` |
| Stderr line monitoring | `monitorStderr` | `AcpProcessTransport` |
| Process exit monitoring | `exitJob` in `connect` | `AcpProcessTransport` |
| Protocol start, client init, capabilities | `initializeConnectedClient` | `AcpConnectionBootstrap` |
| Auth method selection and retry | `AuthFlowCoordinator` (already extracted) | `AcpConnectionBootstrap` calls existing coordinator |
| `sessionReady` `CompletableDeferred` | `openSession`, `awaitOpenSession` | `AcpSessionLifecycle` |
| `newSession` / `loadSession` / `listSessions` | public methods + `openSession` | `AcpSessionLifecycle` |
| `ClientOperationsFactory` → `AcpClientSessionOperationsImpl` | `openSession` | `AcpSessionLifecycle` |
| Resume-plan branching (load, pick, new) | `AcpAgentEditor.openSessionFromResumePlan` et al. | `AcpSessionLifecycle.startSession(resumePlan, picker)` |
| Prompt job launch, join, cancel | `prompt`, `cancelPrompt` | `AcpPromptExecutor` |
| `Event` → `StructuredUpdate` dispatch | `handlePromptEvent` | `AcpPromptExecutor` (delegates to existing `AcpPromptEventDispatcher`) |
| `editorContext` / `launchPlan` field retention | scattered fields | owned by `AcpSessionLifecycle` after bootstrap |

`AcpSessionControllerImpl` becomes a thin coordinator composing the four internal modules (~60–80 lines), satisfying the shrunk public interface.

### Real seams (two adapters each)

Per the **one adapter = hypothetical seam, two adapters = real seam** principle:

| Seam | Production adapter | Test / alternate adapter | Justification |
|------|-------------------|--------------------------|---------------|
| `AcpProcessTransport` | `ProcessStdioTransport` (ProcessBuilder + deprecated `StdioTransport` constructor) | `InMemoryTransport` (Kotlin SDK test utilities or protocol-over-channels) | Enables transport/integration tests without spawning subprocesses; second adapter makes the seam real |
| `SessionPicker` | `EdtSessionPicker` (wraps `SessionPickerDialog.show`) | `FirstSessionPicker` / `NullSessionPicker` (deterministic test doubles) | Editor already needs picker injection; controller resume logic becomes testable without UI |
| `AcpSessionController` (editor-facing) | `AcpSessionControllerImpl` | `RecordingSessionController` (updated to match new interface) | Stays hypothetical until a second production adapter is needed (e.g. headless agent runner); acceptable because internal seams provide test leverage |

`AuthFlowCoordinator` remains a separate class (already a real extraction). `AcpClientSessionOperationsImpl` remains the `ClientSessionOperations` adapter — no change.

### Caller migration (`AcpAgentEditor`)

After deepening, `AcpAgentEditor.launchAndConnect` simplifies to:

1. Build `AcpLaunchPlan` and `AcpEditorContext` (unchanged).
2. Call `sessionController.start(AcpSessionStartRequest(...))`.
3. On success: append `result.statusMessage`, persist `result.sessionId` to worktree state, enable prompt bar.
4. On failure: append error, disable prompt bar (unchanged error handling).

Removed from editor: `openSessionFromResumePlan`, `pickSessionOrStartFresh`, `pickSessionFromCandidates`, `persistCurrentSessionId` (replaced by `result.sessionId` from `start`). `persistBoundSessionId` stays — worktree persistence is an editor/worktree concern.

`prompt`, `cancelPrompt`, `dispose` call sites remain one-liners.

## User Stories

1. As a developer reading `AcpAgentEditor`, I want session startup to be a single `start()` call, so that I don't need to trace five methods and three private helpers to understand how an ACP tab opens.
2. As a developer adding a new resume strategy (e.g. auto-resume last session), I want to change session lifecycle in one module, so that UI and protocol ordering don't drift apart.
3. As a developer writing transport tests, I want to exercise protocol init and auth without spawning a subprocess, so that CI can validate connection logic deterministically.
4. As a developer testing session-picker fallback (load fails → pick → new), I want to inject a fake `SessionPicker`, so that resume flows are unit-testable without Swing.
5. As a maintainer fixing prompt cancellation bugs, I want prompt job logic isolated in `AcpPromptExecutor`, so that changes don't risk process-lifecycle regressions.
6. As an AI agent navigating the codebase, I want the public `AcpSessionController` interface to describe editor intent (start, prompt, cancel, dispose), so that I read ~15 lines instead of ~320 to understand the seam.
7. As a developer disposing an editor tab, I want `dispose()` to remain synchronous and idempotent, so that IntelliJ `Disposable` contract is preserved.
8. As a user resuming a stored ACP session, I want identical behavior after refactoring, so that load/pick/new flows and transcript messages are unchanged.

## Implementation Decisions

### Ownership

- **Vertical slice:** `acp/` — all affected files live in `com.oaalto.agent.acp` and `com.oaalto.agent.acp.auth` (auth coordinator unchanged).
- **No changes to** `pty/`, `worktree/` (except call-site simplification in `AcpAgentEditor`), or `settings/`.

### Modules to create / modify

| File | Action |
|------|--------|
| `AcpSessionController.kt` | Replace eight-method interface with four-method interface + `AcpSessionStartRequest` / `AcpSessionStartResult` / `SessionPicker` |
| `AcpSessionControllerImpl.kt` | Thin coordinator composing internal modules |
| `AcpProcessTransport.kt` | **New** — process + stdio + stderr + exit monitoring |
| `InMemoryAcpTransport.kt` (or SDK test util wrapper) | **New** — test adapter |
| `AcpConnectionBootstrap.kt` | **New** — protocol start, client init, auth via `AuthFlowCoordinator` |
| `AcpSessionLifecycle.kt` | **New** — session open/load/list, resume orchestration, operations factory |
| `AcpPromptExecutor.kt` | **New** — prompt job lifecycle, delegates to `AcpPromptEventDispatcher` |
| `AcpAgentEditor.kt` | Simplify startup; inject `SessionPicker`; remove resume helpers |
| `AcpSessionControllerTest.kt` | Update fake; add tests for resume flows via injected picker |
| `AcpProcessTransportTest.kt` | **New** — transport adapter tests (optional first iteration) |

### `AcpEditorContext` — no structural change

`AcpEditorContext` remains the dependency bundle passed at start time. It already carries everything the controller needs: `listener`, `shellPaneHost`, `authPromptUi`, `permissionPromptUi`, `terminalSessionRegistry`, `scopeRoot`. Deepening does not widen this type; it stops being stored across multiple public method calls and is held internally by `AcpSessionLifecycle` after `start()`.

### `AuthFlowCoordinator` — unchanged public surface

`AuthFlowCoordinator` is already a meaningful extraction (terminal auth, OAuth link, API key prompts). `AcpConnectionBootstrap` calls it during `start()` — same as today's `initializeConnectedClient`. No auth behavior change.

### Session-ready synchronization

The `sessionReady` `CompletableDeferred` pattern moves into `AcpSessionLifecycle`. `AcpPromptExecutor.prompt()` awaits session readiness before sending — same semantics as today's `awaitOpenSession()`. `start()` does not return until the session is open (or fails), so the editor no longer races `connect` against `newSession`.

### StdioTransport deprecation

`AcpProcessTransport` owns the `@Suppress("DEPRECATION")` on the `StdioTransport` Source/Sink constructor. Comment and tracking note move with it; no new transport API introduced in this PRD.

### Error recovery during resume

Resume fallback logic currently in `AcpAgentEditor` moves verbatim into `AcpSessionLifecycle.startSession`:

- `AcpLoad` failure → list + pick dialog → new session on cancel/empty
- `AcpPickSession` with candidates → pick dialog
- `AcpPickSession` empty / `AcpNewSession` / null → new session
- Picker load failure → new session with error surfaced via `AcpSessionStartResult` or thrown exception (match current transcript error behavior)

### ADR alignment

- **ADR 0001** (custom ACP client): No UX model change. Resume flows and transcript messages preserved. Aligned.
- **ADR 0002** (Kotlin ACP SDK): Internal modules still use SDK `Client`, `Protocol`, `StdioTransport`. `InMemoryTransport` test adapter uses SDK test utilities where available. Aligned.
- **ADR 0003** (per-project agent selection): No settings changes. Aligned.

## Testing Decisions

### What to test

- **Editor-facing contract:** `start()` with each `LaunchResumePlan` variant returns correct `sessionId` and status message.
- **Resume fallback:** load failure → picker returns null → new session created; picker returns id → load attempted.
- **Prompt lifecycle:** `dispose()` and `cancelPrompt()` cancel active prompt work (existing test, updated interface).
- **Transport seam:** `InMemoryTransport` adapter can complete protocol init without `ProcessBuilder` (smoke test).
- **No unit tests** for `ProcessBuilder` destroy timeouts or stderr line parsing beyond one happy-path smoke — those are implementation details of `AcpProcessTransport`.

### Modules to test

- **`AcpSessionLifecycle`** — resume orchestration with fake `Client` and injected `SessionPicker`.
- **`AcpPromptExecutor`** — prompt job cancel on `dispose` / `cancelPrompt`.
- **`AcpSessionControllerTest`** — update `RecordingSessionController` to new four-method interface.

### Prior art

- `AcpSessionControllerTest` already uses a recording fake — extend pattern for `start()` result assertions.
- `AuthFlowCoordinator` can be tested independently with mocked `Client` and `AuthPromptUi` (future; not required for this deepening).
- Kotlin SDK test utilities (per ADR 0002 consequences) for `InMemoryTransport`.

### Verification

`./gradlew qualityGate` passes after implementation.

## Out of Scope

- Changing ACP protocol behavior, capabilities negotiation, or auth method support.
- Replacing deprecated `StdioTransport` constructor (tracked separately; only relocation in this work).
- Headless / second production `AcpSessionController` adapter (e.g. CLI-only runner).
- Moving worktree persistence (`AgentWorktreeStateService.setAcpSessionId`) into the controller — stays in editor.
- Refactoring `AcpClientSessionOperationsImpl`, `AcpPromptEventDispatcher`, or transcript rendering.
- PTY Passthrough mode — unrelated slice.
- Changing `AcpEditorContext` fields or `PermissionCoordinator` wiring.

## Further Notes

- **Depth metric:** public interface drops from 8 methods to 4; implementation detail grows across 4 internal modules but each has a single responsibility and at least one testable seam.
- **Deletion test:** removing `AcpSessionController` would scatter process lifecycle, auth ordering, session resume, and prompt jobs across `AcpAgentEditor` — confirming this module earns its keep once deepened.
- **Hypothetical vs real:** the editor-facing `AcpSessionController` seam may remain single-adapter; leverage comes from internal `AcpProcessTransport` and `SessionPicker` seams with two adapters each.
- Implementers should update `CHANGELOG.md` under `### Changed` when the code ships.
- Related wiki page: [ACP client subsystem](../../wiki/subsystems/acp-client.md) — update session lifecycle section after implementation.
- `AcpAgentEditor.sessionControllerFactory` injection point is preserved; signature becomes `(AcpSessionListener) -> AcpSessionController` with the new interface.
