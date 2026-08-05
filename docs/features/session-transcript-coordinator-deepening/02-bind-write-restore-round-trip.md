# 02 — Bind-write-restore round-trip integration test

**Parent:** `prd.md`

**What to build:** Add `SessionTranscriptCoordinatorTest` proving the core **Session transcript file** round-trip through the facade: `bindSession` → `onBlocksChanged` → advance debounce → file on disk contains expected plain text → `restoreIfPresent` → restore sink receives lines in order. Cover representative block families from the v1 contract — user echo (`> ` prefix in file), finalized agent text, and tool-call header line — without exercising Swing or a live ACP process. This is the regression guard developers run instead of the full editor when serialization or coordinator wiring breaks.

**Blocked by:** 01 — Coordinator test seam injection

**Status:** ready-for-agent

- [ ] Test drives coordinator end-to-end with injectable store and scheduler; no `TranscriptPanel` or `AcpAgentEditor` in the test classpath usage
- [ ] After bind and debounced flush, temp file content matches serializer output for mixed user echo, agent text, and tool header blocks
- [ ] `restoreIfPresent` delivers plain lines to the restore sink in file order; user echo lines map through coordinator restore (not only `TranscriptViewController` in isolation)
- [ ] Plan blocks in the blocks provider are omitted from written file per ADR 0004
- [ ] `./gradlew qualityGate` passes
