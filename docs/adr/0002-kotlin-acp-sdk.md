# ADR 0002: Kotlin ACP SDK for the plugin client

- **Status:** Accepted
- **Date:** 2026-06-16

## Context

ADR 0001 commits the Agent CLI plugin to implementing an in-process ACP **client**. The [Agent Client Protocol](https://agentclientprotocol.com/) provides official SDKs in several languages.

Relevant options for this Kotlin IntelliJ plugin:

| SDK | Repository | Notes |
|-----|------------|-------|
| **Kotlin SDK** | `agentclientprotocol/kotlin-sdk` | Coroutines, `ClientSupport` / `ClientSessionOperations`, `TerminalClientSupport` example |
| **Java SDK** | `agentclientprotocol/java-sdk` | Sync/async/Reactor APIs; extensive [Java tutorial](https://github.com/markpollack/acp-java-tutorial) |

The plugin codebase is Kotlin (IntelliJ Platform 2025.3.2). The ACP client must run JSON-RPC over stdio, negotiate capabilities, handle streamed `session/update` notifications, and implement client-side FS, terminal, and permission operations on the EDT/background dispatchers.

## Decision

Use the **Kotlin ACP SDK** (`agentclientprotocol/kotlin-sdk`) for all ACP client protocol and transport code in the `agent/acp/` slice.

Implement `ClientSessionOperations` in Kotlin with coroutines, integrated with IntelliJ threading (`Dispatchers` + `ApplicationManager` rules).

## Alternatives considered

1. **Java SDK from Kotlin** — Viable via interop and richer tutorial material, but adds JVM interop friction and splits the mental model across two SDK styles in one module.
2. **Java SDK core + thin Kotlin façade** — Extra layering without clear benefit given the Kotlin SDK already exposes client APIs and transport modules.
3. **Hand-rolled JSON-RPC** — Rejected. Duplicates maintained protocol code; capability negotiation and schema drift are risky.

## Consequences

### Positive

- Single language for UI (`AgentFileEditor`), worktrees, settings, and ACP client code
- Kotlin SDK `TerminalClientSupport` aligns with the split Transcript + Shell pane design
- Coroutines fit long-lived stdio read loops and streamed session updates
- Test utilities in the Kotlin SDK can support integration tests without the full IDE where feasible

### Negative

- Kotlin SDK documentation is thinner than the Java tutorial; Java tutorial remains reference material only
- If a required ACP feature lands in Java SDK first, we may need to wait for Kotlin SDK parity or contribute upstream

### Neutral

- Gradle dependency added to `build.gradle.kts` for the Kotlin SDK artifacts
- Agent-side SDK (Koog, Java `AcpAgent`) is out of scope until 4.0
