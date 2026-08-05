## Status

ready-for-agent

## Problem Statement

`AcpSessionResumeOrchestrator` was introduced to own ACP session resume execution and the **Worktree** binding invariant: when a session opens successfully and a worktree record is bound, persist `acpSessionId` via `WorktreeSessionBinder`. The orchestrator is fully unit-tested with fake ports — but **production wiring bypasses it**.

Today the binding invariant is split across two layers:

| Layer | Binder | `worktreeRecordId` | When persist runs |
| --- | --- | --- | --- |
| `AcpSessionLifecycle.startSession` | `NoOpWorktreeSessionBinder` | always `null` | never |
| `AcpAgentEditor.applyStartResult` | `WorktreeSessionBinderImpl` | from virtual file | after `controller.start` returns |

The orchestrator's tested path and the runtime path diverge. A developer reading `AcpSessionResumeOrchestrator` KDoc sees the binding invariant documented in one module, then discovers production persistence re-implemented in the editor — violating **locality** and the vertical-slice intent from ADR 0001 (worktree binding should not leak into transcript UI code).

**Deletion test on `WorktreeSessionBinderImpl`:** Removing it would force `AgentWorktreeStateService.setAcpSessionId` calls to reappear in the editor — confirming the orchestrator seam is real but not yet the single production entry for binding.

## Solution

Thread `worktreeRecordId` from `AgentLaunchContext` through `AcpSessionStartRequest` into `AcpSessionLifecycle.startSession`, inject the real `WorktreeSessionBinder` adapter, and **remove** duplicate `persistWorktreeSessionId` from `AcpAgentEditor`.

After this change, every successful ACP session open for a bound **Worktree** persists exclusively through the orchestrator — the same code path exercised in `AcpSessionResumeOrchestratorTest`.

### Target seam (one production entry)

```
AcpAgentEditor.startSession
  └─ AcpSessionController.start(AcpSessionStartRequest { worktreeRecordId, resumePlan, … })
        └─ AcpSessionLifecycle.startSession
              └─ AcpSessionResumeOrchestrator.openSession(…, worktreeRecordId, WorktreeSessionBinderImpl)
                    └─ persistIfNeeded → WorktreeSessionBinder.persistSessionId
```

The editor maps `AcpSessionStartResult` to **Transcript** status lines only — no persistence calls.

## User Stories

1. As a user resuming a **Worktree** with a stored `acpSessionId`, I want the session ID persisted through the same code path that handles load/pick/new, so that binding cannot drift between orchestrator and editor.
2. As a user whose ACP session was loaded via the session picker, I want the chosen session ID bound to the worktree record, so that the next resume uses the correct ID.
3. As a user starting a fresh ACP session in a worktree, I want the new session ID persisted immediately after open, so that closing and reopening the editor resumes correctly.
4. As a user launching ACP in the current project (no worktree record), I want session open to succeed without persistence errors, so that non-worktree launches are unchanged.
5. As a developer fixing a worktree binding bug, I want one module (`AcpSessionResumeOrchestrator`) to own persist-after-open, so that I do not hunt editor and lifecycle call sites.
6. As a developer reading resume orchestration tests, I want production wiring to match the tested orchestrator contract, so that CI coverage reflects runtime behaviour.
7. As a developer adding a new ACP entry point, I want `worktreeRecordId` on `AcpSessionStartRequest`, so that binding works without re-implementing `setAcpSessionId`.
8. As a developer, I want `NoOpWorktreeSessionBinder` removed from the production lifecycle path, so that the no-op adapter is test-only (or deleted if unused).
9. As a user whose stored session ID fails to load, I want fallback (list + picker + new session) to still persist the final session ID, so that recovery paths bind correctly.
10. As a user who changed **Agent configuration** on a worktree, I want the new session ID from a fresh open to replace the stale ID, so that configuration mismatch handling at plan time is reflected at persist time.

## Implementation Decisions

### Modules to modify

- `AcpSessionStartRequest` — add `worktreeRecordId: String?` field sourced from `AgentLaunchContext.worktreeRecordId` (or equivalent on virtual file).
- `AcpSessionLifecycle.startSession` — replace `NoOpWorktreeSessionBinder` with injected `WorktreeSessionBinder`; pass non-null `worktreeRecordId` from request.
- `AcpSessionControllerImpl` — thread `worktreeRecordId` from request into lifecycle; wire default `WorktreeSessionBinderImpl` at composition root.
- `AcpAgentEditor` — remove `persistWorktreeSessionId` and `WorktreeSessionBinderImpl` usage; pass `worktreeRecordId` into `AcpSessionStartRequest`.

### Modules unchanged (behaviour preserved)

- `AcpSessionResumeOrchestrator` — logic unchanged; production finally calls it with real binder and record ID.
- `WorktreeSessionBinderImpl` — remains thin adapter to `AgentWorktreeStateService`.
- `AcpResumeStrategy` / `WorktreeLaunchCoordinator` — plan selection unchanged.
- `AgentWorktreeStateService.setAcpSessionId` — persistence API unchanged.

### Seam design

| Port | Production adapter | Test adapter |
| --- | --- | --- |
| `WorktreeSessionBinder` | `WorktreeSessionBinderImpl` | `FakeWorktreeSessionBinder` (existing in orchestrator tests) |

**Test seam:** `AcpSessionLifecycle` or `AcpSessionControllerImpl` integration test with fake binder — assert `persistSessionId` called once after successful `start` when `worktreeRecordId` is non-null.

### ADR alignment

- **ADR 0001:** Strengthens worktree ↔ ACP session binding locality; no UX change.
- **ADR 0003:** Configuration mismatch still encoded in `LaunchResumePlan` at plan stage; orchestrator does not re-check.

## Testing Decisions

### What to test

- **External behaviour:** After `AcpSessionController.start` with non-null `worktreeRecordId`, `WorktreeSessionBinder.persistSessionId` is called exactly once with the opened session ID.
- **Null record ID:** No persist call when `worktreeRecordId == null`.
- **Resume branches:** Load success, picker fallback, and new-session paths all persist via orchestrator (extend existing `AcpSessionResumeOrchestratorTest` coverage is sufficient for branch logic; add one lifecycle/controller wiring test for production glue).

### Modules to test

- **New or extended:** `AcpSessionLifecycleTest` or `AcpSessionControllerImplTest` — wiring test with `FakeWorktreeSessionBinder` and `InMemoryAcpTransport` / fake session ops.
- **Keep:** `AcpSessionResumeOrchestratorTest` — no behaviour change expected.

### Prior art

- `AcpSessionResumeOrchestratorTest` — fake binder records persist calls.
- `AgentWorktreeStateServiceTest` — persistence semantics for `setAcpSessionId`.

### Verification

`./gradlew qualityGate` passes after implementation.

## Out of Scope

- Changes to `AcpResumeStrategy` plan selection rules.
- PTY Passthrough resume or `PtyResumeStrategy`.
- Persisting session IDs outside worktree binding (current-project launches with `worktreeRecordId == null`).
- `AcpAgentEditor` UI thinning beyond removing persist (see `acp-agent-editor-thinning` PRD).
- Session picker dialog changes.

## Further Notes

- **Recommendation strength:** Strong — smallest diff with highest correctness payoff; closes gap between tested orchestrator and production.
- **Root cause:** Resume orchestration extraction (implemented) left lifecycle wired with `NoOpWorktreeSessionBinder` while editor retained post-hoc persist.
- **Dependency:** Builds on completed `acp-session-resume-orchestration` and `acp-session-controller-deepening` features.
- **Blocks:** `acp-agent-editor-thinning` should ship after or with this feature to avoid transient double-persist.
