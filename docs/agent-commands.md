# Agent Commands

Agent instruction: Read this file before code-changing work.

## Format

`./gradlew ktlintFormat`

## Build / Typecheck

`./gradlew build`  (developer)  
`./gradlew buildPlugin`  (CI / package artifact)

## Lint

`./gradlew ktlintCheck`

## Test

`./gradlew test`

## Quality Gate

`./gradlew qualityGate`  (format, compile, lint, detekt, tests, coverage — run before push)

## Wiki Lint

When `scripts/wiki-lint.mjs` is present, run mechanical wiki lint before commit. Node reference implementation:

`node scripts/wiki-lint.mjs --staged`

This repository includes a Node-based wiki lint script and an npm script `npm run wiki-lint`.

## Docs Checks

`Not configured yet.`

## Runtime-Restricted Checks

Checks requiring credentials, root, Docker, cloud access, paid services, hardware, or local-only infrastructure:

None known.
