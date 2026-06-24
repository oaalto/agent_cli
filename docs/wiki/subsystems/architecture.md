---
title: Architecture decisions map
type: subsystem
status: current
updated: 2026-06-24
sources:
  - docs/adr/0001-custom-acp-client-in-plugin.md
  - docs/adr/0002-kotlin-acp-sdk.md
  - docs/adr/0003-per-project-agent-selection.md
---

# Architecture decisions map

## Summary

Accepted ADRs define how the Agent CLI IntelliJ plugin is structured for 3.0: an in-process ACP client (not JetBrains AI Chat), Kotlin ACP SDK transport, vertical slices under `com.oaalto.agent`, and per-project runtime agent selection separate from IDE-wide configuration defaults.

## Verified Facts

### ADR 0001 — Custom ACP client in the plugin

- The plugin implements its own ACP **client** in `AgentFileEditor`; it does **not** delegate sessions to JetBrains AI Chat.
- **Launch Mode** per agent configuration:
  - **PTY Passthrough** (default): native CLI TUI in embedded terminal.
  - **ACP Client**: ACP agent subprocess over stdio JSON-RPC; split UI (transcript, prompt, shell).
- `agentSettings.xml` remains configuration source of truth; `acp.json` is import/export only.
- Worktrees store `acpSessionId` for ACP resume; PTY mode uses CLI flag-based resume.
- Filesystem client ops scoped to project/worktree root; MCP toggles per config, default off.
- Code organized in vertical slices:

```
agent/
  pty/        — PTY Passthrough editor and launch path
  acp/        — ACP client, session loop, transcript/shell UI
  worktree/   — Git worktree orchestration (mode-agnostic)
  settings/   — Shared configuration and launch mode
```

### ADR 0002 — Kotlin ACP SDK

- ACP client protocol and transport use `agentclientprotocol/kotlin-sdk` in the `agent/acp/` slice.
- `ClientSessionOperations` implemented in Kotlin with coroutines and IntelliJ threading rules.
- Java SDK and hand-rolled JSON-RPC were rejected.

### ADR 0003 — Per-project agent selection

- **Agent configuration definitions** and **default configuration** are application-scoped (`agentSettings.xml`).
- **Selected agent** is per-`Project`, workspace-local (not VCS).
- `AgentConfigurationSelector` is the single facade (`getSelectedConfiguration` / `setSelectedConfiguration`).
- Resolution: project saved ID → seed from default on first access → fallback when stale → rewrite project state.
- Toolbar selection does **not** change the global default; Settings **Default** column writes only `defaultConfigurationId`.

## Agent Synthesis

- New features should land in the slice that owns the concern (`acp/`, `pty/`, `worktree/`, `settings/`) and respect ADR boundaries (no AI Chat delegation, Kotlin SDK for protocol code).
- Call sites that need the active agent must pass `Project` into `AgentConfigurationSelector` rather than reading a global selected ID from `AgentSettingsState`.

## Open Questions

- Koog / custom ACP agent implementation is deferred to 4.0 (ADR 0001).
- IntelliJ MCP integration depends on optional AI Assistant and/or MCP Server plugin APIs.

## Related

- [Domain context & ACP transcript model](../concepts/context.md)
- [Agent CLI overview](../concepts/agent-cli-overview.md)
- [Worktree subsystem](worktree.md)
- [Quality gate & release workflow](../workflows/quality-gate.md)
