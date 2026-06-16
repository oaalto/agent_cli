# Agent CLI plugin — Context

One-paragraph purpose

This repository implements an IntelliJ plugin ("Agent CLI") that helps developers launch and manage external agent CLIs, run isolated worktree sessions, and coordinate agent-driven planning (PRDs) and diagnostics. The plugin constructs and runs agent commands (including WSL/wrapped-shell cases), manages per-session worktrees, and integrates with repo-local planning artifacts under `docs/prd/`.

## Language (canonical)

**Agent (plugin)**:
The IntelliJ plugin code in this repository that orchestrates agent runs, builds commands, and provides UI integrations. Avoid using "agent" alone when ambiguous.

**Agent CLI**:
An external command-line program or binary the plugin may invoke to run an assistant/agent. Distinguish from the plugin when discussing runtime behavior.

**Worktree**:
An isolated project/session created or used by agent runs to keep state and side-effects separate from the main project workspace.

**Command Builder**:
Code that constructs the final shell command used to launch an agent CLI for a given environment (WSL, wrapped shell, etc.).

**PRD (docs/prd/)**:
Repository-local planning artifacts used as the canonical place for agent-driven planning and issue slicing.

## Relationships

- Each `Worktree` is owned by a single agent run and may be mapped to a specific filesystem layout (see `AgentWorktreePathMapper`).
- `PRD` documents are historical sources for planning; treat code/tests as the source of truth for implemented behavior.

## Example dialogue

> **Dev:** "Which binary will the plugin run on Windows when WSL is enabled?"
> **Domain expert:** "The plugin constructs a wrapped shell command and invokes the WSL-installed agent CLI; see `AgentWorktreePathMapper` and the command-builder tests for examples."

## Flagged ambiguities

- The term "agent" has historically been used to mean both the plugin and the external CLI. Use `Agent (plugin)` vs `Agent CLI` to disambiguate in docs and PRDs.

## To Complete

Agent instruction: When this section lists items, offer the user LLM-assisted follow-up to resolve them. Do not invent definitions silently.

- Confirm `made by:` name for changelog attribution (inferred from git config as `Olli Aalto`; confirm or provide alternate name).
- Confirm whether wiki lint should be wired via Node (`scripts/wiki-lint.mjs`) or a Gradle/shell port.
- Confirm preferred build command to surface in `docs/agent-commands.md` (recommended: `./gradlew buildPlugin` for CI, expose `./gradlew build` for local development).
