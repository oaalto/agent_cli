# 01 — Process transport seam

**Parent:** `prd.md`

**What to build:** A testable transport boundary for ACP agent process I/O. Spawning the agent subprocess, wiring stdio pipes into the Kotlin ACP SDK transport, monitoring stderr lines, watching process exit, and tearing down the process and protocol all move behind an internal `AcpProcessTransport` seam. Production continues to use a real subprocess; tests gain an in-memory transport adapter that can exercise protocol wiring without `ProcessBuilder`. User-visible ACP tab behavior is unchanged — this is a structural extraction only.

**Blocked by:** None — can start immediately

**Status:** ready-for-agent

- [ ] An `AcpProcessTransport` abstraction owns process spawn, stdio transport lifecycle, stderr monitoring, exit monitoring, and dispose/teardown (including the deprecated `StdioTransport` constructor suppression and comment).
- [ ] A production adapter (`ProcessStdioTransport` or equivalent) reproduces today's connect/dispose transport behavior with no UX or transcript change.
- [ ] A test adapter (`InMemoryTransport` or SDK test-util wrapper) completes a smoke test proving protocol init can run without spawning a subprocess.
- [ ] `AcpSessionControllerImpl` delegates transport concerns to the seam; existing eight-method public interface and editor call sites still work.
- [ ] `./gradlew qualityGate` passes.
