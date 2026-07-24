package com.oaalto.agent.worktree.resume

/**
 * Port for ACP session protocol operations.
 *
 * Implemented by adapters in the `acp/` slice that delegate to [com.oaalto.agent.acp.AcpSessionController].
 * The orchestrator in this slice depends only on this interface — never on the controller directly.
 */
interface AcpSessionOperations {
    suspend fun newSession()

    suspend fun loadSession(sessionId: String)

    suspend fun listSessions(cwd: String?): List<SessionSummary>

    fun currentSessionId(): String?
}
