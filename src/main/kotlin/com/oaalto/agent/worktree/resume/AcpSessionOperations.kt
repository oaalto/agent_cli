package com.oaalto.agent.worktree.resume

/**
 * Port for ACP session protocol operations.
 *
 * Implemented by lifecycle adapters in the `acp/` slice (for example [com.oaalto.agent.acp.AcpSessionLifecycle]).
 * The orchestrator in this slice depends only on this interface — never on the editor-facing controller directly.
 */
interface AcpSessionOperations {
    suspend fun newSession()

    suspend fun loadSession(sessionId: String)

    suspend fun listSessions(cwd: String?): List<SessionSummary>

    fun currentSessionId(): String?
}
