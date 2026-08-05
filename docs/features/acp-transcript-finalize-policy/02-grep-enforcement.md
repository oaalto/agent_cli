# 02 — Grep enforcement for finalize construction

**Parent:** `prd.md`

**What to build:** Add CI guard that fails when `StructuredUpdate.FinalizeAgentStream` appears outside documented allowlist sites (policy module, `StructuredUpdate` definition, `TranscriptModel.apply`, test/fixture paths).

**Blocked by:** [01-policy-extraction.md](01-policy-extraction.md)

**Status:** done

- [x] Document allowlist in wiki (`acp-client.md`) and policy KDoc
- [x] Add detekt rule or simple Gradle test task that greps production sources and fails on disallowed `FinalizeAgentStream` construction
- [x] `./gradlew qualityGate` includes the check
