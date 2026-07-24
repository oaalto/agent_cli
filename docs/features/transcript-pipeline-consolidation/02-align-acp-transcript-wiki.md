# 02 — Align ACP transcript wiki with live stack

**Parent:** `prd.md`

**What to build:** Update ACP transcript wiki pages so onboarding and subsystem docs describe only the live stack (`TranscriptViewController` → `TranscriptModel` → `TranscriptPanel`) and no longer cite the deleted HTML appender or obsolete streaming state facts.

**Blocked by:** 01 — Delete orphaned HTML transcript rendering path

**Status:** ready-for-agent

- [x] ACP client subsystem wiki documents the live view-controller → model → panel rendering stack only
- [x] Domain context wiki attributes transcript ownership to `TranscriptViewController`, not the deleted HTML appender
- [x] Obsolete `committedBodyHtml` and `streamingPlainText` descriptions are removed or corrected to match live block state
- [x] Wiki lint passes when `docs/wiki/path-map.json` applies
