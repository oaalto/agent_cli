# 03 — Merge transcript event ingestion into single deep module

**Parent:** `prd.md`

**What to build:** Consolidate ACP session-update handling into one ingestion module that owns finalize-before-non-chunk policy and all `SessionUpdate` → `StructuredUpdate` mapping. Callers route prompt events through this single seam instead of a separate dispatcher and mapper. Text-extraction helpers currently living under a misnamed renderer module are absorbed so ingestion owns the full event-to-structured-update path.

**Blocked by:** 01 — Delete orphaned HTML transcript rendering path

**Status:** ready-for-agent

- [ ] One ingestion module exposes `ingest(SessionUpdate)` and `ingestPromptCompleted()` returning `StructuredUpdate` lists
- [ ] Finalize-before-non-chunk policy and every `SessionUpdate` variant mapping live together with clear ownership
- [ ] Session notification and prompt-completed routing call the merged seam instead of separate dispatcher and mapper objects
- [ ] Text-extraction helpers are absorbed into ingestion or a package-private content-text helper; the misnamed renderer module no longer owns them
- [ ] Mapper unit tests are migrated to the merged module; finalize-on-non-chunk cases are covered (including policy cases previously tested alongside the deleted HTML appender tests)
- [ ] Former dispatcher and mapper modules are deleted
- [ ] `./gradlew qualityGate` passes; transcript behaviour is unchanged when verified through the `StructuredUpdate` interface
