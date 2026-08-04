# 03 — Plan row adapter

**Parent:** `prd.md`

**What to build:** Extract `PlanRowAdapter` as a thin wrapper around the existing plan panel. Plan checklists bind by plan id, show status icons, and update in place exactly as today — but creation and update dispatch through the coordinator instead of inline factory branches.

**Blocked by:** 01 — Row adapter scaffold and simple text extraction

**Status:** done

- [x] `PlanRowAdapter` `matches` on plan panel + `PlanBlock`; `create`/`update` delegate to existing panel `bind`; `dispose` is a no-op (preserve current factory behaviour for plan rows)
- [x] Coordinator dispatches plan blocks to `PlanRowAdapter`; inline plan handling removed from factory
- [x] Plan layout constants and insets remain colocated with the adapter (not prose or tool rows)
- [x] Headless EDT tests mount plan adapter output: entry count, status icons, in-place update by plan id
- [x] `./gradlew qualityGate` passes; zero visible plan-row UX change
