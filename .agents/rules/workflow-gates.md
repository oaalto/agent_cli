# Workflow Gates

Apply validation in this order and stop on failure:

1. **Format**
   - Run `./gradlew ktlintFormat` for changed Kotlin sources.
   - `compileKotlin` also depends on `ktlintFormat`, so a normal compile path auto-formats first.
   - Do not continue until `ktlintCheck` passes.
2. **Build / Typecheck**
   - Run `./gradlew compileKotlin compileTestKotlin` (or let later gates compile as part of their dependency chain).
   - Kotlin uses `allWarningsAsErrors`; fix or narrowly suppress warnings before proceeding.
   - Project toolchain targets JDK 21.
3. **Static analysis / lint**
   - Run `./gradlew ktlintCheck detekt detektMain detektTest`.
   - `detektMain` and `detektTest` run type-resolution analysis and are the primary static-analysis gate (replacing Qodana).
   - Treat findings as blocking unless project policy explicitly marks them non-blocking.
4. **Tests**
   - Run `./gradlew test`.
   - `test` depends on `ktlintCheck`, `detekt`, `detektMain`, and `detektTest`.
   - JaCoCo reports are generated via `jacocoTestReport`; coverage thresholds are not enforced yet because IntelliJ Platform sandbox tests do not attach the JaCoCo agent.

## Canonical local command

Prefer the ordered Gradle task:

```text
./gradlew qualityGate
```

This runs format → compile → ktlint → detekt (including type-resolution main/test) → tests → JaCoCo report.

For plugin compatibility with target IDE builds, also run before release-oriented work:

```text
./gradlew verifyPlugin
```

## CI and hooks

- **CI** (`.github/workflows/build-plugin.yml`): `node scripts/wiki-lint.mjs`, then `./gradlew qualityGate verifyPlugin`, then artifact build.
- **Pre-commit** (`scripts/pre-commit`): wiki-lint (staged) and `ktlintCheck` only — run `./gradlew qualityGate` before push when code changed beyond formatting.

## Zero-Suppression Enforcement

**No `@Suppress` annotations are permitted.** The pre-commit hook in `scripts/pre-commit` scans staged files for `@Suppress` and blocks the commit. See `warning-hygiene.md` for full policy and migration rules.

## Fail-Fast Policy

- If any gate fails, fix issues at that gate before proceeding.
- Do not skip gate order unless the project has an explicit documented exception.

## Optional Docs-Only Path

- For docs-only changes, allow a reduced validation path: `npm run wiki-lint` (or `node scripts/wiki-lint.mjs`) plus any docs-specific checks defined by project policy.
- Skip `./gradlew qualityGate` only when the change set contains no Kotlin/build/config changes.
