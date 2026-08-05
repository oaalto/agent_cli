# 06 — Cross-slice import sweep and root boundary check

**Parent:** `prd.md`

**What to build:** Complete any remaining import-path updates for cross-slice consumers (`plan/`, auth flow, transport, worktree resume, settings UI) and verify session transcript file I/O types (`SessionTranscriptCoordinator`, file store, debounced snapshot writer, text serializer, diagnostics collector) remain at the `acp/` orchestration root — not under `transcript/`. Confirm the repackage is moves-only: no logic diffs outside package/import declarations.

**Blocked by:** 05 — Transcript view and row adapters package move

**Status:** done

- [x] `plan/` imports model, theme, and render where needed — not view
- [x] Session transcript file persistence types stay at orchestration root and still serialize `TranscriptBlock` snapshots correctly
- [x] Review diff shows no behavioural changes; only structural moves and import fixes
- [x] `./gradlew qualityGate` passes
