## Status

implemented

## Problem Statement

Plugin maintainers diagnosing agent session failures today rely on ad-hoc `Logger.warn` calls scattered across roughly eight call sites. There is no shared helper, no `info` or `debug` tier, no consistent toggles, and no standard session context on log lines. Many failure paths surface only through transcript text or `Messages.showErrorDialog` with no corresponding IDE log entry, making `idea.log` grep unreliable.

Recoverable failures (for example session load failing but session-picker fallback succeeding) are inconsistently logged. Operational detail useful during normal debugging (session opened, MCP servers resolved, process exit code) is absent unless developers add one-off logging. Developer-only verbosity (launch plan detail, reflection mapper internals) has no home and would pollute tier 1 if emitted at `warn`.

PTY Passthrough and ACP Client **Launch Mode** paths share worktree, settings, and terminal orchestration, but observability is not unified plugin-wide. **Session diagnostics** (tiered IDE log output with session context) is defined in domain vocabulary and ADR 0004, but not yet implemented.

This PRD covers **PR1 only**: tiered IDE logging infrastructure and migration. PR2 (session transcript file, correlation tokens, copy diagnostics) is separate.

## Solution

Introduce a thin `AgentCliLog` helper wrapping IntelliJ Platform `Logger` with three gated tiers and optional **Session diagnostics** context fields. Migrate existing direct `Logger` usage to `AgentCliLog`. Add tier-1 logging at dialog-only failure paths that today show errors to the user without logging.

### Tier model

| Tier | IntelliJ level | Always emitted? | Enable |
|------|----------------|-----------------|--------|
| 1 — Errors | `warn`, `error` | Yes | — |
| 2 — Normal | `info` | No | `AGENT_CLI_LOG=true` **or** registry `agent_cli.log=true` |
| 3 — Debug | `debug` | No | `AGENT_CLI_DEBUG=true` **or** registry `agent_cli.debug=true` |

Rules:

- Environment variable **or** registry flag enables a tier (either is sufficient).
- Debug implies log (enabling tier 3 also enables tier 2).
- Tier 1 includes **all** failures, including recoverable ones where the user sees no hard stop (for example load failed → picker → new session succeeded).
- Tier 2 covers operational milestones: session opened, MCP servers resolved, process exit code, and similar.
- Tier 3 covers developer-only detail: launch plan (with secrets redacted), auth/permission steps, reflection mapper detail.
- `PlanUpdateMapper` reflection failures move from tier 1 (`warn`) to tier 3 (`debug`) to reduce noise.

### Session context on log lines

When available, log lines include structured context:

- `configId` — **Agent configuration** ID
- `sessionId` — ACP session id or PTY resume identifier as applicable
- `launchMode` — **PTY Passthrough** or **ACP Client**
- `worktreePath` — filesystem path when session runs in a **Worktree**

Context is attached consistently so `idea.log` grep by configuration or session is practical.

### Scope of migration (PR1)

**Migrate** existing direct `Logger` call sites in:

- `SelectAgentConfigurationActionGroup`
- `AcpAgentEditor`
- `AcpSessionControllerImpl`
- `TranscriptBlockViewFactory`
- `McpServerSources`
- `PlanUpdateMapper` (reflection failures → tier 3)
- `PtyAgentEditor`
- `DeleteWorktreeAction` (in `RunAgentSplitButtonAction`)

**Add** tier-1 logging at dialog-only failure paths:

- Worktree create, delete, and open failures
- Settings import and export failures
- Terminal launch failures

Applies to both **Launch Mode** paths (PTY and ACP Client).

### Relationship to ADR 0004

This PR delivers the **Session diagnostics** IDE-log channel from [ADR 0004](../../adr/0004-session-observability.md). Transcript persistence, correlation tokens on transcript errors, copy session diagnostics action, and ACP wire trace remain PR2 or later.

## User Stories

1. As a plugin maintainer, I want all session failures logged at tier 1 (`warn`/`error`) even when the UI recovers gracefully, so that `idea.log` reflects what actually happened during a support investigation.
2. As a plugin maintainer, I want tier-1 logging on worktree create failures that today only show an error dialog, so that I can diagnose Git or filesystem issues without reproducing the dialog path manually.
3. As a plugin maintainer, I want tier-1 logging on worktree delete failures that today only show an error dialog, so that orphaned worktrees and permission errors leave a grep-friendly trace.
4. As a plugin maintainer, I want tier-1 logging on worktree open failures that today only show an error dialog, so that path and VCS problems are visible in IDE log.
5. As a plugin maintainer, I want tier-1 logging on agent settings import failures, so that malformed or incompatible `acp.json` imports are diagnosable after the user dismisses the dialog.
6. As a plugin maintainer, I want tier-1 logging on agent settings export failures, so that write-permission or serialization errors are captured in `idea.log`.
7. As a plugin maintainer, I want tier-1 logging on terminal launch failures in both PTY and ACP paths, so that process spawn and shell integration errors are not silent after the user sees a dialog.
8. As a plugin maintainer, I want a single `AgentCliLog` helper instead of ad-hoc `Logger.getInstance` calls, so that tier gates and session context are applied consistently plugin-wide.
9. As a plugin maintainer, I want `configId` on log lines when a session is tied to an **Agent configuration**, so that I can filter logs per named configuration.
10. As a plugin maintainer, I want `sessionId` on log lines when an ACP or PTY session identifier is known, so that I can correlate log output with resume and worktree state.
11. As a plugin maintainer, I want `launchMode` on log lines, so that I can distinguish PTY Passthrough from ACP Client behavior in mixed repro steps.
12. As a plugin maintainer, I want `worktreePath` on log lines when the session uses a **Worktree**, so that filesystem isolation issues map to a concrete directory.
13. As a plugin maintainer, I want session context fields omitted gracefully when not yet available (for example before session id is assigned), so that early bootstrap logs are still useful without fake placeholders.
14. As a plugin maintainer, I want tier 2 (`info`) enabled by setting environment variable `AGENT_CLI_LOG=true`, so that I can turn on operational logging in a CI or local run without IDE registry edits.
15. As a plugin maintainer, I want tier 2 enabled by registry flag `agent_cli.log=true`, so that I can persist logging preference across restarts without shell env setup.
16. As a plugin maintainer, I want tier 3 (`debug`) enabled by environment variable `AGENT_CLI_DEBUG=true`, so that I can capture verbose detail for a single reproduction run.
17. As a plugin maintainer, I want tier 3 enabled by registry flag `agent_cli.debug=true`, so that deep diagnostics stay on during iterative plugin development.
18. As a plugin maintainer, I want enabling debug to automatically enable info (debug implies log), so that I do not need to set two flags for full verbosity below tier 1.
19. As a plugin maintainer, I want either env **or** registry to suffice for each tier, so that command-line launches and IDE-run configurations both work.
20. As a plugin maintainer, I want tier 1 always on with no toggle, so that production users never miss failure signals because logging was accidentally disabled.
21. As a plugin maintainer, I want tier 2 off by default, so that normal IDE usage does not flood `idea.log` with operational detail.
22. As a plugin maintainer, I want tier 3 off by default, so that reflection and launch-plan detail never appear unless explicitly requested.
23. As a plugin maintainer, I want `SelectAgentConfigurationActionGroup` failures migrated to `AgentCliLog`, so that toolbar configuration selection errors use the shared tier and context model.
24. As a plugin maintainer, I want `AcpAgentEditor` logging migrated to `AgentCliLog`, so that ACP Client editor lifecycle emits consistent session context.
25. As a plugin maintainer, I want `AcpSessionControllerImpl` logging migrated to `AgentCliLog`, so that connect, session open, load, and prompt failures include session identifiers.
26. As a plugin maintainer, I want `TranscriptBlockViewFactory` logging migrated to `AgentCliLog`, so that transcript rendering issues are tied to the active session when context exists.
27. As a plugin maintainer, I want `McpServerSources` logging migrated to `AgentCliLog`, so that MCP resolution failures and tier-2 resolution success are gated correctly.
28. As a plugin maintainer, I want `PlanUpdateMapper` reflection failures at tier 3 (`debug`) instead of tier 1 (`warn`), so that optional reflection paths do not alarm users scanning for warnings.
29. As a plugin maintainer, I want `PtyAgentEditor` logging migrated to `AgentCliLog`, so that PTY Passthrough sessions receive the same diagnostics model as ACP Client.
30. As a plugin maintainer, I want `DeleteWorktreeAction` logging migrated to `AgentCliLog`, so that worktree deletion errors from the run split button include worktree context.
31. As a plugin maintainer, I want recoverable ACP session load failures logged at tier 1 before fallback to session picker, so that I can see load errors even when the user successfully picks another session.
32. As a plugin maintainer, I want recoverable session-picker cancellation logged appropriately at tier 1 when it leads to a failed or degraded outcome, so that abandoned resume flows are visible.
33. As a plugin maintainer, I want tier-2 `info` when an ACP session opens successfully, so that I can confirm session id assignment without enabling debug.
34. As a plugin maintainer, I want tier-2 `info` when MCP servers are resolved for a launch, so that configuration mistakes versus runtime failures are easier to separate.
35. As a plugin maintainer, I want tier-2 `info` when an agent subprocess exits with an exit code, so that silent tab closes are explained in log.
36. As a plugin maintainer, I want tier-3 `debug` launch plan detail with secrets redacted, so that I can inspect argv, env keys, and paths without leaking API keys or tokens into `idea.log`.
37. As a plugin maintainer, I want tier-3 `debug` for auth and permission prompt steps, so that I can trace ACP client auth flow without tier-1 noise.
38. As a plugin maintainer, I want tier-3 `debug` for `PlanUpdateMapper` reflection detail, so that SDK shape mismatches are investigable on demand.
39. As a plugin maintainer, I want PTY Passthrough launch and dispose paths to use the same tier gates as ACP Client, so that Launch Mode does not determine observability quality.
40. As a plugin maintainer, I want worktree orchestration logging in both launch modes, so that mode-agnostic **Worktree** failures share one diagnostics vocabulary.
41. As a plugin maintainer, I want logging to remain in `idea.log` via IntelliJ `Logger`, so that standard IDE diagnostic tooling and support workflows continue to apply.
42. As a plugin maintainer, I want no new in-editor diagnostics panel for this work, so that scope stays focused on grep-friendly IDE log (per ADR 0004).
43. As a plugin maintainer, I want agent stderr to remain in the **Transcript** UI for ACP mode and not duplicated to IDE log in PR1, so that log volume stays manageable until explicitly scoped otherwise.
44. As a plugin maintainer, I want tier gates implemented as pure checks against env and registry, so that they are unit-testable without booting the IDE.
45. As a developer writing tests, I want `AgentCliLog` level gate functions testable in isolation, so that CI validates toggle behavior without heavyweight integration fixtures.
46. As a developer extending the plugin, I want a small API surface (`error`, `warn`, `info`, `debug` with optional context builder), so that new call sites default to correct tiers.
47. As a developer extending the plugin, I want lazy message evaluation or equivalent so that tier-3 string building runs only when debug is enabled, so that disabled tiers add negligible overhead.
48. As a plugin user, I want no change to visible UI behavior from logging migration alone, so that PR1 is diagnostics-only with no product UX regression.
49. As a plugin user, I want error dialogs to continue appearing on hard failures, so that logging supplements rather than replaces user-facing errors.
50. As a plugin maintainer, I want documentation in ADR 0004 and **Session diagnostics** glossary in `CONTEXT.md` to align with shipped behavior, so that future PR2 work (correlation tokens, transcript file) builds on consistent terminology.
51. As a plugin maintainer, I want `PlanUpdateMapper` tier change documented in changelog when code ships, so that support knows reflection messages moved from warn to debug.
52. As a plugin maintainer, I want logging practices rule and wiki to reference tiered logging after implementation, so that contributors use `AgentCliLog` instead of raw `Logger` for new code.
53. As a plugin maintainer investigating PTY sessions, I want terminal launch failure logs to include launch mode and configuration context, so that PTY-specific repro steps are self-contained in one grep.
54. As a plugin maintainer investigating ACP sessions, I want session controller errors to include `sessionId` when known, so that resume and load failures map to ACP server state.
55. As a plugin maintainer, I want settings import/export logs to avoid writing secret field values even at debug tier, so that redaction policy matches launch plan handling.
56. As a plugin maintainer, I want registry keys `agent_cli.log` and `agent_cli.debug` to follow existing plugin registry conventions, so that discovery and documentation stay consistent with other agent-cli toggles.
57. As a plugin maintainer running from Gradle `runIde`, I want env vars documented for enabling tiers, so that local reproduction matches CI and support instructions.
58. As a plugin maintainer, I want correlation tokens on transcript errors deferred to PR2, so that PR1 does not half-implement cross-channel linking.
59. As a plugin maintainer, I want ACP JSON-RPC wire trace explicitly out of scope for PR1, so that a separate flag can be designed later without conflating with `AGENT_CLI_DEBUG`.
60. As a plugin maintainer, I want copy session diagnostics editor action deferred to PR2, so that PR1 focuses on automatic log emission rather than manual export UX.

## Implementation Decisions

### Ownership

- **Scope:** Plugin-wide logging infrastructure; touches `acp/`, `pty/`, `worktree/`, `settings/`, and shared UI action modules that today call `Logger` directly.
- **Vertical slices:** Both **Launch Mode** paths and mode-agnostic worktree/settings flows; no transcript file or ACP persistence changes in PR1.

### AgentCliLog module

Introduce a thin helper wrapping IntelliJ Platform `Logger`:

- One `AgentCliLog` instance (or factory) per owning class, mirroring current `Logger.getInstance` usage pattern.
- Methods aligned to tiers: `error`, `warn` (always emit); `info` (tier 2 gate); `debug` (tier 3 gate).
- Optional session context supplied per call or via a scoped context holder (builder or copy-on-write context object).

Context fields (all optional, omitted when unknown):

```kotlin
data class AgentCliSessionContext(
    val configId: String? = null,
    val sessionId: String? = null,
    val launchMode: LaunchMode? = null,
    val worktreePath: String? = null,
)
```

Log message formatting should prefix or suffix context in a stable, grep-friendly shape (exact format is implementer choice; consistency matters more than punctuation).

### Tier gate implementation

Extract pure gate functions (no IDE startup required for unit tests):

```kotlin
fun isAgentCliLogEnabled(env: Map<String, String>, registryLog: Boolean, registryDebug: Boolean): Boolean
fun isAgentCliDebugEnabled(env: Map<String, String>, registryDebug: Boolean): Boolean
```

Rules encoded in gates:

- Tier 2 (`info`): `env["AGENT_CLI_LOG"].equals("true", ignoreCase = true)` **or** registry `agent_cli.log == true` **or** debug enabled.
- Tier 3 (`debug`): `env["AGENT_CLI_DEBUG"].equals("true", ignoreCase = true)` **or** registry `agent_cli.debug == true`.
- Debug implies log: when tier 3 is enabled, tier 2 methods emit.

Production gate reads env and IntelliJ registry; tests inject maps and booleans.

**Primary test seam:** these pure gate functions — unit test without IDE.

**Secondary test seam:** migrate one representative call site to assert log level/context behavior using test doubles or lightweight fakes if an integration-style pattern already exists; follow prior art of small focused unit tests with mocked platform services (similar in spirit to existing worktree state service tests).

### Secret redaction (tier 3)

Launch plan and settings-related debug output must redact secrets:

- Environment variable **values** for known secret keys (API keys, tokens, passwords) → literal `[REDACTED]` or omission.
- Auth headers and bearer tokens never logged at full value.
- Env **key names** may appear; values only when non-sensitive or redacted.
- Same policy applies to settings import/export debug detail.

Exact redaction list can start with keys already treated as sensitive elsewhere in the plugin; extend conservatively.

### Migration map

Replace direct `Logger` usage with `AgentCliLog` at existing call sites:

| Area | Migration note |
|------|----------------|
| Agent configuration toolbar selection | Preserve current severity; add context when configuration id known |
| ACP agent editor | Session lifecycle, launch, dispose; attach `launchMode = ACP Client` |
| ACP session controller | Connect, session open/load/list, prompt errors; attach `sessionId` when assigned |
| Transcript block view factory | Rendering failures; attach session context from editor/session when available |
| MCP server sources | Resolution failures (tier 1); resolution summary (tier 2 info when enabled) |
| Plan update mapper | Reflection failures → **tier 3 debug only** (remove tier 1 warn) |
| PTY agent editor | PTY lifecycle; attach `launchMode = PTY Passthrough` |
| Delete worktree action | Deletion failures; attach `worktreePath` |

No behavioral change to user-visible dialogs or transcript text in PR1 except log destination and tier for `PlanUpdateMapper`.

### New tier-1 coverage (dialog-only gaps)

Add `warn`/`error` via `AgentCliLog` where failures today surface only through `Messages.showErrorDialog` (or equivalent) with no log line:

- Worktree create failure
- Worktree delete failure
- Worktree open failure
- Agent settings import failure
- Agent settings export failure
- Terminal launch failure (PTY and ACP embedded terminal paths)

Each log line should include human-readable failure reason (exception message or structured error) plus available session context.

### Registry keys

- `agent_cli.log` (boolean) — tier 2
- `agent_cli.debug` (boolean) — tier 3

Document alongside env vars `AGENT_CLI_LOG` and `AGENT_CLI_DEBUG` in changelog and optionally wiki when implemented.

### Performance

- Tier 2/3 messages should not evaluate expensive string formatting when gate is false (lambda/supplier pattern or equivalent).
- No synchronous I/O in logging path beyond IntelliJ `Logger` behavior.

### ADR and glossary alignment

- Implements IDE-log portion of [ADR 0004](../../adr/0004-session-observability.md) PR1 delivery slice.
- **Session diagnostics** term in `CONTEXT.md` refers to this tiered `idea.log` output; PR2 adds correlation tokens and transcript file — do not implement those in PR1.

## Testing Decisions

### What makes a good test

- Test **external behavior** of tier gates: given env map and registry flags, assert whether `info` and `debug` would emit — not internal `Logger` delegate wiring.
- Test redaction helpers if extracted: given a launch plan or env map with secret keys, assert debug string never contains raw secret values.
- Do **not** assert exact log line punctuation unless format is a committed contract; prefer behavioral assertions (tier enabled, secret absent).
- Avoid tests that require full IDE startup for gate logic; reserve heavyweight tests for at most one migrated call-site pattern if valuable.

### Modules to test

- **Tier gate pure functions** — primary coverage; matrix of env/registry combinations including debug-implies-log.
- **Redaction helper** (if extracted) — secret keys redacted, non-secret values preserved.
- **Optional:** one representative migrated component with fake `AgentCliLog` or test logger spy — secondary; only if low-cost pattern exists.

### Prior art

- Small unit tests with mocked platform services and focused assertions (worktree state service test style).
- Existing session controller tests use recording fakes — pattern for optional secondary seam, not required to duplicate across all eight migrations.

### Verification

`./gradlew qualityGate` passes after implementation.

## Out of Scope

- **Session transcript file** persistence under `.idea/agent-cli/transcripts/` (PR2).
- **Correlation tokens** on transcript error lines linking to IDE log (PR2).
- **Copy session diagnostics** editor action (PR2).
- **ACP wire trace** (JSON-RPC message logging); future separate flag if added.
- Mirroring agent stderr from **Transcript** into IDE log.
- In-editor diagnostics panel.
- Automatic pruning of log files or transcript files.
- Changing **Transcript** UI behavior or block rendering beyond migrating existing log call sites in the block view factory.
- PTY session transcript file (ADR rejects for PTY).
- Wiki and logging-practices rule updates may land with implementation but are not blocking spec acceptance.

## Further Notes

- **PR sequencing:** Ship PR1 (this spec) before PR2 transcript persistence so maintainers gain grep-friendly IDE log immediately with lower risk than file lifecycle work.
- **Noise reduction:** Moving `PlanUpdateMapper` reflection from warn to debug is intentional; support should enable `AGENT_CLI_DEBUG` when investigating plan panel mapping.
- **Recoverable failures:** Tier 1 explicitly includes failures that do not block the user permanently — this matches ADR 0004 and grilling decision; do not downgrade recoverable paths to tier 2.
- **Launch modes:** PTY and ACP share `AgentCliLog`; context fields differ in which ids are populated (`sessionId` semantics differ by mode) but API is unified.
- **Delivery:** Single PR acceptable for PR1 scope (helper + gates + migration + dialog gaps); split only if review size forces it.
- Implementers should update `CHANGELOG.md` under `### Added` or `### Changed` when code ships.
- Related ADR: [0004 Session observability](../../adr/0004-session-observability.md).
- PR2 spec will live as a separate feature folder or section when scheduled; this PRD must not expand into transcript file requirements.
