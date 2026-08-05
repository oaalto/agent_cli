# 04 — Legacy transcript restore through coordinator

**Parent:** `prd.md`

**What to build:** Integration test that restores a flat legacy `{sessionId}.txt` transcript through `SessionTranscriptCoordinator.restoreIfPresent`, not only through `TranscriptFileStore.read` in isolation. Users with older transcript files still see conversation restored on resume; the coordinator remains the single entry point for restore policy.

**Blocked by:** 01 — Coordinator test seam injection

**Status:** ready-for-agent

- [ ] Fixture uses legacy flat filename under the transcripts directory (same layout as `TranscriptFileStoreTest` legacy case)
- [ ] `restoreIfPresent` reads legacy content and passes it to the restore sink
- [ ] `./gradlew qualityGate` passes
