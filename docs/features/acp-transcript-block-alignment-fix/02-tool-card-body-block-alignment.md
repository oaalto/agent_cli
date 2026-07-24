# 02 — Fix tool card body block width alignment

**Parent:** `prd.md`

**What to build:** Code blocks and HTML content inside collapsible tool card bodies in the ACP transcript stay left-aligned and fully visible within the viewport at any window width above 400px — matching the alignment behavior fixed for transcript message rows.

**Blocked by:** None — can start immediately

**Status:** ready-for-agent

- [ ] Tool card body height adjustment applies the same preferred-size update for JComponent children as the transcript row fix — set preferred width to the available width alongside setSize and maximumSize.
- [ ] Tool card code blocks and HTML parts no longer appear shifted right or clipped at the viewport edge after window resize.
- [ ] Unit test verifies that after the tool card body adjustment runs, every JComponent child has preferred width equal to the passed available width.
- [ ] `./gradlew qualityGate` passes.
- [ ] `CHANGELOG.md` updated under `### Fixed` when both layout fixes ship.
