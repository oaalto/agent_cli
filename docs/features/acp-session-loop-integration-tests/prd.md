## Status

implemented

## Problem Statement

`acp-session-controller-deepening` extracted `AcpProcessTransport`, `AcpConnectionBootstrap`, `AcpSessionLifecycle`, and `AcpPromptExecutor` behind `AcpSessionControllerImpl.start` / `prompt`. Unit tests cover pieces in isolation (`AcpSessionResumeOrchestratorTest`, `InMemoryAcpTransportTest` smoke, `AcpSessionControllerTest` dispose stub) — but **no test drives the full connect → bootstrap → resume → prompt chain** through the public controller interface.

`InMemoryAcpTransport` exists as the second `AcpProcessTransport` adapter (real seam per deepening PRD) yet is not wired into an integration harness for `AcpSessionControllerImpl.start()`. Gaps:

| Phase | Module | Test coverage |
| --- | --- | --- |
| Transport connect/dispose | `ProcessStdioTransport`, `InMemoryAcpTransport` | Smoke only on in-memory |
| Protocol init + auth | `AcpConnectionBootstrap`, `AuthFlowCoordinator` | None |
| Session open/resume | `AcpSessionLifecycle` + orchestrator | Orchestrator only; not via `start()` |
| Prompt collect loop | `AcpPromptExecutor` + `TranscriptEventIngestion` | None at executor level |
| Full `start()` | `AcpSessionControllerImpl` | None |

Regressions in phase ordering (e.g. finalize before bind, dispose before cancel) require manual editor smoke or subprocess tests. The **Transcript** panel has `TranscriptPanelTestHarness`; the session loop has no equivalent.

## Solution

Add an **ACP session loop integration test harness** that drives `AcpSessionControllerImpl` (or test subclass) through `InMemoryAcpTransport` with fake/minimal ACP agent behaviour — no subprocess, no Swing.

### Harness capabilities (v1)

1. `start(AcpSessionStartRequest)` completes with expected `AcpSessionStartResult` when in-memory agent responds to `initialize` + `session/new`.
2. Resume path: `LaunchResumePlan.AcpLoad` with fake agent accepting `session/load`.
3. `prompt(text)` delivers scripted `SessionUpdate` events to listener; assert `StructuredUpdate` sequence or recording listener output.
4. `dispose()` tears down without leak (transport disposed, jobs cancelled).

Optional v1 stretch: auth-required path with `AuthFlowCoordinator` stub.

### Seam under test

**Highest seam:** `AcpSessionController` public interface — one adapter (`AcpSessionControllerImpl`) + in-memory transport + recording listener. Internal modules stay package-private; harness asserts external behaviour only.

## User Stories

1. As a developer changing `AcpSessionControllerImpl.start` phase order, I want an integration test to fail if bootstrap runs after resume, so that ordering regressions are caught in CI.
2. As a developer adding a new `SessionUpdate` variant, I want prompt-loop tests to assert ingestion output, so that executor wiring stays correct.
3. As a developer refactoring `AcpConnectionBootstrap`, I want initialize/auth covered by in-memory transport tests, so that subprocess spawn is not required for protocol glue tests.
4. As a developer fixing dispose races, I want `dispose()` during in-flight `prompt` tested, so that cancel paths are guarded.
5. As a developer onboarding to ACP slice, I want a readable harness test as documentation of the session loop phases, so that architecture matches ADR 0001 stack diagram.
6. As a developer, I want `InMemoryAcpTransport` exercised beyond connect smoke, so that the second transport adapter earns its seam status.
7. As a user, I want no change to production behaviour, so that harness is test-only.
8. As a developer extending finalize policy, I want prompt-start integration to assert `FinalizeAgentStream` ordering when scripted events arrive, so that ADR 0007 invariants hold at loop level.
9. As a developer, I want fake `AcpSessionOperations` or scripted agent JSON-RPC aligned with orchestrator fakes, so that resume branches compose in one test class.
10. As a CI maintainer, I want harness tests under `src/test` with no IntelliJ UI fixture, so that `qualityGate` remains headless.

## Implementation Decisions

### Modules to create

- `AcpSessionLoopTestHarness` (test utility) — builds controller with `InMemoryAcpTransport`, recording listener, fake picker, configurable agent script.
- `AcpSessionControllerIntegrationTest` — scenarios: new session, load session, prompt events, dispose.

### Modules to modify (minimal)

- `InMemoryAcpTransport` — extend only if harness needs additional hooks (scripted responses, capture outbound messages).
- `AcpSessionControllerImpl` — prefer constructor injection for transport factory if not already testable; avoid production behaviour change.

### Test agent script (conceptual)

Declarative sequence: on `initialize` → capabilities response; on `session/new` → session id; on `session/prompt` → emit N `agent_message_chunk` + completion event. Keeps tests deterministic without real Cursor/Codex binary.

### Phase diagram under test

```
start → transport.connect → bootstrap.initialize → lifecycle.bindClient
     → lifecycle.startSession (orchestrator) → AcpSessionStartResult
prompt → executor → ingestion → listener.onStructuredUpdate
dispose → cancelPrompt → transport.dispose
```

### ADR alignment

- **ADR 0001:** Custom in-process ACP client; harness validates client stack without AI Chat.
- **ADR 0007:** Finalize prelude exercised via real ingestion path in prompt tests.

## Testing Decisions

### What to test (external behaviour)

- `start` returns session id and status message for new-session plan.
- `start` with `AcpLoad` plan calls load path (assert agent received `session/load` or recording ops).
- `prompt` invokes listener with expected structured updates for scripted chunks.
- `dispose` after `start` does not throw; second `start` blocked or throws per current contract.
- Optional: prompt cancel mid-stream leaves listener in consistent state.

### Modules to test

- `AcpSessionControllerImpl` via public interface only.
- Do **not** unit-test private `monitorStderr` unless harness exposes stderr script.

### Prior art

- `InMemoryAcpTransportTest` — transport smoke.
- `AcpSessionResumeOrchestratorTest` — fake ops pattern; reuse fakes inside harness.
- `TranscriptPanelTestHarness` — panel-level integration pattern (higher UI layer).
- Kotlin ACP SDK test utilities if present in dependencies.

### Verification

`./gradlew qualityGate` passes; new test class runs in CI without external agent binary.

## Out of Scope

- `ProcessStdioTransport` subprocess integration tests (flaky CI; manual or separate job).
- Full `AcpAgentEditor` UI integration (covered by layout/panel harness tests).
- Auth UI (`AuthPromptPanel`) visual tests — stub auth coordinator in harness.
- MCP bridging integration.
- Performance/load testing of prompt stream.
- Replacing orchestrator unit tests — harness complements, does not subsume.

## Further Notes

- **Recommendation strength:** Worth exploring — high confidence value; moderate effort to script in-memory agent.
- **Related:** architecture review candidate #6; completes the "two adapters = real seam" story for transport.
- **Enables:** Safer refactors for `worktree-orchestrator-production-wiring` and `acp-agent-editor-thinning`.
