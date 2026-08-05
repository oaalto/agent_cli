# 01 — Harness and new-session start integration

**Parent:** `prd.md`

**What to build:** A test-only session-loop harness that drives `AcpSessionController` through `InMemoryAcpTransport` with a scripted in-memory agent, plus the first end-to-end scenario: `start()` with a new-session resume plan completes with the expected session id and status message. No subprocess, no Swing — external behaviour only via the public controller interface and a recording listener.

**Blocked by:** None — can start immediately

**Status:** done

- [x] `InMemoryAcpTransport` (or adjacent test utility) supports a declarative agent script: respond to `initialize` with capabilities and to `session/new` with a deterministic session id
- [x] `AcpSessionLoopTestHarness` builds `AcpSessionControllerImpl` with in-memory transport, recording listener, fake session picker, and minimal editor context — reusing orchestrator fake patterns where they already exist
- [x] Integration test: `start(AcpSessionStartRequest)` with a new-session plan returns `AcpSessionStartResult` with session id and status message matching the scripted agent
- [x] Harness documents the session-loop phase order under test (`connect → bootstrap → bind → startSession`) as readable test structure
- [x] `./gradlew qualityGate` passes; no external agent binary required
