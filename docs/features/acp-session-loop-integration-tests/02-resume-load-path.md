# 02 — Resume load path integration

**Parent:** `prd.md`

**What to build:** Extend the session-loop harness with a resume scenario: `start()` with `LaunchResumePlan.AcpLoad` drives the load branch through the public controller interface and proves the scripted agent received `session/load` (or equivalent recording on session operations).

**Blocked by:** 01 — Harness and new-session start integration

**Status:** ready-for-agent

- [ ] Agent script handles `session/load` with a deterministic success response for a known session id
- [ ] Integration test: `start()` with `AcpLoad` plan returns success result with the loaded session id and `restoreTranscript` behaviour per current contract
- [ ] Test asserts the load path was exercised (outbound `session/load` or recording ops), not only the returned result shape
- [ ] `./gradlew qualityGate` passes
