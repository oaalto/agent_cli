# 01 — Harness review follow-ups

**Parent:** `prd.md`

**What to build:** Close gaps from the post-implementation code review (standards + spec axes) so the mounted-panel harness and golden scenarios faithfully match the PRD — highest-fidelity ViewController apply path, EDT-safe Swing assertions, and hierarchy-level scroll invariants — without changing visible transcript UX.

**Blocked by:** None — can start immediately

**Status:** done

- [x] **EDT-safe assertions:** All harness tests read Swing state (scroll bar values, row preferred sizes) on EDT via harness helpers; no direct off-EDT access after row discovery
- [x] **Streaming golden scenario fidelity:** Golden scenario #1 drives chunked agent message updates through ViewController `apply` (per US11), not a single final-text append only
- [x] **Mixed tool+agent apply path:** Golden scenario #5 drives `StructuredUpdate` sequences through the apply path, not hand-built `syncBlocks` only
- [x] **Stick-to-bottom apply path:** Stick-to-bottom scenarios exercise the default ViewController apply wiring where the PRD prefers highest fidelity, reserving `syncBlocks` for pure view regressions only
- [x] **Single vertical scrollbar ownership:** Golden scenario #3 walks the mounted hierarchy and asserts one scroll pane owns vertical scrolling for the transcript column (not descendant count alone)
- [x] **Horizontal scrollbar hierarchy check:** `assertNoHorizontalScrollbar` inspects the component tree for absent horizontal scrollbars, aligned with US3 and the editor layout prior art — not scroll-pane policy flags alone
- [x] **Project fixture default:** Harness default project matches PRD CI constraint (null or minimal fixture where the panel allows)
- [x] **Scope alignment:** Extra harness surface beyond the PRD API (`applyIngest`, scroll helpers, single-scroll assertion helper) is either documented in wiki/PRD or removed; the seventh `applyIngest` scenario is renamed, extended to assert finalize ordering, or dropped
- [x] **Harness hygiene:** Consolidate duplicate apply overload bodies and shared EDT collection patterns where straightforward; document or replace the fixed EDT pump iteration count
- [x] `./gradlew qualityGate` passes; minimum five PRD golden scenarios verified on the preferred apply path
