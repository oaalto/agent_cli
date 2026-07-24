package com.oaalto.agent.acp

import com.oaalto.agent.worktree.resume.AcpSessionOperations
import com.oaalto.agent.worktree.resume.SessionSummary

/**
 * Thin adapter that delegates [AcpSessionOperations] calls to an [AcpSessionController].
 *
 * The adapter does not change protocol behavior — it exists to satisfy the orchestrator's
 * dependency on the port interface rather than the controller directly.
 */
class AcpSessionOperationsAdapter(
    private val controller: AcpSessionController,
) : AcpSessionOperations {
    override suspend fun newSession() {
        controller.newSession()
    }

    override suspend fun loadSession(sessionId: String) {
        controller.loadSession(sessionId)
    }

    override suspend fun listSessions(cwd: String?): List<SessionSummary> = controller.listSessions(cwd)

    override fun currentSessionId(): String? = controller.currentSessionId()
}
