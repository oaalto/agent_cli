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

## Wiki Lint

When `scripts/wiki-lint.mjs` is present, run mechanical wiki lint before commit. Node reference implementation:

`node scripts/wiki-lint.mjs --staged`

This repository includes a Node-based wiki lint script and an npm script `npm run wiki-lint`.

## Docs Checks

`Not configured yet.`

## Runtime-Restricted Checks

Checks requiring credentials, root, Docker, cloud access, paid services, hardware, or local-only infrastructure:

None known.

## To Complete

Agent instruction: When this section lists items, offer the user LLM-assisted follow-up to resolve them. Do not invent commands silently.

- Confirm format command (`./gradlew ktlintFormat`)
- Confirm build/typecheck commands (`./gradlew build` and/or `./gradlew buildPlugin`)
- Confirm lint command (`./gradlew ktlintCheck`)
- Confirm test command (`./gradlew test`)
- Confirm whether to adopt `scripts/wiki-lint.mjs` (Node) or port it to a Gradle/shell implementation
