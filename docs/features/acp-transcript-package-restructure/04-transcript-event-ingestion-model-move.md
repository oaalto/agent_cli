# 04 — Transcript event ingestion model move

**Parent:** `prd.md`

**What to build:** Move `TranscriptEventIngestion` into `transcript/model/` now that render types exist, preserving the accepted exception: ingestion alone may import render (tool `bodyParts` mapping). Relocate ingestion tests to the mirrored test package. Verify ACP session events still map to `StructuredUpdate` values and finalize prelude still delegates to `TranscriptFinalizePolicy`.

**Blocked by:** 03 — Transcript render package move

**Status:** done

- [x] `TranscriptEventIngestion` lives in `transcript/model` with tests mirrored
- [x] Ingestion imports render only for tool body mapping and display-title helpers — no view/Swing imports in model
- [x] Existing ingestion and finalize-policy tests pass unchanged aside from import paths
- [x] `./gradlew qualityGate` passes
