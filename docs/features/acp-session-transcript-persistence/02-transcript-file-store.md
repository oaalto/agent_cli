# 02 — TranscriptFileStore

**Parent:** `prd.md`

**What to build:** A **TranscriptFileStore** keyed by `acpSessionId` and project workspace root. Resolve workspace-local **Session transcript file** location under project `.idea/agent-cli/transcripts/`, create the transcript directory on first write, atomically overwrite full snapshot content on write, read full file text for restore (missing file → empty, no user error), and isolate files per session id. Surface read/write failures to callers for later observability wiring; do not integrate **PTY Passthrough** — **ACP Client** scope only at call sites that adopt this store.

**Blocked by:** None — can start immediately (parallel with 01)

**Status:** ready-for-agent

- [ ] **TranscriptFileStore** resolves path from project root + `acpSessionId`; filename includes session id for isolation between sessions
- [ ] First write creates transcript directory if missing; subsequent writes replace entire file content (snapshot semantics, not append)
- [ ] Read of missing file returns empty content without throwing to callers expecting restore
- [ ] Read-after-write round-trip and overwrite behavior verified with temporary project directories (temp-dir test pattern)
- [ ] I/O failures are surfaced to callers (result/exception seam) so debounced writer and restore paths can react; **AgentCliLog** pairing deferred to ticket 05
- [ ] `./gradlew qualityGate` passes
