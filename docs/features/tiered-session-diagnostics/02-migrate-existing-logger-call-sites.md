# 02 — Migrate existing Logger call sites to AgentCliLog

**Parent:** `prd.md`

**What to build:** Replace ad-hoc direct `Logger` usage across the plugin with `AgentCliLog` so every existing session-related log line uses the shared tier model and session context. ACP Client and PTY Passthrough editor lifecycles, session controller connect/open/load/prompt failures, MCP server resolution, transcript block rendering, agent configuration toolbar selection, plan update mapping, and worktree deletion from the run split button all route through the helper. Recoverable failures (for example session load failing before session-picker fallback) log at tier 1 even when the UI recovers. Plan update mapper reflection failures move from tier 1 to tier 3 only. When tier 2 is enabled, operational milestones such as successful session open, MCP resolution summary, and agent subprocess exit code emit at `info`. Visible UI behavior and error dialogs are unchanged apart from the intentional plan-mapper severity reduction.

**Blocked by:** 01 — AgentCliLog helper and tier gate functions

**Status:** done

- [x] Agent configuration toolbar selection failures use `AgentCliLog` with configuration context when known
- [x] ACP agent editor lifecycle logging uses `AgentCliLog` with launch mode **ACP Client** and available session context
- [x] ACP session controller connect, session open/load/list, and prompt errors use `AgentCliLog` with `sessionId` when assigned; recoverable load failures log at tier 1 before picker fallback
- [x] Transcript block view factory rendering failures use `AgentCliLog` with session context from the active editor/session when available
- [x] MCP server sources log resolution failures at tier 1 and resolution success summary at tier 2 when the log tier is enabled
- [x] Plan update mapper reflection failures emit at tier 3 (`debug`) only — no tier 1 `warn` remains for optional reflection paths
- [x] PTY agent editor lifecycle logging uses `AgentCliLog` with launch mode **PTY Passthrough** and the same tier gates as ACP Client
- [x] Delete-worktree action from the run split button logs deletion failures at tier 1 with worktree path context
- [x] No agent stderr mirroring into IDE log; no transcript file, correlation tokens, or copy-diagnostics action
- [ ] Optional secondary test: one representative migrated component asserts tier/context behavior with a test double or recording fake if a low-cost pattern already exists in the codebase
- [x] `./gradlew qualityGate` passes; `CHANGELOG.md` updated noting plan-mapper reflection messages moved from warn to debug
