# Graphify Consultation

## Pre-task consultation

Apply before reading, searching, or editing task-relevant paths — not only when making file changes.

1. **Verify graph state:** `graphify-out/` is gitignored. Read/Glob/Grep may report it missing when the graph exists. Check with shell: `test -f graphify-out/graph.json` (PowerShell: `Test-Path graphify-out/graph.json`). Optionally read `graphify-out/.graphify_semantic_marker` or `graphify-out/GRAPH_REPORT.md` when visible. Compare build commit in `GRAPH_REPORT.md` to `git rev-parse HEAD` when checking freshness. Pre-commit runs `graphify update .` (`scripts/pre-commit`); for a first-time graph or manual refresh, run `graphify .` or `graphify update .` from the repository root.
2. **Required for structural questions:** When the task needs cross-file or cross-slice topology (call/import chains, caller maps, dependency paths, impact analysis, which files connect A to B), run `graphify query`, `graphify path`, `graphify explain`, or graphify MCP via **shell or MCP** **before** opening implementation files or broad `rg`/search. Do not wait for the user to say "use graphify". Do not conclude the graph is absent from file-tool misses alone.
3. **Stale or missing graph:** When shell confirms absence, propose `graphify update .` from the repository root before deep structural work; do not guess topology from memory.
4. **Narrative overview questions:** Skip graphify when the task is only a subsystem or architecture summary (what X owns); use wiki and ADRs (see **Narrative overview questions** in the host file preamble).

## Structural question triggers

Treat these as graphify-first when shell confirms `graphify-out/graph.json` exists: call chain, import path, what calls, what connects, cross-slice, public facade, shortest path, who depends on, what breaks if, caller, callee, dependency path, impact analysis.

## Source hierarchy

- Treat AST-extracted graph edges (`EXTRACTED` tier) as structural fact.
- Treat inferred semantic links (`INFERRED` tier) as hypotheses until verified against code, tests, or ADRs.

## Operations

Pair with the upstream graphify skill at `.pi/agent/skills/graphify/SKILL.md` (installed by `install.sh`) for ingest, MCP registration, `.graphifyignore` tailoring, and automation proposals.

## Repository layout

- Primary sources: `src/main/kotlin/com/oaalto/agent/` (Kotlin IntelliJ plugin), `src/test/kotlin/`.
- Ignore build outputs and IDE caches via `.graphifyignore` (`.gradle/`, `build/`, `.idea/`, `.intellijPlatform/`).
- Domain and planning docs: `CONTEXT.md`, `docs/wiki/`, `docs/prds/`, `docs/adr/`.
- Extraction backend is operator-owned — configure per `graphify.env.example` before semantic indexing on doc-heavy paths.
