# 02 — Transcript model core package move

**Parent:** `prd.md`

**What to build:** Relocate transcript state and mutation-policy types (`StructuredUpdate`, `TranscriptBlock`, `TranscriptModel`, `TranscriptBodyPart`, `TranscriptFinalizePolicy`) into `transcript/model/` with mirrored tests. Update project-wide imports for moved types. Leave `TranscriptEventIngestion` at its current location until the render package exists (ingestion maps tool bodies through the content renderer).

**Blocked by:** 01 — Transcript theme package move

**Status:** done

- [x] Model core types and their unit tests live under `transcript/model`
- [x] `TranscriptFinalizePolicy` construction guard test still passes (filename allowlist unchanged)
- [x] Cross-slice consumers (`plan/`, session orchestration, transport) compile against new model package paths
- [x] `./gradlew qualityGate` passes with no logic diffs outside package/import lines
