# 01 — Delete orphaned HTML transcript rendering path

**Parent:** `prd.md`

**What to build:** Remove the retired monolithic `JEditorPane` HTML document path from the ACP transcript stack so only the live block-based rendering stack remains. Trim shared helpers so streaming cursor behaviour has a single live-path implementation. Users see no transcript behaviour change; the codebase no longer carries dead modules or tests that imply two rendering architectures.

**Blocked by:** None — can start immediately

**Status:** ready-for-agent

- [ ] Orphan HTML appender and pane-ops modules are deleted with no remaining production references
- [ ] Dead-path-only helpers are removed from shared cursor and render-helper modules; live-path HTML fragment builders and the live streaming cursor character are retained
- [ ] Obsolete HTML appender and HTML cursor tests are deleted
- [ ] `./gradlew qualityGate` passes with zero visible behaviour change to live transcript rendering
