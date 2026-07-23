---
name: graphify
description: Structural topology and cross-slice exploration — call chains, import paths, dependency analysis. Load before deep code search when graphify-out/ exists; use wiki skill for narrative overview only.
---

# Graphify (consultation overlay)

Repo-local overlay for structural code-graph consultation. **CLI, MCP, ingest, and automation mechanics:** read the upstream graphify skill installed by `install.sh` (platform path varies by target).

## Pre-task consultation

Apply before reading, searching, or editing task-relevant paths — not only when making file changes.

1. **Verify graph state:** `graphify-out/` is gitignored (per `.gitignore`). Check with shell: `test -f graphify-out/graph.json`. This repo has `graphify-out/graph.json` (~2.5MB, AST-only indexing — no LLM extraction backend configured). No `GRAPH_REPORT.md` exists yet.
2. **Required for structural questions:** When the task needs cross-file or cross-slice topology (call/import chains, caller maps, dependency paths, impact analysis, which files connect A to B), run `graphify query`, `graphify path`, `graphify explain`, or graphify MCP via **shell or MCP** **before** opening implementation files or broad search. Do not wait for the user to say "use graphify". Do not conclude the graph is absent from file-tool misses alone.
3. **Stale or missing graph:** When shell confirms absence, propose `graphify .` from the repository root before deep structural work. For this repo: `graphify .` will produce AST-only indexing (no semantic extraction without an LLM backend — see `graphify.env.example`).
4. **Narrative overview questions:** Skip graphify when the task is only a subsystem or architecture summary (what X owns); load the `wiki` skill and ADRs (see **Narrative overview questions** in `.agents/skills/repo-navigation/SKILL.md`).
5. **Graph freshness:** Compare build commit in `graphify-out/GRAPH_REPORT.md` (when it exists) to `git rev-parse HEAD`. The graph is at `graphify-out/graph.json` relative to repo root.

## Structural question triggers

Treat these as graphify-first when shell confirms `graphify-out/graph.json` exists: call chain, import path, what calls, what connects, cross-slice, public facade, shortest path, who depends on, what breaks if, caller, callee, dependency path, impact analysis.

## Source hierarchy

- Treat AST-extracted graph edges (`EXTRACTED` tier) as structural fact.
- Treat inferred semantic links (`INFERRED` tier) as hypotheses until verified against code, tests, or ADRs.

## Operations

Pair with the upstream graphify skill for ingest, MCP registration, `.graphifyignore` tailoring, and automation proposals.
