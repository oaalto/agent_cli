# Domain context

Agent instruction: use this file for **domain vocabulary** before naming concepts. For behavior, call chains, and implementation detail, read the wiki and source — not this file.

## Read next

| Topic | Where |
| --- | --- |
| Wiki index | [`docs/wiki/index.md`](docs/wiki/index.md) |
| Architecture & ADRs | [`docs/wiki/subsystems/architecture.md`](docs/wiki/subsystems/architecture.md), [`docs/adr/`](docs/adr/) |
| ACP transcript model | [`docs/wiki/concepts/context.md`](docs/wiki/concepts/context.md) |
| Worktrees | [`docs/wiki/subsystems/worktree.md`](docs/wiki/subsystems/worktree.md) |
| Quality gates & CI | [`docs/wiki/workflows/quality-gate.md`](docs/wiki/workflows/quality-gate.md), [`docs/agent-commands.md`](docs/agent-commands.md) |
| Planning artifacts | [`docs/features/`](docs/features/) |
| Plugin source | [`src/main/kotlin/com/oaalto/agent/`](src/main/kotlin/com/oaalto/agent/) |
| Structural topology | `graphify query` / `graphify path` (see `.agents/rules/graphify-consultation.md`) |

## Glossary

### Agent CLI plugin

IntelliJ Platform plugin that runs external agent CLIs in a dedicated editor tab with embedded terminal output. Product home is in-tab UX — not JetBrains AI Chat. See [ADR 0001](docs/adr/0001-custom-acp-client-in-plugin.md).

### ACP (Agent Client Protocol)

JSON-RPC protocol between an ACP **client** (this plugin) and an ACP **agent** subprocess. The plugin implements the client in-process. See [ADR 0001](docs/adr/0001-custom-acp-client-in-plugin.md), [ADR 0002](docs/adr/0002-kotlin-acp-sdk.md).

### Launch Mode

Per agent configuration. **PTY Passthrough** (default): native CLI TUI in the embedded terminal. **ACP Client**: ACP agent over stdio; split UI (transcript, prompt input, shell). See [ADR 0001](docs/adr/0001-custom-acp-client-in-plugin.md).

### Agent configuration

Named IDE-wide definition (binary path, launch mode, MCP toggles, etc.) stored in `agentSettings.xml` via `AgentSettingsState`. Not VCS-scoped.

### Default agent configuration

Application-scoped default configuration ID (`defaultConfigurationId` in `agentSettings.xml`). Set from Settings; not changed by per-project toolbar selection. See [ADR 0003](docs/adr/0003-per-project-agent-selection.md).

### Selected agent

Per-`Project` runtime choice of which named configuration to run. Workspace-local (not VCS). Resolved via `AgentConfigurationSelector`. See [ADR 0003](docs/adr/0003-per-project-agent-selection.md).

### AgentConfigurationSelector

Single facade for runtime selection: `getSelectedConfiguration(project)` / `setSelectedConfiguration(project, id)`. All toolbar, Run Agent, and Open Editor call sites use this — not a global selected ID on `AgentSettingsState`.

### Worktree

Git worktree used to isolate an agent session filesystem. Stores `acpSessionId` for ACP resume; PTY mode uses CLI flag-based resume. Slice: `agent/worktree/`. See [worktree subsystem](docs/wiki/subsystems/worktree.md).

### `agentSettings.xml` / `acp.json`

`agentSettings.xml` is the configuration source of truth. `acp.json` is import/export only.

### Vertical slices

Code under `com.oaalto.agent` is organized by concern:

- `pty/` — PTY Passthrough editor and launch path
- `acp/` — ACP client, session loop, transcript/shell UI
- `worktree/` — Git worktree orchestration (mode-agnostic)
- `settings/` — Shared configuration and launch mode

See [ADR 0001](docs/adr/0001-custom-acp-client-in-plugin.md).

### Transcript

ACP mode HTML rendering of `SessionUpdate` events in a `JEditorPane`. Entry point: `AcpAgentEditor`; implementation detail in [`docs/wiki/concepts/context.md`](docs/wiki/concepts/context.md) and `agent/acp/`. Distinct from **Session transcript file**.

### Session transcript file

Workspace-local plain-text record of an ACP session conversation, keyed by `acpSessionId`, written incrementally while the editor is open. Restored as plain lines when the session is resumed. ACP Client mode only. See [ADR 0004](docs/adr/0004-session-observability.md).

### Session diagnostics

Developer-facing observability in IDEA log (`idea.log`): tiered `warn`/`info`/`debug` output with session context and correlation tokens linking transcript errors to log lines. Distinct from **Transcript** and **Session transcript file**. See [ADR 0004](docs/adr/0004-session-observability.md).

### PRD

Product requirements document under `docs/features/` (not `docs/prd/`).
