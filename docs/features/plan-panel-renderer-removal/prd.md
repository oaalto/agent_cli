## Status

implemented

## Problem Statement

The **Transcript** row adapter registry (ADR 0005) routes `PlanBlock` rendering through `PlanRowAdapter` → `PlanPanel` (Swing). A parallel module — `PlanPanelRenderer` — still exists: an HTML string renderer (~276 lines) with its own test suite. It has **zero production imports**.

The migration in `acp-transcript-block-view-decomposition` completed the adapter path but never deleted the legacy renderer. Maintainers now face:

- Duplicate presentation logic (HTML vs Swing) for plan status icons, priority borders, dismissed state, and variants.
- Orphaned tests (`PlanPanelRendererTest`, 12 HTML assertion cases) that guard dead code.
- Navigation friction: grep hits two plan renderers; AI explorers cannot tell which is live.

**Deletion test:** Removing `PlanPanelRenderer` and its tests eliminates ~276 LOC + test maintenance with no runtime behaviour change. Complexity does not reappear elsewhere — `PlanRowAdapter` already owns production rendering.

## Solution

Delete `PlanPanelRenderer` and `PlanPanelRendererTest`. Confirm no references remain. Rely exclusively on `PlanRowAdapter` + `PlanPanel` + `PlanPanelTest` / `PlanRowAdapterTest` for plan row behaviour.

No new module — this is a **deletion** deepening: remove shallow duplicate, keep the deep adapter seam.

## User Stories

1. As a user viewing plan blocks in the **Transcript**, I want plan rows rendered identically to today, so that deleting dead code does not change UX.
2. As a developer changing plan row styling, I want one implementation (`PlanPanel` via `PlanRowAdapter`), so that I do not update HTML and Swing in parallel.
3. As a developer searching for plan rendering code, I want a single entry point in the row adapter registry, so that navigation matches ADR 0005.
4. As a developer running CI, I want tests to cover only the live Swing path, so that false confidence from orphaned HTML tests is removed.
5. As an AI agent exploring the codebase, I want no ambiguous duplicate plan renderers, so that architecture reviews do not re-surface this candidate.
6. As a developer reviewing a plan-row PR, I want changes confined to `PlanRowAdapter` and `PlanPanel`, so that review locality matches the adapter registry design.
7. As a user with dismissed or priority-variant plan blocks, I want the same visual treatment after cleanup, so that `PlanPanel` remains the sole source of truth.
8. As a developer, I want CHANGELOG to note the deletion, so that release notes reflect reduced surface area.

## Implementation Decisions

### Modules to delete

- `PlanPanelRenderer` (HTML string renderer)
- `PlanPanelRendererTest`

### Modules to keep (unchanged)

- `PlanRowAdapter` — production dispatch for `PlanBlock`
- `PlanPanel` — Swing widget assembly
- `PlanPanelTest`, `PlanRowAdapterTest`, `TranscriptBlockViewFactoryTest` — live test coverage

### Verification steps

1. `rg PlanPanelRenderer` — only CHANGELOG/historical docs may reference; no production imports.
2. `./gradlew qualityGate` — all tests pass after deletion.
3. Optional: run `PlanRowAdapterTest` and `PlanPanelTest` explicitly if doing incremental commit.

### ADR alignment

- **ADR 0005:** Completes the adapter registry migration by removing the pre-decomposition monolith remnant.

## Testing Decisions

### What to test

- No new tests required — deletion only.
- Existing `PlanPanelTest`, `PlanRowAdapterTest`, and factory integration tests remain the behavioural guard.

### Modules to test

- None new. Confirm `PlanPanelRendererTest` removal does not reduce meaningful coverage.

### Prior art

- `PlanPanelTest` — live Swing behaviour.
- `PlanRowAdapterTest` — adapter create/update/dispose contract.

### Verification

`./gradlew qualityGate` passes; grep shows no production references to deleted types.

## Out of Scope

- Changes to `PlanPanel` visual design or plan block data model.
- Transcript row adapter registry structure changes.
- **Session transcript file** serialization of plans (still omitted per ADR 0004).
- HTML export or clipboard features that might have been a future use for `PlanPanelRenderer`.

## Further Notes

- **Recommendation strength:** Worth exploring as a quick win — zero user-visible change, immediate locality gain.
- **Effort:** Small (delete + grep + quality gate).
- **Pairs well with:** `worktree-orchestrator-production-wiring` — independent, can ship in same release train.
