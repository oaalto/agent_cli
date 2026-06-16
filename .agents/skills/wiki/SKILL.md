---
name: wiki
description: Query, ingest, update, and lint the LLM-maintained engineering wiki in docs/wiki/.
---

# Wiki

## Purpose

Maintain the engineering wiki as an agent-owned, human-reviewed memory layer.

## Pre-task consultation

Pre-task wiki consultation is in the `wiki-consultation` rule. Use the operations below for query, ingest, update, and lint.

## Operations

Use one of these operations:

- `/wiki-query`: answer a question using the wiki and verified sources.
- `/wiki-ingest`: process a source into the wiki.
- `/wiki-update`: update existing wiki pages after durable knowledge changes.
- `/wiki-lint`: health-check the wiki.
## Before Commit

Wiki skip-log policy is in the `wiki-consultation` rule. Path-map checks and mechanical lint before commit are in the `definition-of-done` rule. Use the operations in this skill (`/wiki-update`, `/wiki-ingest`, `/wiki-lint`) to satisfy them.

## `/wiki-query`

1. Read `docs/wiki/index.md`.
2. Read relevant wiki pages.
3. Verify critical claims against live sources.
4. Answer with citations to files, URLs, or wiki pages.
5. If the answer contains durable new synthesis, ask whether to file it back into the wiki.

## `/wiki-ingest`

1. Identify source type: live, historical, or external.
2. Read the source.
3. Extract durable facts, historical context, open questions, and synthesis.
4. Update or create relevant pages.
5. Update `docs/wiki/index.md`.
6. Append to `docs/wiki/log.md`.
7. Propose promotion to `CONTEXT.md` or ADRs when appropriate.

## `/wiki-update`

1. Identify which durable knowledge changed.
2. Find affected wiki pages through the index and `path-map.json`.
3. Update pages with evidence status.
4. Preserve historical context instead of overwriting it silently.
5. Update related links.
6. Update index and log.

## `/wiki-lint`

Run mechanical lint first when `scripts/wiki-lint.mjs` exists, then check semantic drift:

- stale claims contradicted by live sources,
- pages without evidence,
- orphan pages,
- missing inbound links,
- duplicate pages for the same concept,
- ADR/wiki contradictions,
- `CONTEXT.md` terms missing from wiki maps,
- wiki concepts that should be promoted to `CONTEXT.md`,
- PRD claims being used as live truth.

Report findings before making broad edits. Fix clear mechanical issues when safe.

## Output

For queries, answer with citations and verification notes.

For ingests and updates, summarize:

- pages created,
- pages updated,
- sources used,
- open questions,
- promotion candidates.

For lint, list findings by severity and recommend fixes.