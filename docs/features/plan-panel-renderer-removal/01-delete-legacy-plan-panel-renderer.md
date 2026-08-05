# 01 — Delete legacy plan panel renderer

**Parent:** `prd.md`

**What to build:** Remove the orphaned HTML `PlanPanelRenderer` and its test suite so plan blocks in the **Transcript** have a single live rendering path through `PlanRowAdapter` → `PlanPanel` (Swing). Users see identical plan rows — status icons, priority borders, dismissed state, and variants — with no UX change. Developers and agents searching for plan rendering land on one implementation aligned with ADR 0005.

**Blocked by:** None — can start immediately

**Status:** done

- [x] `PlanPanelRenderer` and `PlanPanelRendererTest` are deleted; no production or test code imports the removed types
- [x] `rg PlanPanelRenderer` finds only `CHANGELOG.md` and other historical references — live wiki and concept docs no longer describe the deleted renderer as current
- [x] `PlanPanelTest`, `PlanRowAdapterTest`, and transcript factory integration tests remain the behavioural guard; `./gradlew qualityGate` passes
- [x] `CHANGELOG.md` records the deletion under today's date
