# 01 — Prefactor LaunchResumePlan ACP resolve variant

**Parent:** `prd.md`

**What to build:** Replace the misleading `AcpPickSession(candidates)` plan variant with an honest `AcpResolveSession` shape that reflects runtime behavior — session listing always happens post-connect, never at plan time. Update plan selection and the ACP editor's `when` branch so no code path depends on pre-populated candidate lists.

**Blocked by:** None — can start immediately

**Status:** done

- [x] `LaunchResumePlan` exposes `AcpResolveSession` (data object or equivalent with no candidate list); the old `AcpPickSession` variant is removed
- [x] `AcpResumeStrategy` returns `AcpResolveSession` when resume is true and the **Worktree** record has no stored `acpSessionId`; configuration-mismatch and `resume=false` behavior unchanged (`AcpNewSession`)
- [x] `AcpAgentEditor` session-open branching handles `AcpResolveSession` by delegating to the existing list-and-pick flow (no branch on pre-filled candidates)
- [x] `AcpResumeStrategyTest` and any other affected tests are updated; `./gradlew qualityGate` passes with no behavior change for end users
