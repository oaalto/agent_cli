# 03 — Tier-1 logging at dialog-only failure paths

**Parent:** `prd.md`

**What to build:** Fill observability gaps where failures today surface only through user error dialogs (or equivalent) with no matching IDE log line. Worktree create, delete, and open failures; agent settings import and export failures; and terminal launch failures in both PTY Passthrough and ACP Client embedded-terminal paths each emit tier-1 `warn` or `error` via `AgentCliLog` alongside the existing dialog. Each line includes a human-readable failure reason (exception message or structured error) plus available session context (configuration id, session id, launch mode, worktree path). Settings-related debug detail follows the same secret redaction policy as launch-plan logging. Error dialogs continue to appear on hard failures; this ticket adds grep-friendly `idea.log` traces only.

**Blocked by:** 01 — AgentCliLog helper and tier gate functions

**Status:** done

- [x] Worktree create failure shows the existing error dialog and emits a tier-1 log line with failure reason and worktree path context when known
- [x] Worktree delete failure shows the existing error dialog and emits a tier-1 log line with failure reason and worktree path context
- [x] Worktree open failure shows the existing error dialog and emits a tier-1 log line with failure reason and path context
- [x] Agent settings import failure shows the existing error dialog and emits a tier-1 log line; no secret field values appear even if debug tier is enabled elsewhere
- [x] Agent settings export failure shows the existing error dialog and emits a tier-1 log line with write/serialization error detail
- [x] Terminal launch failure in PTY Passthrough path shows the existing error dialog and emits tier-1 log with launch mode and configuration context
- [x] Terminal launch failure in ACP Client embedded-terminal path shows the existing error dialog and emits tier-1 log with launch mode, configuration, and session context when known
- [x] Recoverable or degraded outcomes from abandoned resume flows log at tier 1 when they represent a failed or degraded session outcome per PRD
- [x] No new in-editor diagnostics panel; no transcript persistence or correlation tokens (PR2 scope)
- [x] `./gradlew qualityGate` passes; `CHANGELOG.md` updated under `### Added` or `### Changed` for dialog-path logging coverage
