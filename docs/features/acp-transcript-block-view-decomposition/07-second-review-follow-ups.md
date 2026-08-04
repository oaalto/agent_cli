# 07 — Second review follow-ups

**Parent:** `prd.md`

**What to build:** Close remaining gaps from the post–ticket-06 code review so block-view decomposition is fully verified and feature-complete before package restructure. Fix the simple-text color-provider contract regression, tighten agent-text finalize disposal assertions, add missing adapter-level regression tests, and complete feature housekeeping (ticket statuses, master list).

**Blocked by:** None — can start immediately

**Status:** done

- [x] **Simple-text color provider:** Simple-text row binding honours `RowContext.colorProvider` (injected/testable) instead of resolving colours only via IDE service lookup inside `bindTranscriptBlock`; headless tests with a custom provider prove theme colours apply
- [x] **Finalize disposal assertion:** Streaming → finalized agent text test asserts code-editor components are actually disposed (`disposedCount >= 1`), not a tautology that always passes
- [x] **Simple-text column resize test:** Headless test that narrowing column width remeasures wrapped simple-text row height (parity with tool-card resize coverage added in ticket 06)
- [x] **Simple-text mismatch adapter test:** Adapter-level regression: `SimpleTextRow` + non–simple-text block returns `false` from `update()` and preserves prior bound text (the exact bug ticket 06 fixed)
- [x] **Feature close-out:** Mark ticket 06 and this ticket `done`; move `acp-transcript-block-view-decomposition` to **Implemented** in `FEATURES.md`; refresh architecture-conflict note so package restructure is unblocked
- [x] `./gradlew qualityGate` passes; zero visible transcript UX change across all block families
