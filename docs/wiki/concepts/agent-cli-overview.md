---
title: Agent CLI overview
type: concept
status: current
updated: 2026-07-24
sources:
  - CONTEXT.md
  - src/main/kotlin/com/oaalto/agent/
  - build.gradle.kts
---
## Summary

This page explains the purpose and high-level design of the Agent CLI plugin in this repository. The plugin constructs and runs external agent CLI binaries, manages isolated worktree sessions, and integrates planning artifacts (PRDs) under `docs/features/`.

## Verified Facts

- Plugin code lives under `src/main/kotlin/com/oaalto/agent/` and tests under `src/test/kotlin/`.
- Vertical slices: `pty/` (PTY Passthrough), `acp/` (ACP client + transcript UI), `worktree/` (git worktree isolation), `settings/` (shared configuration).
- Build and quality gates are implemented via Gradle tasks in `build.gradle.kts` (ktlint, detekt with type resolution, `qualityGate` task).
- CI builds artifacts via `./gradlew buildPlugin` as shown in `.github/workflows/build-plugin.yml`.
- Wiki mechanical lint runs via Node: `npm run wiki-lint` (`scripts/wiki-lint.mjs`); enforced in pre-commit.

## Agent Synthesis

- The repository is a single Gradle-based IntelliJ plugin project; recommended developer workflow uses the Gradle wrapper (`./gradlew`), ktlint for formatting, and the provided `qualityGate` task for ordered checks.
- Domain vocabulary is in `CONTEXT.md`; subsystem detail is in `docs/wiki/`.

## Related

- [Worktree subsystem](../subsystems/worktree.md)
