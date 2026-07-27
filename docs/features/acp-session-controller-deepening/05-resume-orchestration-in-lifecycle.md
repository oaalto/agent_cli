# 05 — Resume orchestration in session lifecycle

**Parent:** `prd.md`

**What to build:** Session-resume branching currently in `AcpAgentEditor` moves into `AcpSessionLifecycle`, driven by an injected `SessionPicker` seam so resume flows are testable without Swing. Introduce `SessionPicker` as a suspend functional type, a production `EdtSessionPicker` wrapping the existing session-picker dialog, and deterministic test doubles (`FirstSessionPicker`, `NullSessionPicker`, or equivalent). All `LaunchResumePlan` variants and fallback paths (load failure → list → pick → new session; empty candidates → new session; picker cancel → new session; picker load failure → new session with error surfaced) execute inside lifecycle with the same transcript messages and session outcomes as today.

**Conceptual note:** This overlaps the *execution* concerns described in the sibling `acp-session-resume-orchestration` PRD, but this ticket is **self-contained for controller deepening only** — resume branching lives in `AcpSessionLifecycle`, not a new worktree-level module. Worktree session-ID persistence (`AgentWorktreeStateService.setAcpSessionId`) stays in the editor; lifecycle returns the opened `sessionId` and status text for the caller to persist.

**Blocked by:** 03 — Session lifecycle core

**Status:** done

- [x] `SessionPicker` type and production/test adapters exist; controller resume logic calls the picker without importing Swing.
- [x] `AcpSessionLifecycle.startSession(resumePlan, picker)` implements all branches currently in `openSessionFromResumePlan`, `pickSessionOrStartFresh`, and `pickSessionFromCandidates` with matching fallback behavior.
- [x] Unit tests cover each `LaunchResumePlan` variant and key fallbacks (load failure → picker returns null → new session; picker returns id → load attempted) using injected picker doubles and a fake client.
- [x] Editor still owns resume orchestration call sites until ticket 07; this ticket may temporarily expose lifecycle resume via existing granular methods or an internal entry point — behavior must be verifiable in tests.
- [x] `./gradlew qualityGate` passes.
