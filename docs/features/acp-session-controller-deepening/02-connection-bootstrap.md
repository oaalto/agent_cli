# 02 — Connection bootstrap extraction

**Parent:** `prd.md`

**What to build:** Protocol initialization and authentication move into a dedicated internal `AcpConnectionBootstrap` module. After transport connects, bootstrap starts the protocol, initializes the ACP client with capabilities, and runs `AuthFlowCoordinator` — same ordering and behavior as today's `initializeConnectedClient`. A developer opening an ACP tab still sees identical auth prompts and connection success; auth coordinator public surface stays unchanged.

**Blocked by:** 01 — Process transport seam

**Status:** done

- [x] `AcpConnectionBootstrap` encapsulates protocol start, client initialize, and `AuthFlowCoordinator.authenticateIfRequired()` for a connected transport.
- [x] Bootstrap failures complete session-ready exceptionally and dispose transport, matching current error recovery.
- [x] `AcpSessionControllerImpl` calls bootstrap through the transport seam from ticket 01; connect still works end-to-end.
- [x] No change to auth method support, capabilities negotiation, or transcript messages during connect.
- [x] `./gradlew qualityGate` passes.
