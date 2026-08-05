## Status

implemented

**Triage:** `ready-for-agent`

## Problem Statement

The ACP transcript stack has ~25+ `Transcript*` types at the `acp/` package root after `transcript-pipeline-consolidation`. Model (`StructuredUpdate`, `TranscriptBlock`, `TranscriptModel`), render (markdown, HTML builders, tool renderers), and view (`TranscriptPanel`, block view factory, label binder) modules are **flat siblings** — understanding one vertical slice requires scanning the whole package. AI navigators and new contributors experience poor **locality**: the **interface** for “where does rendering live?” has no package-level answer.

The parent consolidation PRD scoped Phase 3 package moves as optional; implementation order deferred structural reorganization. With content-renderer unification and block-view decomposition planned, repackaging now prevents new files from landing at the root and re-widening the shallow surface.

## Solution

Repackage transcript modules into three subpackages under `acp/transcript/`:

| Package | Contents |
| --- | --- |
| `transcript/model/` | `StructuredUpdate`, `TranscriptBlock`, `TranscriptModel`, `TranscriptBodyPart`, `TranscriptEventIngestion`, `TranscriptFinalizePolicy` |
| `transcript/render/` | Content renderer, markdown renderer, HTML/table builders, tool call renderers, fence normalizer, fence language resolver, `TranscriptRenderer` |
| `transcript/view/` | `TranscriptViewController`, `TranscriptPanel`, block view factory, `TranscriptFooter`, code block view factory, column sizing, collapsible tool panel |
| `transcript/view/rows/` | Row adapters (`*RowAdapter.kt`), `TranscriptBodyPartWidgetMapper`, `TranscriptStreamingCursor` |
| `transcript/theme/` | `TranscriptColorProvider`, `TranscriptPalette`, `TranscriptBadgeStyle` (shared by plan + render + view) |

`plan/` remains `acp/plan/` — plan blocks integrate at view adapter seam only. Session transcript file I/O (`SessionTranscriptCoordinator`, `TranscriptFileStore`, `TranscriptTextSerializer`) stays at `acp/` root.

**No behaviour changes** — imports and file moves only, plus wiki/path-map updates. Public API to other slices (`AcpAgentEditor`, session operations) unchanged: they import from `acp` or new package paths via IDE refactor.

## User Stories

1. As a developer adding a render helper, I want a `transcript/render/` home, so that I do not add another root-level `Transcript*.kt`.
2. As an AI agent, I want package names to encode layer (model/render/view), so that exploration routes to the right depth on first try.
3. As a developer fixing a view bug, I want grep scoped to `transcript/view/`, so that search results exclude ingestion mappers.
4. As a maintainer updating wiki path-map, I want canonical package diagram in architecture page, so that docs match disk layout.
5. As a developer reviewing content-renderer PR, I want render package to contain all markdown→body-part code, so that PR scope is directory-bounded.
6. As a developer on session operations, I want stable import paths documented in migration note, so that cross-slice imports update mechanically.
7. As a contributor running graphify, I want dependency edges model→render→view visible, so that forbidden reverse dependencies are obvious.
8. As a user, I want zero functional change from repackaging, so that release notes list structural change only.
9. As a developer writing detekt architecture rules, I want package boundaries enforceable (view must not import swing from model), so that layering violations fail CI.
10. As a reviewer, I want this PR to be moves-only with no logic diffs, so that review is import/path verification.
11. As a developer coordinating block adapter extraction, I want adapters under `transcript/view/rows/`, so that factory decomposition has a directory.
12. As a product owner, I want this after functional refactors stabilize file names, so that we do not move files twice.
13. As a wiki maintainer, I want `docs/wiki/subsystems/acp-client.md` package tree updated, so that stale flat-package description is removed.
14. As a developer on test sources, I want test packages mirror main (`transcript/model`, etc.), so that test discovery matches production layout.
15. As a CI maintainer, I want `./gradlew qualityGate` to catch broken imports, so that moves are complete in one commit.
16. As an implementer, I want IntelliJ refactor → move safe across modules, so that manual path errors are minimized.
17. As a developer reading CONTEXT.md glossary, I want transcript stack pointer to mention three-package layout, so that domain vocabulary links to structure.
18. As a future contributor, I want dependency rule: model has no Swing imports; render has no Swing; view owns Swing, so that indiscretions are structurally discouraged.

## Implementation Decisions

### Ownership

- **Vertical slice:** ACP `acp/` package — file moves and import updates across main + test sources; wiki path-map.

### Move timing

- **Blocked by (soft):** `acp-transcript-content-renderer` (done) and `acp-transcript-block-view-decomposition` — perform package restructure after block-view adapters land in `acp/` to avoid move-then-rename churn. Adapter files (`*RowAdapter.kt`) and `TranscriptBodyPartWidgetMapper` move to `transcript/view/rows/` in this PR.

### Package dependency rules

```
transcript/model (except TranscriptEventIngestion)  →  no transcript/view, no javax.swing
TranscriptEventIngestion                            →  transcript/render only (tool bodyParts)
transcript/render                                 →  model + theme; no view; no javax.swing
transcript/view                                   →  model + render + theme; Swing allowed
acp/plan                                          →  model + theme/render; not view
```

Architecture grep test in same PR (mirror `FinalizeAgentStreamConstructionTest` / `AgentFenceNormalizationConstructionTest` style). Optional detekt rule out of scope.

### Files staying at `acp/` root

- `AcpAgentEditor`, session controller, prompt executor, layout — orchestration, not transcript internals.
- `SessionTranscriptCoordinator`, `TranscriptFileStore`, `DebouncedTranscriptSnapshotWriter`, `TranscriptTextSerializer`, `SessionDiagnosticsCollector` — session transcript file domain (distinct from live transcript UI stack).

### Seam for testing

**No new test seam** — existing tests pass with updated imports. Add package dependency grep test asserting dependency matrix (same PR).

### Grill accept (2026-08-05)

Grill-with-docs-batch decisions recorded in wiki and `CONTEXT.md`: `transcript/theme/` sibling package; `TranscriptEventIngestion` alone may import render; row adapters + `TranscriptStreamingCursor` in `view/rows/` (no `TranscriptBlockLabelBinder` — removed during fence normalization); no new ADR.

### Wiki / docs

- Done (grill accept 2026-08-05): `docs/wiki/subsystems/acp-client.md`, `docs/wiki/concepts/context.md`, `CONTEXT.md`.
- Run wiki lint when path-map references change after code moves land.

### ADR alignment

- Internal structure only — ADR 0001/0002 unchanged. No new ADR (reversible navigational change).

### Migration

- Single commit preferred: `git mv` sources + tests, import fix, architecture grep test, wiki refresh after moves.
- CHANGELOG under `### Changed` — structural repackage (separate from grill-accept documentation bullet).

## Testing Decisions

### What makes a good test

- Full `./gradlew qualityGate` — compile proves imports.
- Package dependency grep test in same PR (mirror `FinalizeAgentStreamConstructionTest` style); `javax.swing` forbidden in model/render.

### Modules to test

- No new behaviour tests required.
- Relocate test files with sources; git history follows via `git mv`.

### Prior art

- `transcript-pipeline-consolidation` Phase 3 description in parent PRD.
- Other package moves in repo history (if any) — follow same IntelliJ refactor workflow.

### Verification

- `./gradlew qualityGate` passes.
- Wiki lint clean if path-map touched.

## Out of Scope

- Renaming types (only packages/directories).
- Splitting or merging files beyond what sibling PRDs already did.
- Moving `plan/` package inside `transcript/`.
- PTY transcript code.
- Publishing separate Gradle modules — single module, logical packages only.

## Further Notes

- Architecture review strength: **Speculative** — high clarity, zero user value alone; ship after functional deepening PRDs.
- **Deletion test:** repackaging fails the test if it does not improve locality — value is navigational, not runtime.
- Insert last in implementation order in FEATURES.md.
