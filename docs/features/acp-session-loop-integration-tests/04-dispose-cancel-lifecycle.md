# 04 — Dispose and cancel lifecycle integration

**Parent:** `prd.md`

**What to build:** Harness scenarios for teardown and in-flight cancellation: `dispose()` after `start()` is safe; `dispose()` or `cancelPrompt()` during an in-flight `prompt` leaves listener and transport state consistent; second `start()` behaviour matches the current controller contract (blocked or throws).

**Blocked by:** 03 — Prompt loop and ingestion integration

**Status:** done

- [x] Integration test: `dispose()` after successful `start()` does not throw; transport is disposed and prompt work is cancelled
- [x] Integration test: `dispose()` or `cancelPrompt()` during a slow scripted prompt stream completes without leak or hang
- [x] Integration test: second `start()` after `dispose()` follows current contract (documented assertion on blocked vs allowed restart)
- [x] `./gradlew qualityGate` passes
