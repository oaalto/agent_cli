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
- `src/main/kotlin/com/oaalto/agent/OpenAgentEditorAction.kt`
- `src/main/kotlin/com/oaalto/agent/SelectAgentConfigurationActionGroup.kt`
- `src/main/kotlin/com/oaalto/agent/settings/AgentSettingsConfigurable.kt`
- `src/main/kotlin/com/oaalto/agent/settings/AgentSettingsState.kt`

## Prerequisites

- JDK 21 (project compiles with Java/Kotlin target 21)
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

- Verify plugin:

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

GitHub Actions workflow: `.github/workflows/build-plugin.yml`

- On push/PR/manual trigger:
  - Builds plugin artifacts
  - Uploads `.zip` + `.jar` as workflow artifacts
- On tag push (for example `v1.0.0`):
  - Uploads `.zip` + `.jar` to GitHub Releases

## Local testing checklist

- Add at least one configuration in `Settings -> Tools -> Agent CLI`
- Confirm `Select Agent` shows configured entries
- Confirm `Run Agent` opens one tab per selected configuration
- Validate error states:
  - missing binary path
  - non-executable binary path
  - invalid working directory

## IntelliJ Platform references

- IntelliJ Platform SDK docs: https://plugins.jetbrains.com/docs/intellij
- Gradle plugin docs: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html
- Plugin configuration file docs: https://plugins.jetbrains.com/docs/intellij/plugin-configuration-file.html
