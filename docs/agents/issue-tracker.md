# Issue tracker: Repo PRDs (`docs/prd/`)

Planning artifacts for this repo live as markdown PRDs under `docs/prd/`. Implementation slices may be tracked as issues in your external tracker or as follow-on docs; skills that need a "ticket" should prefer the PRD path unless the human points elsewhere.

## Conventions

- **Create a PRD**: `/to-prd` writes `docs/prd/{slug}.md` using the process PRD template.
- **Read a PRD**: open the file under `docs/prd/`; treat content as **historical for behavior claims** until verified against code, tests, and `CONTEXT.md`.
- **Split work**: `/to-issues` produces implementation slices; link each slice back to the PRD path in its Context section.
- **Status**: record planning status in the PRD (draft / in review / accepted / superseded) in a `## Status` section near the top.

## When a skill says "publish to the issue tracker"

Create or update a file under `docs/prd/` (not GitHub/GitLab unless the human explicitly redirects).

## When a skill says "fetch the relevant ticket"

Read the referenced `docs/prd/{slug}.md` path. If the human passes an external issue URL, treat it as supplementary historical context only.

## Related configuration

See `docs/agents/domain.md` for `CONTEXT.md` and ADR layout. See `.agentic-config/USAGE.md` for slash commands.
