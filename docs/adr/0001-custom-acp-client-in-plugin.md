# ADR 0001: Custom ACP client in the Agent CLI plugin

- **Status:** Accepted
- **Date:** 2026-06-16

## Context

IntelliJ Platform 2025.3+ ships an ACP (Agent Client Protocol) client inside **JetBrains AI Chat**. That client provides structured chat UI, permissions, MCP bridging, and session management for ACP-compliant agents (Cursor, Codex, goose, etc.).

The Agent CLI plugin exists so developers can run agent workflows **inside a dedicated editor tab with terminal output**, not in the AI Chat tool window. Delegating agent sessions to JetBrains AI Chat would contradict the primary reason for this plugin.

ACP still offers value without AI Chat:

- Standardized session APIs (`session/new`, `session/load`, `listSessions`) instead of per-CLI resume flags
- IDE-integrated filesystem and terminal provisioning via the client role
- Permission and MCP hooks without maintaining bespoke CLI integrations per agent

The plugin already embeds `ShellTerminalWidget` in `AgentFileEditor` and orchestrates worktrees, WSL launch, and command building. Version 3.0 must adopt ACP without surrendering the in-tab terminal experience.

## Decision

The Agent CLI plugin will **implement its own ACP client** in-process, hosted by `AgentFileEditor`, using the Kotlin ACP SDK. It will **not** route agent sessions through JetBrains AI Chat.

Hybrid **Launch Mode** per agent configuration:

| Mode | Behavior |
|------|----------|
| **PTY Passthrough** (default) | Spawn Agent CLI in embedded terminal; native TUI unchanged |
| **ACP Client** | Spawn ACP agent subprocess (stdio JSON-RPC); plugin renders split UI (Transcript pane, Prompt input, Shell pane) |

Additional 3.0 constraints tied to this decision:

- Plugin settings (`agentSettings.xml`) remain the configuration source of truth; `acp.json` is import/export only
- Worktrees store `acpSessionId` for ACP resume; PTY mode keeps CLI flag-based resume
- Filesystem client ops scoped to project/worktree root
- MCP toggles (IntelliJ + user) per config, default off; IntelliJ MCP requires optional AI Assistant plugin dependency
- Koog / custom agent implementation deferred to 4.0

Code is organized in **vertical slices** under `com.oaalto.agent`:

```
agent/
  pty/        — PTY Passthrough editor and launch path
  acp/        — ACP client, session loop, transcript/shell UI
  worktree/   — Git worktree orchestration (mode-agnostic)
  settings/   — Shared configuration and launch mode
```

## Alternatives considered

1. **Delegate to JetBrains AI Chat** — Rejected. Removes the in-editor terminal UX that defines the product.
2. **Config-only plugin (`acp.json` sync)** — Rejected. Does not replace custom resume hacks or integrate worktrees with ACP sessions; users still use AI Chat for runs.
3. **PTY-only forever (no ACP)** — Rejected. Per-agent resume flag mapping and Cursor probe logic do not scale as agents adopt ACP.
4. **Custom Koog-based ACP agent (4.0)** — Postponed. Building an agent is separate from consuming ACP as a client.

## Consequences

### Positive

- Full control over editor-tab UX (terminal aesthetic, no chat bubbles)
- Worktree + ACP session binding is a differentiated feature ACP/AI Chat does not provide out of the box
- One protocol for ACP-native agents; PTY path preserved for Pi and other TUI CLIs
- Slices keep PTY and ACP runtimes isolated and testable

### Negative

- Plugin maintains ACP client responsibilities: `fs/*`, `terminal/*`, permissions, auth, MCP bridging
- Larger 3.0 surface than delegating to JetBrains AI Chat
- IntelliJ MCP integration depends on optional AI Assistant plugin APIs

### Neutral

- JetBrains AI Assistant may be installed alongside Agent CLI without conflict; the plugins serve different UI surfaces
- `AgentCommandBuilder` WSL/node-wrapper logic is reused for ACP agent process launch commands
