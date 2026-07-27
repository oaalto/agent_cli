# 04 — Prompt executor extraction

**Parent:** `prd.md`

**What to build:** Prompt job launch, join, cancel, and event dispatch move into an internal `AcpPromptExecutor` module. Sending a prompt, cancelling an in-flight prompt, and disposing the controller while a prompt runs behave exactly as today. `AcpPromptEventDispatcher` remains the event adapter — the executor delegates to it. Isolating prompt work reduces risk that prompt fixes touch process-lifecycle code.

**Blocked by:** 03 — Session lifecycle core

**Status:** done

- [x] `AcpPromptExecutor` owns prompt job lifecycle: await session readiness, launch prompt coroutine, join, cancel, and `Event` → `StructuredUpdate` dispatch via existing dispatcher.
- [x] `cancelPrompt()` and `dispose()` cancel active prompt work and finalize agent stream — existing `AcpSessionControllerTest` updated and still passes.
- [x] `dispose()` remains synchronous and idempotent (IntelliJ `Disposable` contract preserved).
- [x] `AcpSessionControllerImpl` delegates prompt/cancel to the executor; eight-method public interface unchanged.
- [x] `./gradlew qualityGate` passes.
