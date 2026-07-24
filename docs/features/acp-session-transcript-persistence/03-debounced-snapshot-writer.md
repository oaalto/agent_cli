# 03 — Debounced snapshot writer

**Parent:** `prd.md`

**What to build:** Wire a debounced full-snapshot writer to **TranscriptModel** block-change notifications in **ACP Client** mode. On each coalesced flush (~300–500 ms, single timer — exact delay not tested), take `blocks()` → **TranscriptTextSerializer** → **TranscriptFileStore** overwrite for the current `acpSessionId`. Until `acpSessionId` is known, buffer serialized plain text in memory; on first ID assignment, write initial file and continue debounced snapshots. Cancel or no-op when the editor disposes, session id changes, or launch mode is not **ACP Client**. Do not use append-only semantics.

**Blocked by:** 01 — TranscriptTextSerializer; 02 — TranscriptFileStore

**Status:** ready-for-agent

- [ ] Model change hook schedules debounced snapshot writes; rapid updates coalesce to one pending flush
- [ ] Each flush serializes current blocks and overwrites the session transcript file for the active `acpSessionId`
- [ ] Pre-session buffer holds plain text until `acpSessionId` is assigned, then creates/overwrites file and continues normal debounced writes
- [ ] Writer stops or no-ops on editor dispose, session id transition/rebind (target switches to current `acpSessionId` only), and non–**ACP Client** launch modes (**PTY Passthrough** creates no file)
- [ ] Tests invoke flush hook or store directly — do **not** assert debounce timer duration
- [ ] Snapshot content correctness covered by combining serializer + store assertions (given flushed blocks, file content matches expected plain text)
- [ ] `./gradlew qualityGate` passes
