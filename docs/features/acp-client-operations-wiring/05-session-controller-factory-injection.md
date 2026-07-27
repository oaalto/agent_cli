# 05 — Session controller factory injection

**Parent:** `prd.md`

**What to build:** Session open no longer buries operations construction inside a lambda calling a static companion. The session controller depends on the operations factory interface; `openSession` invokes the factory so tests can inject a recording double and verify the seam without constructing real filesystem dependencies.

**Blocked by:** 04 — Session operations composition root

**Status:** done

- [ ] `AcpSessionControllerImpl` receives the session operations factory via constructor or editor context — not by calling the adapter companion directly.
- [ ] `openSession` builds `ClientOperationsFactory` from the injected factory's `create(context)` call.
- [ ] A session controller test with an injectable recording factory verifies the factory is invoked when a session opens.
- [ ] Existing session controller dispose behaviour remains intact.
- [ ] `./gradlew qualityGate` passes.
