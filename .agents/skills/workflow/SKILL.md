---
name: workflow
description: Validation gate order (format, build/typecheck, lint, test). Load before editing or fixing any source file and run gates before changelog or marking work complete.
---

# Workflow gates

Apply before editing repository source files — including typo fixes and small bugfixes — not only before commit.

Apply validation in order and **stop on failure at each gate**. Fix every finding at the current gate before the next gate, changelog, commit, or marking work complete. Commands: `docs/agent-commands.md` when present.

Keep scoped `code-format` and `warning-hygiene` rules for baseline formatting presentation and warning-fix discipline when installed.

## Kotlin/Gradle gate order (this repository)

This repository uses Gradle with ktlint, detekt, JaCoCo, and IntelliJ Platform plugin verification:

| Step | Gate                    | Command                                              |
| ---- | ----------------------- | ---------------------------------------------------- |
| 1    | Format (ktlint)         | `./gradlew ktlintFormat`                             |
| 2    | Build / compile         | `./gradlew build` (dev) or `./gradlew buildPlugin` (CI) |
| 3    | Lint (ktlint + detekt)  | `./gradlew ktlintCheck`                              |
| 4    | Tests                   | `./gradlew test`                                     |
| 5    | Coverage                | JaCoCo report (finalized by test task)               |
| 6    | Verify plugin           | `./gradlew verifyPlugin` (CI only)                   |
| 7    | Graphify                | `graphify update .` when graph stale/missing         |

For the full gate chain: `./gradlew qualityGate` (format, compile, lint, detekt, tests, coverage).

Wiki-only changes: `node scripts/wiki-lint.mjs --staged` (also runs in pre-commit hook and CI).

## Generic gate order (toolchain not installed)

1. **Format** — formatting checks pass before continuing.
2. **Build / typecheck** — no compile/type errors.
3. **Static analysis / lint** — blocking unless project policy says otherwise.
4. **Tests** — required scope passes.

## Fix-everything-before-continue (mandatory)

**Absolute requirement:** Resolve every finding at the current gate before doing anything else — including the next gate, changelog, commit, or marking work complete.

- **One gate at a time:** Stop on first failure; re-run that gate until green before moving on.
- **No partial green:** Warnings, test failures, and format drift are blocking unless project policy documents a carve-out.
- **No deferral:** Do not leave follow-up fixes, baseline allowlists, or suppressions while a gate is red.
- **No suppressions:** Do not add lint/test/tool suppressions (`@Suppress`, detekt baselines, allowlists). Fix the code or config root cause.
- **No bypass:** Do not skip gate order or use `git commit --no-verify`.
- **Pre-commit parity:** Fix every pre-commit and wiki-lint finding before commit succeeds (`scripts/install-git-hooks.sh`).

Do not skip gate order unless project policy documents an exception.

## Format (ktlint)

- **Companion:** `code-format` scoped rule for diff presentation and file-ending hygiene.
- **Canonical formatter:** ktlint via Gradle (`./gradlew ktlintFormat`). Config in `build.gradle.kts`.
- **Command map:** `docs/agent-commands.md` — read before code-changing work; do not invent commands.
- Formatting checks are blocking. Do not hand-format around ktlint for stylistic preferences.
- Kotlin source files only — IntelliJ Platform plugin project (`com.oaalto.agent`).

## Lint (ktlint + detekt)

- **ktlint:** `./gradlew ktlintCheck` — Kotlin formatting and style.
- **detekt:** `./gradlew detekt` — static analysis with type resolution. Config in `detekt.yml`.
- **Companion:** `warning-hygiene` scoped rule for warning-fix discipline.
- **No suppressions:** do not add `@Suppress` annotations or detekt baseline files to land changes — fix the underlying issue.
- detekt `allRules = false` in `build.gradle.kts` — only enabled rules apply.

## Tests

- `./gradlew test` — runs Kotlin test suite (JUnit + `kotlin.test`).
- Tests depend on ktlintCheck and detekt (configured in `build.gradle.kts`).
- JaCoCo coverage report is finalized automatically after tests (`jacocoTestReport`).

## Graphify (step 7)

When graphify is installed (`command -v graphify` or `graphify-out/graph.json`):

- **Verify:** graph build commit matches `git rev-parse HEAD` when the project documents freshness checks.
- **Stale or missing:** `graphify update .` from repository root before marking work complete.
- **Non-blocking for CI:** local development concern only unless project policy says otherwise.

## Optional docs-only path

For docs-only changes, use wiki lint (`node scripts/wiki-lint.mjs --staged`) instead of the full Gradle gate sequence.
