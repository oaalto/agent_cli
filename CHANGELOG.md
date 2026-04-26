# Changelog

## 2026-04-26

### Added
- Added Gradle-based `ktlint` integration plus a `qualityGate` task to enforce ordered format/compile/lint/test checks, so contributor validation is consistent and reproducible.
- Added focused unit tests for WSL/host path mapping and normalization logic to prevent regressions in worktree path handling across Windows and WSL inputs.
- Added a regression test asserting the split-button default action remains `Run in Current Project`, guarding intended run behavior.

### Changed
- Refactored worktree path conversion logic into a pure helper (`AgentWorktreePathMapper`) to isolate deterministic transformations from IDE-bound service code and make them directly testable.
- Updated contributor documentation to match current Run Agent behavior and to document `ktlint`/`qualityGate` commands so docs stay aligned with actual workflow gates.

### Fixed
- Fixed unresolved Gradle Kotlin stdlib conflict warning by disabling the default stdlib dependency in `gradle.properties`, reducing runtime/version mismatch risk in IntelliJ platform builds.
- Fixed ignored status returns in selection/delete flows by handling failed state updates with explicit logging and fallback behavior, improving diagnosability and correctness.

- made by: Olli Aalto
- made with: Cursor
- model: Codex 5.3

## 2026-04-03

### Changed
- Updated the worktree action icon in the toolbar for clearer run/worktree affordance and faster visual recognition during agent launches.
- made by: Olli Aalto

## 2026-04-02

### Added
- Added a split `Run Agent` workflow with a current-project main action and dropdown worktree actions to support isolated session flows.
- Added WSL execution targeting so Windows IDE sessions can launch Linux-installed agent binaries with Linux path semantics.
- Added WSL-related screenshot coverage and refreshed README imagery so setup guidance matches current UI behavior.
- made by: Olli Aalto

### Changed
- Refactored Cursor resume fallback to process-based probing so resume behavior can degrade gracefully when no prior chats exist.
- Refreshed README feature highlights with versioned capability notes to make user-facing changes easier to scan.
- Established a `2.0` development baseline to anchor subsequent worktree and multi-CLI session work.
- made by: Olli Aalto

### Fixed
- Fixed stale managed worktree entry handling and refreshed split-button state after delete operations to prevent outdated menu items.
- Fixed WSL project working-directory resolution for UNC paths to avoid incorrect launch directories on Windows + WSL setups.
- made by: Olli Aalto

## 2026-02-23

### Added
- Added repository-level ignore rules for `.cursor` artifacts to reduce accidental noise in source control.
- Added automated tag-based release-note generation for GitHub Releases and IDEA plugin update notes to reduce manual release overhead.
- made by: Olli Aalto

### Fixed
- Fixed configuration-cache serialization in plugin metadata configuration to keep Gradle config-cache runs stable.
- Fixed settings navigation and editor shortcut handling in the Agent tab to improve command flow reliability.
- made by: Olli Aalto

## 2026-02-20

### Fixed
- Fixed terminal focus behavior so input is correctly directed when the Agent tab is selected.
- Fixed multi-session opening behavior so concurrent/new Agent tab sessions open as intended.
- made by: Olli Aalto

## 2026-02-19

### Added
- Initial project commit establishing the Agent CLI plugin codebase and baseline structure.
- made by: Olli Aalto

### Fixed
- Fixed CI execution by granting execute permission to `gradlew` so automated builds can run consistently.
- made by: Olli Aalto
