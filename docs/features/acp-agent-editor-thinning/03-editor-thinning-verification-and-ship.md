# 03 — Editor thinning verification and ship

**Parent:** `prd.md`

**What to build:** Confirm `AcpAgentEditor` is a view adapter: layout composition, `AcpSessionListener` routing to `TranscriptViewController` and footer, prompt submit delegation, and dispose cleanup. EDT helpers stay inline unless duplicate extraction is required to meet the line budget. Ship with quality gate green and optional thin controller-factory test.

**Blocked by:** 01 — Executor-only finalize on prompt submit; 02 — Thin session start delegate

**Status:** ready-for-agent

- [ ] `AcpAgentEditor` is predominantly wiring (target ~250 lines; auth/permission suspend UI may keep the file above that — note residual surface in CHANGELOG if so)
- [ ] `AcpEditorLayoutTest` remains the primary editor unit test and passes; existing controller, lifecycle, and orchestrator tests pass unchanged
- [ ] Optional (lower priority): editor test with mocked `sessionControllerFactory` asserting `start` receives expected `AcpSessionStartRequest` fields via `RecordingSessionController` pattern
- [ ] `./gradlew qualityGate` passes; manual smoke: open editor, resume worktree, submit prompt, trigger permission or auth suspend UI, dispose tab — all match prior behaviour
- [ ] `CHANGELOG.md` updated under `### Changed`; feature marked complete in `FEATURES.md` when all tickets are `done`
