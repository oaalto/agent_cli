# 08 — Third review follow-ups

**Parent:** `prd.md`

**What to build:** Close remaining verification and documentation gaps from the post–ticket-07 code review so block-view decomposition is honestly tested and tracked before package restructure. Tighten resize and colour-provider regression tests, align disposal test naming with actual transition semantics, correct overstated changelog claims, and finish git/tracker housekeeping.

**Blocked by:** None — can start immediately

**Status:** done

- [x] **Agent streaming colour test:** Headless test proves `AgentTextRowAdapter` streaming path honours injected `RowContext.colorProvider` (not only simple-text adapter)
- [x] **Simple-text resize height assertion:** Column-narrowing test asserts wrapped row height changes (before/after), not only text-pane width — parity with tool-card resize coverage
- [x] **Disposal test semantics:** Rename or split agent-text disposal test so name and criterion match behaviour — final→stream disposes code editors; stream→final rebuild does not dispose (document that path instead of mislabeling)
- [x] **Colour-provider breadth:** At least one additional simple-text block family (e.g. Error or Thought) verified with custom provider colours
- [x] **Changelog accuracy:** Correct changelog entries that claim agent colour coverage or stream→final disposal before tests actually prove those behaviours
- [x] **Tracker and git hygiene:** Ticket files 06–08 committed; `FEATURES.md` summary counts match active frontier (no stale ready-for-agent ticket count); feature remains **Implemented** once this ticket is `done`
- [x] `./gradlew qualityGate` passes; zero visible transcript UX change
