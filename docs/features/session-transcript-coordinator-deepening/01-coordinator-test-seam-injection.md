# 01 — Coordinator test seam injection

**Parent:** `prd.md`

**What to build:** Refactor `SessionTranscriptCoordinator` so integration tests can drive the full persistence stack without Swing, live ACP, or real `.idea/` paths. Production wiring stays equivalent: the editor still passes callbacks for blocks, restore, logging, and context. Tests inject a fake or temp `TranscriptFileStore`, a controllable debounce scheduler (same pattern as `DebouncedTranscriptSnapshotWriterTest`), and a restore sink that captures plain lines. No change to on-disk format, debounce interval, or user-visible restore behaviour.

**Blocked by:** None — can start immediately

**Status:** ready-for-agent

- [ ] Coordinator constructor (or internal factory used only from editor) accepts injectable file store and debounce scheduler; production defaults preserve current behaviour
- [ ] Restore sink is observable in tests via existing `SessionTranscriptCallbacks.restoreLines` / `appendPlainLine` without mounting `TranscriptPanel`
- [ ] `AcpAgentEditor` wiring unchanged from the user's perspective — session still binds, snapshots debounce, and restores on resume as today
- [ ] `./gradlew qualityGate` passes
