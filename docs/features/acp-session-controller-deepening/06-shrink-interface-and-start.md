# 06 — Shrink public interface and compose start()

**Parent:** `prd.md`

**What to build:** Replace the eight-method `AcpSessionController` surface with four editor-intent operations: `start`, `prompt`, `cancelPrompt`, and `dispose`. Introduce `AcpSessionStartRequest` (bundling launch plan, editor context, resume plan, and session picker) and `AcpSessionStartResult` (session id + human-readable status message). `start()` connects, bootstraps, and runs resume orchestration in one call — it does not return until the session is open or fails, eliminating the editor race between connect and session open. `AcpSessionControllerImpl` becomes a thin coordinator composing transport, bootstrap, lifecycle, and prompt executor modules (~60–80 lines). Update `RecordingSessionController` to the new interface.

**Blocked by:** 04 — Prompt executor extraction; 05 — Resume orchestration in session lifecycle

**Status:** ready-for-agent

- [ ] Public interface exposes only `start(request)`, `prompt(text)`, `cancelPrompt()`, and `dispose()` plus the new request/result/picker types.
- [ ] `start()` composes transport → bootstrap → lifecycle resume → returns `AcpSessionStartResult` with correct `sessionId` and status message for each resume path.
- [ ] `prompt`, `cancelPrompt`, and `dispose` delegate to the prompt executor; dispose remains synchronous and idempotent.
- [ ] `RecordingSessionController` and `AcpSessionControllerTest` updated; tests assert `start()` result for at least new-session and one resume variant.
- [ ] `AcpAgentEditor` may still call old methods if not yet migrated — if so, keep a temporary internal bridge or land editor migration in the same PR only when both tickets ship together; prefer leaving editor on old API until ticket 07 if needed for green CI between tickets.
- [ ] `./gradlew qualityGate` passes.
