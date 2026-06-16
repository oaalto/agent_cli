# Wiki Consultation

## Pre-task consultation

1. **Tier 1 (always):** Read `docs/wiki/path-map.json` and `docs/wiki/index.md`. List candidate pages from path-map `sources` matching planned edit paths **or** index entries matching domain terms in the task.
2. **Tier 2 (when candidates exist):** Read up to **3** candidate pages (subsystem → concept → workflow priority).
3. **Tier 3 (before implementing from wiki):** Verify claims against code, tests, or ADRs; treat unverified synthesis as hypothesis.
4. **No match:** Proceed; note no wiki coverage.

## Source hierarchy

- Treat code, tests, accepted ADRs, `CONTEXT.md`, current runbooks, and current official external docs as live sources.
- Treat PRDs, issue discussions, PR discussions, and chat history as historical sources unless verified against live sources.

## Write obligations

- Update or propose wiki changes when durable knowledge changes.
- Do not require wiki updates for trivial edits; record a `skip` log entry in `docs/wiki/log.md` when wiki work is intentionally omitted.
- Propose before changing rules, skills, or `docs/wiki/schema.md`.

Wiki completion gates (path-map checks, mechanical lint before commit) are in the `definition-of-done` rule.
