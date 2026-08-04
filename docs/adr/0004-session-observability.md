# ADR 0004: Session observability — transcript file and tiered IDE logging

- **Status:** Accepted
- **Date:** 2026-07-24

## Context

The plugin has two observability needs with different audiences:

1. **Conversation access** — users running agents in ACP Client mode need an easy way to read the LLM discussion during and after a session.
2. **Developer diagnostics** — plugin maintainers need structured, grep-friendly detail in `idea.log` when sessions fail or behave unexpectedly.

Today:

- The **Transcript** UI is in-memory only; closing the editor tab loses conversation history.
- IDE logging uses ad-hoc `Logger.warn` in ~8 files; no `info`/`debug`, no toggles, no shared session context.
- Many failure paths surface only via transcript text or `Messages.showErrorDialog` with no log line.
- Agent stderr is shown in the transcript (`[stderr] …`) but not mirrored to IDE log.

PTY Passthrough mode does not use the Transcript UI; external agent CLIs typically persist their own session history. Observability work for conversation persistence is scoped to ACP Client mode only.

## Decision

Split observability into two channels and implement tiered IDE logging plugin-wide.

### Two channels

| Channel | Audience | Medium | Content |
|---------|----------|--------|---------|
| **Transcript** (live UI) | User | In-tab ACP editor | Full LLM thread, short error lines, agent stderr |
| **Session transcript file** | User | Workspace-local disk | Plain-text conversation record |
| **IDE log** | Developer | `idea.log` via IntelliJ `Logger` | Warnings, errors, and optional info/debug detail |

Transcript errors shown to the user include a correlation token (e.g. `[agent-cli:a3f2]`) so the matching IDE log lines can be found with one grep. Stack traces stay in IDE log only.

**Copy session diagnostics** (editor action): copies session context to the clipboard — correlation IDs, config/session IDs, launch mode, worktree path, recent errors — for bug reports. Complements grep-by-token; does not replace it.

### Session transcript file (ACP only)

| Property | Value |
|----------|-------|
| Key | `acpSessionId` |
| Path | `<project>/.idea/agent-cli/transcripts/{yyyy-MM-dd_HH-mm-ss}_{acpSessionId}.txt` |
| Filename | Human-readable local session-start timestamp prefix; logical key remains `acpSessionId` |
| Legacy | Pre-change files at `<acpSessionId>.txt` are read on restore; new sessions use the timestamped pattern |
| Scope | Workspace-local (not VCS) |
| Write | Debounced full snapshot (~300–500ms) of plain-text render from `TranscriptModel.blocks()` |
| Restore | On editor open with resumed session: read file → replay lines as plain transcript text (no block reconstruction) |
| Pre-session | Buffer in memory until `acpSessionId` is known, then create/append file |

Serialized file contents:

- User prompts (`> …`), agent text, errors, stderr lines
- One line per tool call: `[tool: name …]` (headers only, no full payloads)
- No plan panels, markdown reconstruction, or tool result bodies

PTY Passthrough: no session transcript file (terminal scrollback and agent-side history suffice).

### Tiered IDE logging (plugin-wide)

Introduce a thin `AgentCliLog` helper wrapping IntelliJ `Logger` with level gates and optional session context fields (`configId`, `sessionId`, `launchMode`, `worktreePath`).

| Tier | IntelliJ level | Always emitted? | Enable |
|------|----------------|-----------------|--------|
| 1 — Errors | `warn`, `error` | Yes | — |
| 2 — Normal | `info` | No | `AGENT_CLI_LOG=true` **or** registry `agent_cli.log=true` |
| 3 — Debug | `debug` | No | `AGENT_CLI_DEBUG=true` **or** registry `agent_cli.debug=true` |

Rules:

- Environment variable **or** registry flag enables a tier (either is sufficient).
- Debug implies log (enabling debug also enables info).
- Tier 1 includes recoverable failures (e.g. session load failed but picker fallback succeeded), not only hard stops.
- Tier 2 examples: session opened, MCP servers resolved, process exit code.
- Tier 3 examples: launch plan detail (env redacted), auth/permission steps, reflection mapper detail.
- ACP wire trace (JSON-RPC in/out) is **out of scope** for v1; add later behind a separate flag if needed.

Migrate existing `Logger` call sites to `AgentCliLog`. Add tier-1 logging at dialog-only failure paths (worktree create/delete, settings import, terminal launch).

### Delivery

Two PRs:

1. **PR1 — Logging:** `AgentCliLog`, tier gates, migrate existing sites, fill dialog-only gaps.
2. **PR2 — Transcript persistence:** file store, serializer, debounced write, plain restore, correlation tokens, copy diagnostics action.

## Alternatives considered

1. **Single debug flag** — Rejected. Conflates normal operational logging with developer-only verbosity; user requested three distinct tiers.
2. **Copy/export transcript only (no disk persistence)** — Rejected. User needs conversation available without manual export after tab close.
3. **Transcript keyed by `worktreeId`** — Rejected. Current-project runs have no worktree; `acpSessionId` aligns with ACP resume.
4. **Full block reconstruction on restore** — Rejected for v1. Plain-text replay is sufficient; parsing `[tool: …]` back into UI blocks adds complexity with little user benefit.
5. **Append-only transcript file** — Rejected. Streaming agent text updates in place; debounced snapshot keeps file consistent with UI.
6. **PTY transcript file** — Rejected. Duplicate of terminal scrollback; external agents already persist sessions.
7. **In-editor diagnostics panel** — Rejected (YAGNI). `idea.log` + correlation token + copy diagnostics covers debugging.
8. **One PR for all work** — Rejected. Logging infra is independently useful and lower risk than transcript lifecycle changes.

## Consequences

### Positive

- Users retain ACP conversation across editor close/reopen via workspace-local files
- Developers get grep-friendly IDE log with optional verbosity and session context
- Clear separation: transcript for discussion, IDE log for diagnosis
- Phased delivery allows logging improvements to ship before transcript persistence

### Negative

- New `AgentCliLog` and `TranscriptFileStore` modules to maintain
- Debounced snapshot writes add background I/O (mitigated by 300–500ms debounce)
- Restored history is plain text only until new live turns arrive
- Transcript files accumulate under `.idea/` (no automatic pruning in v1)

### Neutral

- Existing **Transcript** glossary term remains the live UI; **Session transcript file** is the persisted plain-text record
- `PlanUpdateMapper` reflection warnings move to tier 3 (`debug`) to reduce tier-1 noise
- Wiki and `logging-practices` rule should reference this ADR when implementation lands
