# 05 — Auth-required bootstrap integration (stretch)

**Parent:** `prd.md`

**What to build:** Optional v1 stretch — harness scenario where the scripted agent requires authentication during bootstrap. `AuthFlowCoordinator` is satisfied via stubbed editor auth UI (no `AuthPromptPanel` visual test), proving initialize → auth → session open composes through `start()` without a subprocess.

**Blocked by:** 01 — Harness and new-session start integration

**Status:** done

- [x] Agent script or capabilities response signals auth required; harness supplies stub auth coordinator inputs through minimal `AcpEditorContext` fakes
- [x] Integration test: `start()` completes after stub auth succeeds and returns expected session result
- [x] Integration test: auth failure or cancellation surfaces as start failure per current bootstrap contract
- [x] `./gradlew qualityGate` passes
