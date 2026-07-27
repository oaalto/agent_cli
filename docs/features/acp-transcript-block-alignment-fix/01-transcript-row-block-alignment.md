# 01 — Fix transcript row block width alignment

**Parent:** `prd.md`

**What to build:** In ACP Client mode, fenced code blocks, markdown tables, blockquotes, and thematic breaks inside transcript message rows align with inline prose on the left edge and remain fully visible within the scroll pane viewport at any window width above 400px — without horizontal scrolling.

**Blocked by:** None — can start immediately

**Status:** done

- [x] Transcript row layout resize updates preferred size for all JComponent children (not only text panes), so the vertical content column width matches the available row width after resize.
- [x] Block content no longer clips on the right edge under horizontal-scrollbar-never scroll policy; inline prose and block content share the same effective column width.
- [x] The intentional 20px left inset on code blocks, tables, blockquotes, and thematic breaks is unchanged — only container width constraint is fixed.
- [x] Unit test verifies that after the transcript row width adjustment runs, every child component has preferred width equal to the passed available width.
- [x] `./gradlew qualityGate` passes.
