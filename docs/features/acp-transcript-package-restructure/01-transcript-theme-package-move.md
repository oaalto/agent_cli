# 01 — Transcript theme package move

**Parent:** `prd.md`

**What to build:** Relocate shared transcript theme types (`TranscriptColorProvider`, `TranscriptPalette`, `TranscriptBadgeStyle`) into `transcript/theme/` with matching test package layout. Update all importers (render helpers, view components, plan panels still at their current locations) so `./gradlew qualityGate` passes with zero behaviour change.

**Blocked by:** None — can start immediately

**Status:** done

- [x] Theme types live under `com.oaalto.agent.acp.transcript.theme` with tests mirrored under `transcript/theme`
- [x] Plan panel and HTML badge rendering still resolve theme colors correctly (headless or existing tests pass unchanged)
- [x] No new runtime behaviour; diff is moves, package declarations, and import fixes only
- [x] `./gradlew qualityGate` passes
