# 05 — Correlation tokens and Session diagnostics log pairing

**Parent:** `prd.md`

**What to build:** Append a short correlation token (format `[agent-cli:xxxx]`, four hex chars or equivalent compact id) to user-visible **Transcript** error lines in **ACP Client** mode — plugin errors, session failures surfaced in transcript, auth failures shown to user, and similar paths. Emit matching **AgentCliLog** warn/error lines with the same token and structured session context (`configId`, `sessionId`, `launchMode`, `worktreePath`). Stack traces and verbose detail stay in IDE log only, not in the **Session transcript file** or user error line beyond short message + token. Wire **TranscriptFileStore** read/write failure paths to **AgentCliLog** tier 1 when user-visible impact exists, using correlation tokens where appropriate.

**Blocked by:** 03 — Debounced snapshot writer (persistence paths to instrument); tiered-session-diagnostics PR1 merged (**AgentCliLog** available)

**Status:** done

- [x] User-visible transcript error lines include `[agent-cli:…]` token; one token minimum per user-visible error event
- [x] Matching **AgentCliLog.warn** / **AgentCliLog.error** lines include the same token plus session context fields
- [x] Stack traces never copied into **Session transcript file** or user-facing error text beyond short message + token
- [x] Transcript file write/read failures with user-visible impact log via **AgentCliLog** tier 1 with correlation token when applicable
- [x] Light unit test: token format generation and error emission attaches token to user message string (no live IDE log appender required)
- [x] Tier-1 emission works without `AGENT_CLI_LOG` / registry flags enabled (PR1 tier-1 always-on rule)
- [x] `./gradlew qualityGate` passes
