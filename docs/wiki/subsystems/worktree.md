---
title: Worktree subsystem
type: subsystem
status: draft
updated: 2026-08-03
sources:
  - src/main/kotlin/com/oaalto/agent/worktree/AgentWorktreePathMapper.kt
  - src/main/kotlin/com/oaalto/agent/worktree/WorktreePendingLaunchHandoff.kt
  - src/test/kotlin/com/oaalto/agent/worktree/AgentWorktreePathMapperTest.kt
  - src/test/kotlin/com/oaalto/agent/worktree/WorktreePendingLaunchHandoffTest.kt
---

# Worktree subsystem

## Summary

The Worktree subsystem isolates agent-run sessions into their own filesystem paths and provides mapping logic between host and agent-visible paths (including WSL/Windows handling). Cross-project agent launches use `WorktreePendingLaunchHandoff` to schedule a pending launch, open the worktree project, and complete consumption on startup.

## Verified Facts

- Mapping logic and normalization live in `AgentWorktreePathMapper.kt`.
- Cross-project handoff orchestration lives in `WorktreePendingLaunchHandoff.kt` (`scheduleLaunch`, `completePendingLaunchIfAny`).
- Pending-launch persistence is in `AgentWorktreeStateService` (`enqueuePendingLaunch` / `consumePendingLaunch`).
- Unit tests covering path mapping and handoff are in `src/test/kotlin/com/oaalto/agent/worktree/`.
- Worktree behavior affects how the plugin constructs commands for WSL and wrapped-shell execution flows.

## Pending launch handoff lifecycle

1. **Schedule** — UI actions call `WorktreePendingLaunchHandoff.scheduleLaunch`, which enqueues intent in `agentWorktrees.xml`, opens the worktree project, and rolls back the enqueue if open fails.
2. **Consume** — `AgentPendingLaunchStartupActivity` delegates to `completePendingLaunchIfAny`, which consumes the pending launch for `project.basePath`, validates configuration, builds launch context via `WorktreeLaunchCoordinator`, opens the agent editor, and touches the worktree record once on success.

## Agent Synthesis

- When editing or testing path-mapping behavior, run the unit tests under `src/test/kotlin/...` and validate WSL path transforms on Windows hosts.

## Open Questions

- Are there repository-specific conventions for worktree storage locations or retention policies? (To Complete)

## Related

- [Agent CLI overview](../concepts/agent-cli-overview.md)
