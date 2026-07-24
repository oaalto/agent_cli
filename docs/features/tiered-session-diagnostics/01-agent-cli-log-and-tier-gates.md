# 01 — AgentCliLog helper and tier gate functions

**Parent:** `prd.md`

**What to build:** Introduce the shared **Session diagnostics** IDE-log helper and pure tier gate logic so plugin maintainers get a single, consistent logging API before any call-site migration. Tier 1 (`warn`/`error`) always emits to `idea.log`; tier 2 (`info`) and tier 3 (`debug`) emit only when enabled via environment variable or IntelliJ registry, with debug automatically enabling info. Log lines accept optional session context (`configId`, `sessionId`, launch mode, worktree path) and omit unknown fields rather than emitting placeholders. Tier 2 and tier 3 use lazy message evaluation so disabled tiers avoid expensive string building. Secret redaction helpers for tier-3 launch-plan and settings detail are available for later call sites. No existing production logging call sites are migrated in this ticket.

**Blocked by:** None — can start immediately

**Status:** ready-for-agent

- [ ] `AgentCliLog` exposes tier-aligned methods (`error`, `warn`, `info`, `debug`) wrapping IntelliJ `Logger`, with one instance per owning class mirroring today's `Logger.getInstance` pattern
- [ ] `AgentCliSessionContext` carries optional `configId`, `sessionId`, launch mode, and `worktreePath`; context is formatted consistently and grep-friendly on each emitted line
- [ ] Pure gate functions `isAgentCliLogEnabled` and `isAgentCliDebugEnabled` encode PRD rules: tier 2 on `AGENT_CLI_LOG=true` or registry `agent_cli.log=true` or when debug is enabled; tier 3 on `AGENT_CLI_DEBUG=true` or registry `agent_cli.debug=true`; debug implies log
- [ ] Production gate wiring reads process environment and IntelliJ registry keys `agent_cli.log` and `agent_cli.debug` following existing plugin registry conventions
- [ ] Unit tests cover the gate matrix (env-only, registry-only, both, neither, debug-implies-log) using injected env maps and registry booleans — no IDE startup required
- [ ] If a redaction helper is extracted, unit tests assert known secret env keys and auth token values never appear in debug strings while non-sensitive values remain usable
- [ ] No migration of existing direct `Logger` call sites; no new tier-1 coverage at dialog-only failure paths yet
- [ ] `./gradlew qualityGate` passes
