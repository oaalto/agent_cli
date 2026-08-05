# 03 — Pre-id buffer and missing-file restore

**Parent:** `prd.md`

**What to build:** Coordinator-level tests for session lifecycle edge cases: blocks accumulated before `acpSessionId` is known are not lost when `bindSession` runs and flush completes, and `restoreIfPresent` when no transcript file exists is a silent no-op with no error line appended. Users resuming or starting sessions see the same behaviour as today; tests make the implicit contract explicit at the facade.

**Blocked by:** 01 — Coordinator test seam injection

**Status:** ready-for-agent

- [ ] `onBlocksChanged` before `bindSession` → after bind and flush, file includes buffered content
- [ ] `restoreIfPresent` with no existing file: restore sink not called (or receives empty), no `appendPlainLine` error, no log-warn callback from restore path
- [ ] `./gradlew qualityGate` passes
