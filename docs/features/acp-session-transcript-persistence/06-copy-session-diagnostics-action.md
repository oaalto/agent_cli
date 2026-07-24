# 06 — Copy session diagnostics editor action

**Parent:** `prd.md`

**What to build:** Register a **Copy session diagnostics** editor action on the **ACP Client** editor tab (existing editor action / menu pattern). On invoke, copy a plain-text clipboard bundle with at minimum: active correlation token(s) from recent session errors (or last token), `configId`, `sessionId` (`acpSessionId`), `launchMode` (**ACP Client**), `worktreePath` when bound or an indication of current-project run, and recent error messages (short form). Do not copy full **Session transcript file** or live HTML **Transcript** — diagnostics only. Complements grep-by-token; does not replace it. **ACP Client** only; not registered for **PTY Passthrough**.

**Blocked by:** 05 — Correlation tokens and Session diagnostics log pairing; tiered-session-diagnostics PR1 merged (**AgentCliLog** available)

**Status:** done

- [x] Editor action available from ACP editor chrome (action menu or equivalent existing pattern) in **ACP Client** mode only
- [x] Clipboard bundle includes correlation token(s), `configId`, `sessionId`, `launchMode`, worktree path or current-project indication, and recent error summaries
- [x] Action does not paste full transcript file or rich HTML transcript content
- [x] Optional: clipboard content test with stubbed session context when clipboard test infrastructure exists; otherwise document manual QA step
- [x] `./gradlew qualityGate` passes; `CHANGELOG.md` updated under `### Added` when code ships (final ticket in feature slice)
