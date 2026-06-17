# Agent CLI plugin — Context

One-paragraph purpose

This repository implements an IntelliJ plugin ("Agent CLI") that helps developers launch and manage external agent CLIs, run isolated worktree sessions, and coordinate agent-driven planning (PRDs) and diagnostics. The plugin constructs and runs agent commands (including WSL/wrapped-shell cases), manages per-session worktrees, and integrates with repo-local planning artifacts under `docs/prd/`.

## Language (canonical)

**Agent (plugin)**:
The IntelliJ plugin code in this repository that orchestrates agent runs, builds commands, and provides UI integrations. Avoid using "agent" alone when ambiguous.

**Agent CLI**:
An external command-line program or binary the plugin may invoke to run an assistant/agent. Distinguish from the plugin when discussing runtime behavior.

**Worktree**:
An isolated project/session created or used by agent runs to keep state and side-effects separate from the main project workspace. In ACP Client launch mode, a Worktree may store a bound **ACP session ID** so reopen-with-resume can call `session/load`; if the ID is missing, the plugin falls back to the agent's `listSessions` picker. PTY Passthrough mode keeps CLI flag-based resume.

**Command Builder**:
Code that constructs the final shell command used to launch an agent CLI for a given environment (WSL, wrapped shell, etc.).

**Launch Mode**:
How the Agent (plugin) connects to an Agent CLI for a run. Two modes are planned for 3.0: **PTY Passthrough** (spawn the CLI's native TUI in an embedded terminal) and **ACP Client** (plugin speaks ACP over stdio and renders agent I/O in the editor tab). Hybrid support means each agent configuration picks one mode via an explicit per-configuration setting (default: PTY Passthrough).

**PTY Passthrough**:
Launch Mode where the plugin spawns the Agent CLI binary directly in an IntelliJ `ShellTerminalWidget` and the user interacts with the CLI's native terminal UI. Worktree resume uses CLI flag injection (for example `--continue`); Cursor empty-chat probing applies here only.

**ACP Client (plugin)**:
Launch Mode where the plugin implements the ACP client role (not JetBrains AI Chat), spawns an ACP-compliant agent subprocess, and renders agent output in the `Agent` editor tab using a split layout: **Transcript pane** (agent stream above) and **Shell pane** (embedded PTY below for interactive terminal I/O). Agent authentication follows the advertised ACP auth method: Terminal Auth in the Shell pane; Agent Auth (API key/OAuth) inline in the Transcript pane.

**Transcript pane**:
The read-only, terminal-styled scrollback area in ACP Client launch mode that renders `session/update` output from the agent (text, tool-call status).

**Prompt input**:
The dedicated input control in ACP Client launch mode (below the Transcript pane, above the Shell pane) where the user types messages sent via `session/prompt`. Separate from the Shell pane, which is only for agent-initiated terminal I/O.

**Permission prompt**:
An inline Transcript pane UI shown when an ACP agent calls `session/request_permission`. The user may choose allow/reject once or always; `allow_always` / `reject_always` choices are remembered per tool type (and optionally per agent configuration).

**MCP exposure**:
Optional capability in ACP Client launch mode to pass MCP servers to the agent. Each ACP-mode agent configuration has separate toggles for IntelliJ MCP and user-configured MCP servers; both default to off. IntelliJ MCP bridging uses optional dependencies on JetBrains AI Assistant (`com.intellij.ml.llm`) and/or the MCP Server plugin (`com.intellij.mcpServer`); the IntelliJ MCP toggle is enabled when either plugin is installed and active. User-configured MCP works independently.

**ACP filesystem scope**:
In ACP Client launch mode, `fs/read_text_file` and `fs/write_text_file` are limited to the project or Worktree root. Reads within scope are auto-allowed; writes require a Permission prompt. IDE read-only zones and ignored paths (for example `.gitignore`) are respected.

**Shell pane**:
The embedded `ShellTerminalWidget` in ACP Client launch mode, used for interactive shell I/O when the agent requests ACP `terminal/create` or runs commands.

**Agent configuration store**:
The plugin's persistent settings (`agentSettings.xml`) are the source of truth for agent launch config **definitions** (binary path, launch mode, MCP toggles, etc.). `acp.json` is optional: configs may be imported from or exported to that format, but there is no automatic sync. Config definitions are IDE-wide (shared across all open projects).

**Selected agent**:
The agent configuration currently chosen for a **Project** — shown in the toolbar selector and used by Run Agent / Open Agent Editor in that project window. Each Project remembers its own selection independently; changing it in one window does not affect another. Changing the toolbar selection does **not** change the **Default agent configuration**. Stored per-project (not in `agentSettings.xml`).

**Default agent configuration**:
The IDE-wide fallback agent configuration marked as default in Settings ("Default" column). Used when a Project has no saved **Selected agent**, or when the saved selection no longer exists. Stored in `agentSettings.xml` alongside config definitions.

**Launch slice**:
Code under `com.oaalto.agent` organized by responsibility: `pty/` (PTY Passthrough), `acp/` (ACP Client), `worktree/` (Git worktree orchestration), `settings/` (shared configuration). See ADR 0001.

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
