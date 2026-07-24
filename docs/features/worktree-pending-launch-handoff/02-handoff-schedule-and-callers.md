# 02 — Handoff schedule launch and thin callers

**Parent:** `prd.md`

**What to build:** Extend `WorktreePendingLaunchHandoff` with `scheduleLaunch`, which enqueues pending-launch intent, opens the worktree project, and rolls back the enqueue when open fails. Replace the duplicated enqueue/open/rollback orchestration in `RunAgentInNewWorktreeAction` and `OpenOrResumeWorktreeAction` with a single `scheduleLaunch` call each. Unify `lastUsedAtEpochMs` touch so it happens once at handoff completion (after successful editor open), not before enqueue in the open/resume action.

**Blocked by:** 01 — Handoff deep module: consume path

**Status:** ready-for-agent

- [ ] `WorktreePendingLaunchHandoff.scheduleLaunch(originatingProject, worktreePath, configuration, resume)` enqueues intent, calls `openWorktreeProject`, and consumes (rolls back) the pending launch before returning error when open fails
- [ ] `RunAgentInNewWorktreeAction` and `OpenOrResumeWorktreeAction` delegate scheduling to `scheduleLaunch`; no inline enqueue/open/rollback remains in action classes
- [ ] Rollback invariant: open failure leaves no orphaned pending launch in persistence
- [ ] Resume flag chosen in the dropdown survives the project switch through to launch context assembly
- [ ] Public handoff surface is ≤3 entry points (e.g. `scheduleLaunch`, `completePendingLaunchIfAny`, and any test-visible seam)
- [ ] `WorktreePendingLaunchHandoffTest` covers `scheduleLaunch` rollback on mocked open failure and path-normalization consistency between enqueue and consume keys
- [ ] End-to-end manual check: "Run in New Worktree" and "Open/Resume worktree" both open the agent tab in the worktree project with the same reliability as before
- [ ] `./gradlew qualityGate` passes; `CHANGELOG.md` updated under `### Changed`
