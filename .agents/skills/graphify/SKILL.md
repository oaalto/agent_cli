---
name: graphify
description: Structural topology and cross-slice exploration — call chains, import paths, dependency analysis. Load before deep code search when graphify-out/ exists; use wiki skill for narrative overview only.
---

# Graphify (consultation overlay)

Repo-local overlay for structural code-graph consultation. **CLI, MCP, ingest, and automation mechanics:** read the upstream graphify skill at `.pi/agent/skills/graphify/SKILL.md` (installed by `install.sh`).

## Pre-task consultation

Apply before reading, searching, or editing task-relevant paths — not only when making file changes.

1. **Verify graph state:** `graphify-out/` is gitignored (per `.gitignore`). Check with shell: `test -f graphify-out/graph.json`. This repo has `graphify-out/graph.json` and `graphify-out/GRAPH_REPORT.md` (AST-only; 3204 nodes, 4644 edges as of last index).
2. **Required for structural questions:** When the task needs cross-file or cross-slice topology (call/import chains, caller maps, dependency paths, impact analysis, which files connect A to B), run `graphify query`, `graphify path`, `graphify explain`, or graphify MCP via **shell or MCP** **before** opening implementation files or broad search. Do not wait for the user to say "use graphify". Do not conclude the graph is absent from file-tool misses alone.
3. **Stale or missing graph:** Compare build commit in `GRAPH_REPORT.md` to `git rev-parse HEAD`. When stale or absent, run `graphify update .` from the repository root before deep structural work. **This repo uses AST-only indexing** (free tier — no LLM extraction backend configured). Do not propose semantic/doc extraction unless the operator opts in; see `graphify.env.example`.
4. **Narrative overview questions:** Skip graphify when the task is only a subsystem or architecture summary (what X owns); load the `wiki` skill and ADRs (see **Narrative overview questions** in `.agents/skills/repo-navigation/SKILL.md`).
5. **Primary slices:** `pty/`, `acp/`, `worktree/`, `settings/` under `src/main/kotlin/com/oaalto/agent/` — use graphify for cross-slice paths, not directory sweeps.

## Structural question triggers

Treat these as graphify-first when shell confirms `graphify-out/graph.json` exists: call chain, import path, what calls, what connects, cross-slice, public facade, shortest path, who depends on, what breaks if, caller, callee, dependency path, impact analysis.

## Source hierarchy

- Treat AST-extracted graph edges (`EXTRACTED` tier) as structural fact.
- Treat inferred semantic links (`INFERRED` tier) as hypotheses until verified against code, tests, or ADRs.

## Operations

Pair with the upstream graphify skill for ingest, MCP registration, `.graphifyignore` tailoring, and automation proposals.
