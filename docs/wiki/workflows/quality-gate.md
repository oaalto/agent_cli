---
title: Quality gate & release workflow
type: workflow
status: draft
updated: 2026-06-24
sources:
  - build.gradle.kts
  - .github/workflows/build-plugin.yml
---

# Quality gate & release workflow

## Summary

The repository exposes a `qualityGate` Gradle task that runs formatting, compilation, linting, detekt (including type-resolution `detektMain` / `detektTest`), and tests in order. CI builds plugin artifacts via `./gradlew buildPlugin` in the GitHub Actions workflow.

## Verified Facts

- `qualityGate` task is defined in `build.gradle.kts` and depends on `test`, `detektMain`, and `detektTest`.
- `detekt.yml` is the canonical static-analysis policy; Qodana was removed in favor of expanded detekt coverage.
- CI uses `./gradlew clean buildPlugin jar` in `.github/workflows/build-plugin.yml`.

## Agent Synthesis

- For developer validation, prefer running `./gradlew qualityGate` locally. For CI artifact creation use `./gradlew buildPlugin`.

## Related

- [Agent CLI overview](../concepts/agent-cli-overview.md)
