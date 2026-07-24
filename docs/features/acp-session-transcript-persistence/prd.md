## Status

ready-for-agent

## Problem Statement

In **ACP Client** mode, the **Transcript** (live UI) is the user's view of the LLM conversation — user prompts, agent replies, errors, agent stderr, and tool activity. Today that conversation exists only in memory inside the open editor tab. Closing the tab or restarting the IDE discards the thread unless the user manually copied text elsewhere.

Users running agents in **ACP Client** mode need a durable, readable conversation record tied to the ACP session they are resuming. They should be able to close and reopen the editor, or return to a **Worktree** or current-project session later, and still read what was said — without exporting manually each time.

Developers supporting those users need a way to connect user-visible transcript errors to structured **Session diagnostics** in the IDE log. Today many failure paths show only a short line in the **Transcript** or a dialog, with no shared correlation identifier between what the user sees and what appears in `idea.log`.

**PTY Passthrough** mode is out of scope for this problem: external agent CLIs and terminal scrollback already provide session history, and the plugin does not host a **Transcript** UI in that mode.

This PRD covers **PR2** of [ADR 0004](../../adr/0004-session-observability.md) — **Session transcript file** persistence, plain-text restore, correlation tokens on transcript errors, and the **Copy session diagnostics** editor action. **PR1** (`AgentCliLog`, tiered IDE logging, migration of existing log sites) ships separately and is a dependency for correlation-token log pairing and the diagnostics clipboard bundle.

## Solution

Persist an ACP session's conversation as a workspace-local **Session transcript file** — a plain-text snapshot keyed by `acpSessionId`, written while the editor is open and restored when the user reopens a resumed session.

### Two observability channels (PR2 scope)

| Channel | Audience | Medium | PR2 delivers |
| --- | --- | --- | --- |
| **Transcript** (live UI) | User | In-tab ACP editor | Correlation tokens on error lines; unchanged rich rendering for live turns |
| **Session transcript file** | User | `<project>/.idea/agent-cli/transcripts/<acpSessionId>.txt` | Debounced full snapshot of plain-text render |
| **Session diagnostics** | Developer | IDE log via `AgentCliLog` (PR1) | Matching warn/error lines share correlation token with transcript errors |

**Copy session diagnostics** (new editor action): copies a clipboard bundle with correlation token(s), `configId`, `sessionId`, `launchMode`, `worktreePath`, and recent errors — for bug reports. Complements grep-by-token; does not replace it.

### Session transcript file behavior

| Property | Value |
| --- | --- |
| Scope | **ACP Client** mode only |
| Key | `acpSessionId` (not `worktreeId`) |
| Path | Workspace-local under project `.idea/agent-cli/transcripts/<acpSessionId>.txt` (not VCS) |
| Pre-session | Buffer serialized plain text in memory until `acpSessionId` is known, then create/overwrite file |
| Write | Debounced full snapshot (~300–500 ms) of plain-text render derived from `TranscriptModel` blocks |
| Restore | On editor open with resumed session: read file → replay lines as plain **Transcript** text (no block/UI reconstruction) |
| Format | User prompts as `> …`, agent text, error lines, stderr (`[stderr] …`); one line per tool call as `[tool: name …]` header only; no plan panels, tool payloads, or markdown reconstruction |

Restored history appears as plain lines until new live ACP turns arrive with full block rendering. That trade-off is intentional for v1 (see ADR 0004 alternative rejected: full block reconstruction).

PTY Passthrough: no **Session transcript file** — terminal scrollback and agent-side history suffice.

## User Stories

1. As a user running an agent in **ACP Client** mode, I want my conversation persisted automatically while the editor is open, so that I do not lose the LLM thread when I close the tab.
2. As a user who closed an ACP editor tab, I want to reopen the same resumed session and see prior conversation text restored, so that I can continue reading where I left off.
3. As a user resuming a **Worktree** with a stored `acpSessionId`, I want the **Session transcript file** for that session loaded into the **Transcript**, so that worktree-isolated sessions retain conversation history across IDE restarts.
4. As a user running an agent against the current project (no **Worktree**), I want conversation persisted keyed by `acpSessionId`, so that current-project sessions are not excluded because they lack a worktree record.
5. As a user starting a brand-new ACP session, I want early transcript lines buffered until the session ID is assigned, so that prompts and replies before ID assignment are not lost from the persisted file.
6. As a user watching streaming agent text update in the **Transcript**, I want the on-disk file to reflect the latest conversation state shortly after updates settle, so that a crash or force-quit still leaves a recent snapshot without hammering disk on every chunk.
7. As a user, I want transcript persistence to use debounced full snapshots rather than append-only writes, so that the file always matches the UI state even when agent text is updated in place during streaming.
8. As a user reading a **Session transcript file**, I want my prompts prefixed with `> `, so that user vs agent lines are visually distinct in plain text.
9. As a user reading a **Session transcript file**, I want agent message text written as plain lines, so that I can grep and read the discussion without HTML or Swing artifacts.
10. As a user, I want error lines from the agent or plugin included in the **Session transcript file**, so that failures visible in the UI are also in the saved record.
11. As a user, I want agent stderr lines (`[stderr] …`) included in the **Session transcript file**, so that CLI noise is preserved alongside the conversation.
12. As a user, I want each tool invocation represented by a single header line `[tool: name …]` in the file, so that I can see what tools ran without wading through full payloads.
13. As a user, I want tool result bodies and arguments omitted from the **Session transcript file**, so that the file stays readable and does not duplicate large diffs or file contents.
14. As a user, I want plan panel content omitted from the **Session transcript file**, so that the saved record focuses on the conversation rather than internal plan UI state.
15. As a user restoring a session, I want prior history replayed as plain transcript lines, so that I can read old turns even though live block styling is not reconstructed from disk.
16. As a user restoring a session, I want newly arriving ACP updates to render with full live **Transcript** block UI after restore, so that I get rich formatting for new activity even if restored lines are plain.
17. As a user who sees an error in the **Transcript**, I want a short correlation token such as `[agent-cli:a3f2]` appended to the error line, so that I can grep `idea.log` for the same token when reporting a bug.
18. As a developer reading **Session diagnostics** in the IDE log, I want warn/error lines for transcript failures to include the same correlation token as the user-visible error, so that one grep connects user report to stack trace detail.
19. As a user filing a bug report, I want a **Copy session diagnostics** editor action, so that I can paste session context (token, config, session ID, launch mode, worktree path, recent errors) without manually hunting through settings and logs.
20. As a developer, I want **Copy session diagnostics** to complement token grep rather than replace it, so that users can share context quickly while developers still use structured log detail.
21. As a user in **PTY Passthrough** mode, I want no **Session transcript file** created, so that the plugin does not duplicate history the terminal and external CLI already keep.
22. As a user, I want **Session transcript file** paths under project `.idea/` (workspace-local, not VCS), so that transcripts stay with the project without polluting version control.
23. As a user with multiple ACP sessions over time, I want each session's file keyed by its own `acpSessionId`, so that resuming session A does not overwrite or load session B's history.
24. As a user resuming session A after having run session B in the same project, I want only session A's file restored into the editor, so that session identity drives persistence correctly.
25. As a user editing in a **Worktree** whose `acpSessionId` was rebound after "start fresh", I want the file for the new session ID used going forward, so that persistence tracks the active ACP session not an stale ID.
26. As a user, I want auth-failure and permission-denied lines that appear in the **Transcript** represented appropriately in plain-text serialization, so that security-related failures are part of the saved record.
27. As a user, I want thought/reasoning blocks handled consistently in serialization (included or omitted per serializer rules), so that the file reflects a deliberate plain-text policy rather than accidental omission.
28. As a user reopening an editor for a session that has no prior file yet, I want an empty **Transcript** with normal live behavior, so that first-time sessions do not error on missing files.
29. As a user reopening an editor when the transcript file exists but is empty, I want restore to succeed with no visible error, so that edge cases do not block session use.
30. As a user, I want restore to tolerate normally formatted plain lines from the serializer, so that minor format evolution does not break resume.
31. As a developer implementing restore, I want replay to inject plain lines through the existing **Transcript** view path (not rebuild `TranscriptBlock` graph from disk), so that restore stays simple and aligned with ADR 0004.
32. As a developer, I want stack traces to remain in IDE log only (not copied into the **Session transcript file**), so that user-facing files stay short while **Session diagnostics** retain detail.
33. As a user seeing a recoverable failure (e.g. session load failed but picker succeeded), I want a short error line with correlation token in the **Transcript** and matching tier-1 log line (PR1), so that both audiences get appropriate detail.
34. As a user who never enabled debug logging, I still want correlation tokens on transcript errors and tier-1 IDE log lines, so that basic support workflows work out of the box.
35. As a user with `AGENT_CLI_LOG` or registry `agent_cli.log=true` (PR1), I want optional info-level **Session diagnostics** during session open and file restore, so that I can trace persistence without enabling full debug.
36. As a developer, I want transcript file write failures logged via `AgentCliLog` (PR1) at tier 1 with a correlation token when user-visible impact exists, so that silent data loss is diagnosable.
37. As a user, I want live **Transcript** rendering unchanged for in-session activity aside from correlation tokens on new error lines, so that persistence does not regress UX during an open tab.
38. As a user closing and reopening the tab within seconds, I want debounced writes to have flushed a recent snapshot, so that I do not lose the last few hundred milliseconds of conversation routinely.
39. As a user with a long-running session producing many tool calls, I want tool headers deduplicated or updated per serializer rules matching UI intent (one logical line per tool), so that the file does not explode with duplicate tool headers.
40. As a user, I want usage/cost footer state excluded from the **Session transcript file** unless explicitly included by serializer policy, so that billing metadata does not clutter the conversation record.
41. As a developer maintaining the plugin, I want a dedicated plain-text serializer seam from `TranscriptModel` blocks, so that file format policy is centralized and testable separately from Swing and debounce I/O.
42. As a developer, I want a dedicated file store seam keyed by `acpSessionId` and project root, so that path construction and read/write errors are testable with temporary directories.
43. As a developer, I want debounce timing treated as an implementation detail not asserted in tests, so that CI stays stable while still verifying snapshot correctness.
44. As a user exporting diagnostics via the editor action, I want recent errors summarized in the clipboard bundle, so that support receives actionable context without full transcript paste.
45. As a user, I want **Copy session diagnostics** available from the ACP editor chrome (action menu or equivalent existing pattern), so that I can find it where I already work with the session.
46. As a developer integrating persistence, I want the write path triggered from **Transcript** model changes (after block updates), so that the file stays consistent with `TranscriptModel.blocks()` state.
47. As a user switching between projects, I want transcript files scoped to each project's `.idea/agent-cli/transcripts/`, so that conversations do not leak across projects.
48. As a user, I want no automatic deletion or pruning of old transcript files in v1, so that I retain history until manual cleanup (accepted ADR trade-off).
49. As a developer on PR2, I want to depend on PR1 `AgentCliLog` for correlation-token log emission rather than reintroducing ad-hoc `Logger` calls, so that observability stays consistent plugin-wide.
50. As a user reading restored plain lines, I want user prompt lines to remain distinguishable after restore (`> ` prefix or equivalent plain-line marker), so that restored history matches file format expectations.

## Implementation Decisions

### Delivery boundary

- **In scope:** PR2 only — **Session transcript file** store, plain-text serializer, debounced snapshot writer, plain-line restore on resumed session open, correlation tokens on **Transcript** error lines, **Copy session diagnostics** editor action, integration hooks in ACP editor lifecycle.
- **Dependency:** PR1 (`AgentCliLog`, tier gates, migrated log sites) must land first. PR2 uses `AgentCliLog` for matching warn/error lines sharing correlation tokens and for file I/O failure diagnostics.
- **Out of PR2:** Tiered logging infrastructure itself, migration of non-transcript log sites, dialog-only logging gaps outside transcript/persistence paths.

### Mode and keying

- **Launch Mode gate:** Persistence, restore, correlation tokens on errors, and **Copy session diagnostics** apply to **ACP Client** mode only. **PTY Passthrough** editors do not create, read, or write **Session transcript file** entries.
- **Primary key:** `acpSessionId` — aligns with ACP `session/load` resume and applies to both **Worktree**-bound and current-project sessions. Do not key files by `worktreeId` (current-project runs have no worktree).
- **Pre-session buffer:** Until `acpSessionId` is known (new session handshake complete), hold serialized plain-text lines or snapshot content in memory; on ID assignment, write initial file and continue debounced snapshots.

### Session transcript file store

- Introduce **TranscriptFileStore** (name as implemented) responsible for:
  - Resolving workspace-local path: `<project>/.idea/agent-cli/transcripts/<acpSessionId>.txt`
  - Creating transcript directory on first write if missing
  - Atomic or equivalent safe overwrite of full snapshot content (debounced writer replaces entire file)
  - Reading full file text for restore; treat missing file as empty (no user error)
  - Surfacing I/O failures to `AgentCliLog` (tier 1) and optional user-visible error with correlation token when restore/write blocks expected behavior
- Scope is project/workspace-local; not checked into VCS.

### Plain-text serialization

- Introduce **TranscriptTextSerializer** (name as implemented) responsible for mapping ordered `TranscriptModel` blocks to a single plain-text string (lines separated by `\n`), applying explicit content policy:

| Block / content type | Serialized form |
| --- | --- |
| User prompt / user echo | `> …` line |
| Agent text (streaming or final) | Plain text lines (normalized newlines) |
| Plain lines (incl. stderr) | As shown in UI plain-line form, e.g. `[stderr] …` |
| Errors / auth failures | Plain error lines (correlation token appended at UI/error emission layer, not necessarily duplicated in serializer if applied before persist) |
| Tool calls | Single header line per tool: `[tool: name …]` — title/name/status summary only |
| Plan blocks | Omitted |
| Tool payloads / results / diffs | Omitted |
| Markdown / HTML structure | Flattened to plain text only where agent text blocks already extract text; no markdown reconstruction |
| Usage / cost footer | Omitted unless product decision during implementation explicitly adds a one-line summary |

- Serializer reads block list snapshot; does not depend on Swing components.
- Reuse or delegate text extraction helpers already used by **Transcript** rendering where they encode the same user-visible plain meaning (avoid divergent formatting rules).

### Debounced snapshot writer

- On **Transcript** model change (after blocks updated), schedule a debounced full snapshot write (~300–500 ms, single timer coalescing rapid updates).
- Each flush: `blocks()` → serializer → file store overwrite for current `acpSessionId`.
- Cancel or no-op writes when editor disposes, session ID changes, or mode is not ACP Client.
- Do not use append-only file semantics; streaming agent text updates in place in the model, so snapshot consistency requires full rewrite.

### Restore on resumed session open

- When **AcpAgentEditor** (or equivalent host) opens with a resumed session and known `acpSessionId`, read **Session transcript file** before or as part of transcript initialization.
- Replay strategy: split file into lines (or logical records per serializer line breaks) and inject as plain **Transcript** lines via existing plain-line / append pathways — **no** parsing `[tool: …]` back into `TranscriptBlock.ToolCallBlock`, **no** plan reconstruction, **no** markdown block rebuild.
- Restored content appears as plain history; subsequent live `SessionUpdate` traffic uses normal block pipeline.
- If file missing or unreadable: start with empty history; log via `AgentCliLog` (tier 1 on unexpected I/O error).

### Correlation tokens (transcript errors)

- For user-visible **Transcript** error lines (plugin errors, session failures surfaced in transcript, auth failures shown to user, etc.), append a stable short token format: `[agent-cli:xxxx]` (four hex chars or equivalent compact id).
- Generate token per error event (or per correlated failure group — implementer chooses one token per user-visible error line minimum).
- Emit matching `AgentCliLog.warn` / `AgentCliLog.error` (PR1) with same token in message and structured session context fields (`configId`, `sessionId`, `launchMode`, `worktreePath`).
- Stack traces and verbose detail: IDE log only, not in **Session transcript file** or user error line beyond short message + token.

### Copy session diagnostics editor action

- Register an editor action on ACP Client editor tab (follow existing editor action patterns).
- On invoke, copy to system clipboard a plain-text bundle including at minimum:
  - Active correlation token(s) from recent errors in session (or last token)
  - `configId` (agent configuration id)
  - `sessionId` (`acpSessionId`)
  - `launchMode` (ACP Client)
  - `worktreePath` if bound, else indication of current-project run
  - Recent error messages (short form)
- Does not copy full **Session transcript file** or live HTML **Transcript** — diagnostics only.
- Complements grep-by-token workflow documented in ADR 0004.

### Integration points (conceptual)

- **AcpAgentEditor** (or **TranscriptViewController** coordinator): wire store + debounced writer lifecycle to session ID availability, editor open/dispose, and model change notifications.
- **TranscriptViewController** / **TranscriptModel**: expose block change hook or reuse existing apply path to signal snapshot scheduling.
- Session ID transitions: flush buffer to new file when ID first assigned; switch file target if session rebinding occurs ( rare — document behavior: persist under current `acpSessionId` only).
- **AgentCliLog** (PR1): all new persistence/diagnostics logging through this helper.

### Testing seams (recommended)

- **Primary seams:** unit-test **TranscriptTextSerializer** (blocks → plain text) and **TranscriptFileStore** (read/write path keyed by session id with temp directories). Ideal: one serializer seam + one file store seam.
- **Restore:** if feasible, one behavioral test replaying plain lines into **TranscriptViewController** and asserting visible plain history without block reconstruction — optional but valuable.
- **Prior art:** patterns from **TranscriptBlockViewFactory** tests, **TranscriptModel** tests — reuse test builders for block lists fed to serializer.
- **Avoid:** asserting debounce delay timings; test snapshot content given a flushed write callback or direct store invoke instead.

### ADR alignment

- **ADR 0004:** Implements PR2 row of delivery table; accepts plain-text restore trade-off; rejects PTY file, worktree keying, append-only writes, full block reconstruction, in-editor diagnostics panel.
- **ADR 0001 / 0002:** ACP Client **Transcript** model unchanged in protocol terms; persistence is plugin-side observability.
- **ADR 0003:** Persistence respects per-project workspace scope; `configId` included in diagnostics bundle.

### Glossary (CONTEXT.md)

- **Transcript** — live UI (unchanged term).
- **Session transcript file** — persisted plain-text record (this PRD).
- **Session diagnostics** — IDE log detail via `AgentCliLog` (PR1 + correlation in PR2).

## Testing Decisions

### What makes a good test

- Test **external behavior** at module seams: given block lists, assert serialized plain text; given session id and temp project root, assert read/write round-trip; given restored lines, assert **Transcript** shows expected plain history.
- Do **not** test debounce timer duration, Swing layout, or HTML rendering regressions unrelated to persistence.
- Do **not** assert exact filesystem debounce scheduling — invoke flush hook or store directly in tests.

### Modules to test

- **TranscriptTextSerializer** — comprehensive cases: user prompt prefix, agent text normalization, error/plain/stderr lines, tool header only (no body), plan omission, multiple tools, empty model, streaming vs final agent text equivalence in output.
- **TranscriptFileStore** — path resolution under temp project dir, create-if-missing directory, overwrite snapshot, read missing file → empty, read after write round-trip, session id in filename isolation.
- **Restore integration (optional):** plain line replay into **TranscriptViewController** — restored lines visible; subsequent structured update still appends live blocks.
- **Correlation token (light):** unit test token format generation and that error emission path attaches token to user message string (without requiring live IDE log appender).
- **Copy session diagnostics (optional):** clipboard content contains required fields when session context stub provided — if clipboard test infrastructure exists; otherwise manual QA note.

### Modules not requiring dedicated timing tests

- Debounced writer — cover via serializer + store tests and manual/integration QA for coalescing behavior.

### Prior art

- **TranscriptModel** tests — block construction patterns for serializer inputs.
- **TranscriptBlockViewFactory** tests — plain-text / label expectations where overlap exists.
- **TranscriptSessionUpdateMapper** tests — indirect prior art for mapping events to blocks before serialization.
- Temp-directory file tests elsewhere in plugin (if present) — pattern for **TranscriptFileStore**.

### Verification

- `./gradlew qualityGate` passes after implementation.
- Manual smoke: open ACP session → converse → close tab → reopen resumed session → restored plain lines visible → new agent reply renders with live blocks → file on disk matches conversation shape → error line shows `[agent-cli:…]` → matching line grepable in log with PR1 enabled.

## Out of Scope

- **PTY Passthrough** **Session transcript file** — terminal scrollback and external CLI session history suffice.
- **ACP wire trace** (JSON-RPC in/out logging) — deferred in ADR 0004 v1; separate flag if added later.
- **Full block reconstruction on restore** — no parsing `[tool: …]` or plan data back into `TranscriptBlock` graph in v1.
- **Automatic file pruning / retention policy** — transcript files may accumulate under `.idea/agent-cli/transcripts/`; no cleanup job in v1.
- **Copy/export transcript only without disk persistence** — rejected in ADR 0004; user needs conversation after tab close without manual export each time.
- **PR1 logging infrastructure** — `AgentCliLog` introduction, tier gates, env/registry toggles, migration of existing ~8 ad-hoc log sites, dialog-only failure logging gaps unrelated to transcript persistence.
- **In-editor diagnostics panel** — YAGNI per ADR 0004; IDE log + token + **Copy session diagnostics** suffice.
- **VCS check-in of transcript files** — workspace-local `.idea/` only.
- **Markdown/HTML fidelity in Session transcript file** — plain text only.
- **Tool result bodies, plan panels, permission detail panels** in serialized file.
- **Cross-project or global transcript store** — project-local only.
- **Encrypting or redacting transcript files** — future security hardening not in v1 unless separately specified.

## Further Notes

- **ADR reference:** [docs/adr/0004-session-observability.md](../../adr/0004-session-observability.md) — authoritative for two-channel model, file path, keying, and phased PR split.
- **PR dependency:** Merge or release PR1 before PR2 so correlation tokens have `AgentCliLog` targets and session context fields on log lines.
- **Restore UX expectation:** Users see plain lines for history; tool calls from disk appear as plain `[tool: …]` lines, not collapsible tool cards, until new live tool events occur.
- **Debounced snapshot:** 300–500 ms window is a balance between data loss risk on crash and I/O churn during streaming; exact constant choosable within range.
- **Thought blocks:** Serializer should document whether `[thought]` lines are included; align with `TranscriptRenderer` plain conventions if thoughts are user-visible in UI.
- **Wiki / rules:** After implementation, update wiki observability notes and `logging-practices` rule to reference ADR 0004 (may span PR1 + PR2).
- **CHANGELOG:** Record under `### Added` for persistence, restore, correlation tokens, copy diagnostics when code ships.
- **Related glossary:** [CONTEXT.md](../../../CONTEXT.md) — **Transcript**, **Session transcript file**, **Session diagnostics**.
- **Prior consolidation work:** Transcript pipeline modules (`TranscriptModel`, `TranscriptViewController`, block factory) are integration hosts; prefer thin wiring over duplicating serialization in editor class.
