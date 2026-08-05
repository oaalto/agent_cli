# 01 — Policy extraction and caller migration

**Parent:** `prd.md`

**What to build:** Introduce `TranscriptFinalizePolicy` as the orchestration-layer gate for `FinalizeAgentStream` emission. Migrate `TranscriptEventIngestion`, `AcpPromptExecutor`, and `AcpAgentEditor` off direct finalize construction. Add table-driven policy tests; adjust ingestion tests to delegate finalize expectations.

**Blocked by:** None — can start immediately

**Status:** done

- [x] `TranscriptFinalizePolicy` (`internal object`) exposes `finalizePrelude(SessionUpdate)`, `onPromptStarting()`, `onPromptResponse()`, `onPromptFlowCompleted()`, `onPromptFailed()`, `onPromptInterrupted()` — each returns `List<StructuredUpdate>` (typically 0 or 1 `FinalizeAgentStream`)
- [x] `finalizePrelude` emits finalize for all `SessionUpdate` variants except `AgentMessageChunk`; blank chunks still short-circuit in ingestion with no finalize
- [x] `TranscriptEventIngestion.ingest` composes `policy.finalizePrelude(update) + mapUpdate(update)`; remove `ingestPromptCompleted()` public API
- [x] `AcpPromptExecutor` delegates all five finalize sites to policy hooks (`onPromptStarting`, `onPromptResponse`, `onPromptFlowCompleted`, `onPromptFailed`, `onPromptInterrupted`)
- [x] `AcpAgentEditor` delegates prompt-start and error paths to `onPromptStarting()` / `onPromptFailed()`; keep `TranscriptViewController.finalizeAgentStream()` as thin `apply` wrapper only
- [x] `TranscriptFinalizePolicyTest` — table-driven sequences: `[chunk, chunk, tool_update]`, `[chunk, prompt_complete]`, `[chunk, error]`, blank non-chunk → finalize with empty map, redundant finalize idempotency via model
- [x] `TranscriptEventIngestionTest` retains mapping coverage; finalize ordering assertions reference policy tables or shared fixtures
- [x] `./gradlew qualityGate` passes; manual smoke: streaming reply → tool call → final code block visible without second prompt
