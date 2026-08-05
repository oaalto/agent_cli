# 05 — Error paths and diagnostics clipboard

**Parent:** `prd.md`

**What to build:** Coordinator tests for error decoration and diagnostics assembly without the editor: `decorateError` returns user-visible message plus correlation token and records diagnostics; write failure on flush invokes warn callback with token; restore failure appends short error line with token; `clipboardText` returns expected bundle shape (tokens, session context fields, worktree label) from stubbed log context. Users copying session diagnostics and seeing transcript errors behave as today; tests lock the facade to tiered-session-diagnostics pairing.

**Blocked by:** 01 — Coordinator test seam injection

**Status:** ready-for-agent

- [ ] `decorateError` output includes `[agent-cli:…]` token; diagnostics collector records the event
- [ ] Simulated file write failure on flush triggers `logWarn` with matching token (no stack trace in user-visible path)
- [ ] Simulated restore failure triggers `appendPlainLine` error with token
- [ ] `clipboardText` with stubbed `AgentCliSessionContext` includes correlation entries and context fields; does not embed full transcript file body
- [ ] `./gradlew qualityGate` passes
