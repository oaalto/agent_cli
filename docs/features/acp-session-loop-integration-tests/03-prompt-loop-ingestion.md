# 03 — Prompt loop and ingestion integration

**Parent:** `prd.md`

**What to build:** Harness scenario that calls `prompt(text)` after a successful `start()` and asserts the recording listener receives the expected `StructuredUpdate` sequence when the scripted agent emits `SessionUpdate` chunks (including `agent_message_chunk` and completion). Exercises the real prompt executor → ingestion → listener path and ADR 0007 finalize ordering at loop level.

**Blocked by:** 01 — Harness and new-session start integration

**Status:** done

- [x] Agent script handles `session/prompt` by emitting a deterministic sequence of `SessionUpdate` events (chunks plus completion)
- [x] Integration test: `prompt(text)` delivers scripted updates to the recording listener in the expected order
- [x] Test asserts `FinalizeAgentStream` (or equivalent finalize prelude) precedes non-chunk ingestion output when the script includes a tool or non-chunk event after streaming chunks
- [x] `./gradlew qualityGate` passes
