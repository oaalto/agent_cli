---
title: Agent CLI overview
type: concept
status: draft
updated: 2026-06-16
sources:
  - src/main/kotlin/com/oaalto/agent/
  - build.gradle.kts
---
## Summary

This page explains the purpose and high-level design of the Agent CLI plugin in this repository. The plugin constructs and runs external agent CLI binaries, manages isolated worktree sessions, and integrates planning artifacts (PRDs) under `docs/prd/`.

## Verified Facts

- Plugin code lives under `src/main/kotlin/com/oaalto/agent/` and tests under `src/test/kotlin/`.
- Build and quality gates are implemented via Gradle tasks in `build.gradle.kts` (ktlint integration, `qualityGate` task).
- CI builds artifacts via `./gradlew buildPlugin` as shown in `.github/workflows/build-plugin.yml`.

## Agent Synthesis

- The repository is a single Gradle-based IntelliJ plugin project; recommended developer workflow uses the Gradle wrapper (`./gradlew`), ktlint for formatting, and the provided `qualityGate` task for ordered checks.

## Open Questions

- Should wiki lint be run via Node (`scripts/wiki-lint.mjs`) or ported to a Gradle-friendly script? (To Complete)

## Related

- [Worktree subsystem](../subsystems/worktree.md)
