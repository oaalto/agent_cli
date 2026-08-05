## Status

ready-for-agent

## Problem Statement

`acp-session-controller-deepening` moved transport, auth bootstrap, resume branching, and prompt execution behind `AcpSessionController.start` / `prompt` / `cancelPrompt` / `dispose`. `AcpAgentEditor` is no longer a protocol god object — but it remains a **fat UI coordinator** (~400 lines, ~15 injected collaborators) that still owns concerns better placed at orchestration seams:

| Concern still in editor | Better owner |
| --- | --- |
| `persistWorktreeSessionId` post `start` | `AcpSessionResumeOrchestrator` (via `worktree-orchestrator-production-wiring`) |
| `TranscriptFinalizePolicy.onPromptStarting()` on prompt bar submit | Already in `AcpPromptExecutor` — dual call-site risk |
| Inline EDT helpers (`runOnEdt`, `onEdtAsync`, `snapshotBlocksOnEdt`) | Shared EDT seam or lifecycle module |
| Session kickoff assembly (`buildLaunchPlan` + `start` + `applyStartResult`) | Thin delegate to controller; editor wires UI only |

Understanding ACP editor startup still requires reading UI coordination, finalize policy touchpoints, and worktree side effects in one file — low **leverage** for a module whose interface should be "host Transcript + prompt bar + shell pane."

**Deletion test:** Shrinking the editor to layout + listener routing + prompt-bar callback would concentrate session lifecycle knowledge in `AcpSessionController` stack; removing that stack would scatter complexity across future entry points — the controller earns its depth, the editor does not.

## Solution

Finish the migration started in `acp-session-controller-deepening` slice 07: reduce `AcpAgentEditor` to a **thin view adapter** that:

1. Builds `AcpEditorLayout` and wires panel callbacks.
2. Calls `sessionController.start(request)` with fully populated `AcpSessionStartRequest` (including `worktreeRecordId`, `resumePlan`, `sessionPicker`).
3. Maps `AcpSessionStartResult` to **Transcript** status and enables prompt bar.
4. Routes `AcpSessionListener` callbacks to `TranscriptViewController` and footer.
5. Delegates prompt submit to `sessionController.prompt` (finalize policy owned by executor path only).
6. Disposes panels and controller on close.

Extract or share EDT bridging only if duplicate helpers block the thinning; prefer deletion over new abstractions.

### Prerequisite

`worktree-orchestrator-production-wiring` must ship first (or in the same PR) so `persistWorktreeSessionId` removal does not drop binding behaviour.

## User Stories

1. As a developer opening `AcpAgentEditor`, I want session open/resume/persist logic absent from the file, so that I read UI wiring only.
2. As a developer fixing a prompt-finalize ordering bug, I want `TranscriptFinalizePolicy` invoked from one orchestration path (`AcpPromptExecutor`), so that editor and executor cannot diverge.
3. As a developer adding a second ACP editor entry point, I want to reuse `AcpSessionController.start` without copying editor private methods, so that new surfaces stay thin.
4. As a user submitting a prompt, I want identical finalize-and-send behaviour to today, so that thinning does not change streaming UX.
5. As a user seeing permission or auth prompts, I want suspend UI callbacks to keep working, so that EDT bridging removal does not break prompts.
6. As a developer writing editor tests, I want to mock `AcpSessionController` factory and assert layout + callback wiring, so that protocol tests stay in controller/lifecycle tests.
7. As a user resuming a worktree session, I want transcript restore and status messages unchanged, so that `applyStartResult` simplification is behaviour-preserving.
8. As a developer, I want `AcpEditorLayoutTest` to remain the primary editor unit test, so that layout regressions are caught without heavyweight UI harness.
9. As a user disposing an editor tab, I want prompt cancel and panel cleanup unchanged, so that dispose path stays in editor as UI concern.
10. As an AI navigator, I want `AcpAgentEditor` classified as view adapter in wiki/graphify, so that session loop docs point to controller stack.

## Implementation Decisions

### Modules to modify

- `AcpAgentEditor` — remove `persistWorktreeSessionId`, duplicate finalize-on-prompt if executor already covers it, collapse `startSession`/`applyStartResult` to thin delegate, delete private resume helpers if any remain.
- `AcpPromptExecutor` — confirm single owner of `TranscriptFinalizePolicy.onPromptStarting()` for user-initiated prompts; document if editor must call `onPromptStarting` before echo (prefer executor-only).
- `AcpSessionStartRequest` — ensure all fields needed by editor are set in one builder at `startSession` call site.

### Modules unchanged

- `AcpSessionControllerImpl`, `AcpSessionLifecycle`, `AcpPromptExecutor`, `AcpConnectionBootstrap` — already own protocol phases.
- `AcpEditorLayout` — layout composition stays extracted.
- `SessionTranscriptCoordinator` — bind/restore callbacks remain wired from editor `applyStartResult` (or move to lifecycle if cleaner — optional, not required).

### Seam design

| Seam | Role after thinning |
| --- | --- |
| `(AcpSessionListener) -> AcpSessionController` | Editor injects factory; tests use `RecordingSessionController` |
| `AcpEditorLayout` | Pure layout; no session logic |
| `TranscriptViewController` | View state; editor routes listener events |

**Target line budget:** Editor under ~250 lines, mostly wiring.

### ADR alignment

- **ADR 0001:** Editor stays ACP Client UI host; no AI Chat delegation.
- **ADR 0007:** Finalize policy stays orchestration-layer; remove duplicate editor call sites.

## Testing Decisions

### What to test

- **Behaviour preservation:** Existing `AcpEditorLayoutTest`, `AcpSessionControllerTest`, `AcpSessionResumeOrchestratorTest`, and transcript panel harness tests must pass unchanged.
- **Optional:** `AcpAgentEditor` test with mocked controller factory verifying `start` called with expected `AcpSessionStartRequest` fields — lower priority than controller/lifecycle tests.

### Modules to test

- No new tests required if quality gate green and manual smoke confirms prompt/finalize ordering.
- If finalize dual-call removed: add assertion in `AcpPromptExecutorTest` (if created) or extend existing prompt tests.

### Prior art

- `AcpEditorLayoutTest` — layout-only editor test pattern.
- `RecordingSessionController` — stub controller for listener tests.

### Verification

`./gradlew qualityGate` passes; manual smoke: open ACP editor, resume worktree, submit prompt, verify streaming finalizes correctly.

## Out of Scope

- Further shrinking `AcpSessionController` public API.
- Transcript panel or row adapter changes.
- **Session transcript file** coordinator moves (see `session-transcript-coordinator-deepening`).
- Headless ACP entry points (future consumers of thinned editor pattern).
- Koog / custom agent (4.0).

## Further Notes

- **Recommendation strength:** Strong — completes editor/controller separation from architecture review candidate #1.
- **Depends on:** `worktree-orchestrator-production-wiring`.
- **Related slice doc:** `acp-session-controller-deepening/07-editor-migration.md`.
