# 01 — Handoff deep module: consume path

**Parent:** `prd.md`

**What to build:** A new `WorktreePendingLaunchHandoff` deep module in `worktree/` that owns cross-project pending-launch consumption. `AgentPendingLaunchStartupActivity` becomes a thin delegate that calls `completePendingLaunchIfAny(project)`. When a worktree project starts and a pending launch exists for its base path, the module validates configuration, assembles launch context via `WorktreeLaunchCoordinator`, opens the agent editor, and touches the worktree record once on successful completion. Missing configuration surfaces the existing error dialog; no editor opens.

**Blocked by:** None — can start immediately

**Status:** ready-for-agent

- [ ] `WorktreePendingLaunchHandoff.completePendingLaunchIfAny` exists and encapsulates consume → validate → launch-context → editor-open → touch ordering
- [ ] `AgentPendingLaunchStartupActivity` delegates to the handoff module with no remaining orchestration logic
- [ ] Path key invariant: enqueue and consume use the same normalized path key (`AgentWorktreeStateSupport.normalizedPathKey`)
- [ ] Configuration gate: missing configuration at consume time shows error dialog and does not open editor
- [ ] `WorktreeLaunchCoordinator` remains the launch-context seam; handoff module delegates to it without duplicating resume logic
- [ ] `WorktreePendingLaunchHandoffTest` covers successful consume path and missing-configuration path using test doubles for persistence, coordinator, and editor open
- [ ] `AgentWorktreeStateServiceTest` covers pending-launch enqueue/consume round-trip (fields preserved, consume removes record, re-enqueue replaces duplicate)
- [ ] Existing `WorktreeLaunchCoordinatorTest` and other worktree tests pass unchanged; `./gradlew qualityGate` passes
