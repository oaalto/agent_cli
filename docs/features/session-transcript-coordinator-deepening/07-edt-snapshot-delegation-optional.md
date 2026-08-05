# 07 — EDT snapshot delegation (optional)

**Parent:** `prd.md`

**What to build:** If ticket 02's seam shows editor-owned `snapshotBlocksOnEdt` is the main coupling point, move EDT snapshot policy behind a coordinator-supplied callback so `AcpAgentEditor` passes `() -> List<TranscriptBlock>` without owning `SwingUtilities` invoke-and-wait logic. Skip this ticket if the prefactor already keeps EDT policy in the editor with no measurable coupling win — document skip in ticket status. No change to on-disk format or restore UX.

**Blocked by:** 02 — Bind-write-restore round-trip integration test

**Status:** ready-for-agent

- [ ] Either: editor delegates block snapshot through coordinator callback and EDT policy lives in coordinator (or a single coordinator-owned helper); round-trip tests still pass
- [ ] Or: ticket marked done with note that editor coupling is acceptable and EDT delegation deferred — no speculative abstraction added
- [ ] User-visible transcript persistence and restore unchanged from manual smoke
- [ ] `./gradlew qualityGate` passes; `CHANGELOG.md` updated under `### Changed` when code ships (final ticket in feature slice)
