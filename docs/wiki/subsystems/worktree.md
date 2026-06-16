---
title: Worktree subsystem
type: subsystem
status: draft
updated: 2026-06-16
sources:
  - src/main/kotlin/com/oaalto/agent/worktree/AgentWorktreePathMapper.kt
  - src/test/kotlin/com/oaalto/agent/worktree/AgentWorktreePathMapperTest.kt
---

# Worktree subsystem

## Summary

The Worktree subsystem isolates agent-run sessions into their own filesystem paths and provides mapping logic between host and agent-visible paths (including WSL/Windows handling).

## Verified Facts

- Mapping logic and normalization live in `AgentWorktreePathMapper.kt`.
- Unit tests covering path mapping are in `src/test/kotlin/com/oaalto/agent/worktree/`.
- Worktree behavior affects how the plugin constructs commands for WSL and wrapped-shell execution flows.

## Agent Synthesis

- When editing or testing path-mapping behavior, run the unit tests under `src/test/kotlin/...` and validate WSL path transforms on Windows hosts.

## Open Questions

- Are there repository-specific conventions for worktree storage locations or retention policies? (To Complete)

## Related

- [Agent CLI overview](../concepts/agent-cli-overview.md)
