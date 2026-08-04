# 01 — Row adapter scaffold and simple text extraction

**Parent:** `prd.md`

**What to build:** Introduce the row adapter seam (`TranscriptBlockRowAdapter`, `RowContext`) and convert the block view factory into a thin coordinator that dispatches to adapters. Extract `SimpleTextRowAdapter` for user echo, thought, plain line, error line, and auth-failure blocks — with its own single-`JTextPane` row shell (separate from agent text rows per ADR 0006). Simple transcript rows render and update exactly as today; non-simple block families continue through the existing inline factory path until later tickets migrate them.

**Blocked by:** None — can start immediately

**Status:** done

- [x] `TranscriptBlockRowAdapter` interface exposes `matches`, `create`, `update`, `dispose`; `RowContext` carries session-wide view deps (`project`, `columnWidth`, `codeBlockViewFactory`, `colorProvider`, `logContextProvider`) — `onToolToggle` is not on `RowContext`
- [x] Coordinator registers `SimpleTextRowAdapter` last in the fixed order (Tool → Plan → Agent text → Simple text); simple adapter `matches` only its block families on a `JTextPane`-centric row shell
- [x] `SimpleTextRowAdapter` delegates presentation to `bindTranscriptBlock`; streaming and finalized agent text remain on the legacy path for now
- [x] Type mismatch at a reused `blockId` logs and does not auto-recreate (preserve current panel sync behaviour)
- [x] Headless EDT tests mount `SimpleTextRowAdapter` output and assert colours, text, and preferred heights for representative simple block types
- [x] `./gradlew qualityGate` passes; zero visible transcript UX change
