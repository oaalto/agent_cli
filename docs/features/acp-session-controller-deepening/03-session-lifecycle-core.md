# 03 — Session lifecycle core

**Parent:** `prd.md`

**What to build:** Session open, load, list, and readiness synchronization move into an internal `AcpSessionLifecycle` module. The `sessionReady` deferred pattern, `newSession` / `loadSession` / `listSessions`, `ClientOperationsFactory` wiring to `AcpClientSessionOperationsImpl`, and `currentSessionId` semantics live here instead of inline in the controller impl. Resume-plan branching is **out of scope** for this ticket — only the primitive session operations. Editor startup with `AcpNewSession` (and direct load when the editor still calls `loadSession`) behaves identically.

**Blocked by:** 02 — Connection bootstrap extraction

**Status:** done

- [x] `AcpSessionLifecycle` owns `sessionReady`, session open/load/list, operations factory creation, and post-bootstrap `editorContext` / `launchPlan` retention.
- [x] `prompt()` still awaits session readiness before sending — same semantics as today's `awaitOpenSession()`.
- [x] `listSessions` maps SDK session info to `SessionSummary` as today.
- [x] `AcpSessionControllerImpl` delegates session primitives to lifecycle; eight-method public interface unchanged.
- [x] `./gradlew qualityGate` passes.
