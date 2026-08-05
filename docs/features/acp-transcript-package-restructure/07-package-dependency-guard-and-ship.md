# 07 — Package dependency guard and ship

**Parent:** `prd.md`

**What to build:** Add a CI package-dependency grep test enforcing the accepted matrix (model except ingestion → no view/Swing; ingestion → render only; render → model+theme, no Swing; view → model+render+theme; plan → model+theme/render, not view). Refresh wiki path-map entries if source paths changed on disk, run wiki lint, and record the structural repackage under `CHANGELOG.md` `### Changed`. `./gradlew qualityGate` is the final ship gate.

**Blocked by:** 06 — Cross-slice import sweep and root boundary check

**Status:** ready-for-agent

- [ ] Package dependency grep test fails on forbidden edges (mirror existing construction-guard test style)
- [ ] Existing construction guards (`FinalizeAgentStream`, fence normalizer) still pass
- [ ] Wiki path-map and subsystem page source references match on-disk layout; wiki lint clean
- [ ] `CHANGELOG.md` `### Changed` entry records structural repackage (separate from grill-accept documentation bullet)
- [ ] `./gradlew qualityGate` passes
