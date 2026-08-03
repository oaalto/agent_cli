# Development Guide

This document is for contributors working on the `Agent CLI` plugin codebase.

## Project overview

- Language: Kotlin + Gradle
- Plugin ID: `com.oaalto.agent_cli`
- IntelliJ Platform target: `2025.3.2` (since-build `253`)
- Core plugin descriptor: `src/main/resources/META-INF/plugin.xml`

Main implementation files:

- `src/main/kotlin/com/oaalto/agent/AgentFileEditor.kt`
- `src/main/kotlin/com/oaalto/agent/AgentFileEditorProvider.kt`
- `src/main/kotlin/com/oaalto/agent/worktree/RunAgentSplitButtonAction.kt`
- `src/main/kotlin/com/oaalto/agent/worktree/AgentWorktreeService.kt`
- `src/main/kotlin/com/oaalto/agent/worktree/AgentWorktreeStateService.kt`
- `src/main/kotlin/com/oaalto/agent/SelectAgentConfigurationActionGroup.kt`
- `src/main/kotlin/com/oaalto/agent/settings/AgentSettingsConfigurable.kt`
- `src/main/kotlin/com/oaalto/agent/settings/AgentSettingsState.kt`

## Prerequisites

- JDK 21 (project compiles with Java/Kotlin target 21; Gradle auto-provisions JDK 21 via the Foojay toolchain resolver when needed)
- `gradle/gradle-daemon-jvm.properties` pins the Gradle daemon to JDK 21 so detekt 1.x works when the system default is Java 25+; regenerate with `./gradlew updateDaemonJvm --jvm-version=21` if criteria change
- Git
- IntelliJ IDEA (for local plugin development)

## Common Gradle tasks

- Run plugin in sandbox IDE:

  ```bash
  ./gradlew runIde
  ```

- Run tests:

  ```bash
  ./gradlew test
  ```

- Format and lint Kotlin sources:

  ```bash
  ./gradlew ktlintFormat
  ./gradlew ktlintCheck
  ```

- Run static analysis (detekt):

  ```bash
  ./gradlew detekt
  ```

- Run test coverage report:

  ```bash
  ./gradlew test jacocoTestReport
  ```

  JaCoCo is configured for HTML/XML reports. Threshold enforcement is not part of `qualityGate` yet because IntelliJ Platform tests run in an isolated sandbox JVM where the JaCoCo agent does not attach.

- Run ordered local quality gates (format -> compile -> lint -> static analysis -> tests -> coverage report):

  ```bash
  ./gradlew qualityGate
  ```

- Verify plugin compatibility with target IDE builds:

  ```bash
  ./gradlew verifyPlugin
  ```

- Build installable artifacts (`.zip` and `.jar`):

  ```bash
  ./gradlew clean buildPlugin jar
  ```

Expected outputs:

- `build/distributions/*.zip`
- `build/libs/*.jar`

## CI and release flow

GitHub Actions workflows:

- `.github/workflows/build-plugin.yml` — wiki lint, `qualityGate`, `verifyPlugin`, artifact build, and tag releases
- `.github/workflows/qodana.yml` — JetBrains Qodana static analysis (optional `QODANA_TOKEN` secret for cloud features)

Dependabot (`.github/dependabot.yml`) opens weekly update PRs for Gradle, GitHub Actions, and npm.

### Build plugin workflow

- On push/PR/manual trigger:
  - Runs wiki lint, `./gradlew qualityGate`, and `./gradlew verifyPlugin`
  - Builds plugin artifacts
  - Uploads `.zip` + `.jar` as workflow artifacts
- On tag push (for example `v3.0.0`, `3.0.0`, `v3.0-rc1`, `3.0-rc1`):
  - Derives plugin version from tag (with optional leading `v`)
  - Generates release notes from commit subjects since the previous tag
  - Injects generated notes into plugin `<change-notes>` metadata
  - Uploads `.zip` + `.jar` to GitHub Releases with generated release notes as the body
  - Marks GitHub release as prerelease when the parsed version contains a prerelease suffix (for example `-rc1`)

### Release note source

- Release notes are generated from `git log --no-merges --pretty=format:%s` for:
  - `previousTag..currentTag`, or
  - full history for the first tag.
- Commit message subjects should be user-facing because they are shown in:
  - GitHub Release notes
  - IDEA plugin update "What's New" (from `<change-notes>` in plugin metadata)

## Local testing checklist

- Add at least one configuration in `Settings -> Tools -> Agent CLI`
- Confirm `Select Agent` shows configured entries
- Confirm main click on `Run Agent` runs in the current project
- Confirm `Run in New Worktree` creates a new worktree and opens it
- Confirm new worktree branch name matches `agent/<config-slug>/<timestamp>`
- Confirm `Run Agent` dropdown lists previous plugin-managed worktrees
- Confirm selecting `Resume ...` opens that worktree and runs Cursor with `--continue`
- Confirm selecting `Resume ...` opens that worktree and runs OpenCode with `--continue` when `opencode` is the selected binary
- Confirm selecting `Delete ...` removes the selected non-current worktree
- Validate error states:
  - missing binary path
  - non-executable binary path
  - invalid working directory
  - detached HEAD when creating worktree
  - multiple repositories in one project (worktree creation blocked)

## IntelliJ Platform references

- IntelliJ Platform SDK docs: https://plugins.jetbrains.com/docs/intellij
- Gradle plugin docs: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html
- Plugin configuration file docs: https://plugins.jetbrains.com/docs/intellij/plugin-configuration-file.html
