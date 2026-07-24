---
title: Quality gate & release workflow
type: workflow
status: current
updated: 2026-07-24
sources:
  - build.gradle.kts
  - detekt.yml
  - .github/workflows/build-plugin.yml
  - scripts/pre-commit
  - scripts/wiki-lint.mjs
---

# Quality gate & release workflow

## Summary

The repository exposes a `qualityGate` Gradle task that runs formatting, compilation, linting, detekt (including type-resolution `detektMain` / `detektTest`), tests, and JaCoCo coverage. CI builds plugin artifacts via `./gradlew buildPlugin`. Pre-commit hooks run wiki-lint, ktlintCheck, and graphify update.

## Verified Facts

- `qualityGate` task is defined in `build.gradle.kts` and depends on `test`, `jacocoTestReport`, `detektMain`, and `detektTest`.
- Task chain: `compileKotlin` → `ktlintFormat`; `test` → `ktlintCheck`, `detekt`, `detektMain`, `detektTest` → `jacocoTestReport`.
- `detekt.yml` is the canonical static-analysis policy; Qodana was removed in favor of expanded detekt coverage.
- CI uses `./gradlew clean buildPlugin jar` in `.github/workflows/build-plugin.yml`.
- Pre-commit (`scripts/pre-commit`): blocks `@Suppress` annotations, runs `npm run wiki-lint`, `./gradlew ktlintCheck`, and `graphify update .`.
- Wiki mechanical lint: `npm run wiki-lint` or `node scripts/wiki-lint.mjs --staged`.

## Agent Synthesis

- For developer validation, prefer running `./gradlew qualityGate` locally. For CI artifact creation use `./gradlew buildPlugin`.
- Pre-commit enforces wiki path-map compliance and keeps the structural graph fresh via graphify.

## Related

- [Agent CLI overview](../concepts/agent-cli-overview.md)
