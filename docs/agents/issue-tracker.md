# Issue tracker: Repo specs (`docs/features/`)

Planning artifacts for this repo live as markdown PRDs under `docs/features/<feature_name>/`. Implementation slices live under `docs/issues/<feature_name>/`. Skills that need a "ticket" should prefer these paths unless the human points elsewhere.

## Conventions

- **Create a PRD**: `/to-spec` writes `docs/features/<feature_name>/prd.md` using the process PRD template.
- **Read a PRD**: open the file under `docs/features/<feature_name>/`; treat content as **historical for behavior claims** until verified against code, tests, and `CONTEXT.md`.
- **Split work**: `/to-tickets` produces implementation slices as numbered files co-located with the PRD: `docs/features/<feature_name>/<NN>-<slice-slug>.md`; link each slice back to the PRD in its **Parent** section.
- **Status**: record planning status in the PRD (draft / in review / accepted / superseded) in a `## Status` section near the top.
- **Master list**: keep `docs/features/FEATURES.md` in sync with every PRD — it is the single source of truth for the feature portfolio.
  - Sections: **Summary**, **Active (implementation order)** (with per-feature ticket tables), **Implemented**, and optional **Architecture conflicts** when open work overlaps.
  - Top of **Active** = next to ship. Per-ticket `**Status:**` in slice files is authoritative.
  - **Ordering**: agents determine and maintain implementation order from PRDs, ticket blocking edges, ADRs, and cross-feature dependencies. Do not ask the human to set order; re-order **Active** when adding or when dependencies change.
  - `/to-spec`: add the feature to **Active** (status = `draft`, rationale, PRD link) at the correct priority position; refresh **Summary**.
  - `/to-tickets`: if the feature has no **Active** entry yet, add it (status from the PRD's `## Status`) with a ticket table (links + blocked-by); re-order **Active** if ticket edges reveal a better sequence; refresh **Summary**; add **Architecture conflicts** when new work overlaps open features.
  - `/tdd` (implement): when slices are all done, mark the PRD `## Status` as `implemented`, move the feature from **Active** to **Implemented** (most recently completed first), refresh **Summary**, and clear resolved conflict notes.
  - When the PRD status changes for any other reason: update the feature's status in **Active** (or move to **Implemented** when status becomes `implemented`).
  - When a PRD is deleted or superseded: remove the feature from whichever section it is in.
  - Append a **Documentation** bullet to `CHANGELOG.md` when `FEATURES.md` changes materially (not for a single ticket status flip inside an otherwise unchanged feature).
  - Status vocabulary: `draft`, `accepted`, `ready-for-agent`, `implemented`, `superseded`.

## When a skill says "publish to the issue tracker"

Create or update a file under `docs/features/<feature_name>/` (not GitHub/GitLab unless the human explicitly redirects).

## When a skill says "fetch the relevant ticket"

Read the referenced `docs/features/<feature_name>/prd.md` path. For implementation slices, read `docs/features/<feature_name>/<NN>-<slice-slug>.md`. If the human passes an external issue URL, treat it as supplementary historical context only.

## Related configuration

See `docs/agents/domain.md` for `CONTEXT.md` and ADR layout. See `.agentic-config/USAGE.md` for slash commands.
