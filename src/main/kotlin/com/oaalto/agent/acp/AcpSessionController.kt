package com.oaalto.agent.acp

import com.oaalto.agent.acp.AcpEditorContext
import com.oaalto.agent.worktree.resume.SessionSummary

interface AcpSessionController {
    suspend fun connect(
        launchPlan: AcpLaunchPlan,
        editorContext: AcpEditorContext,
    )

    suspend fun newSession()

    suspend fun loadSession(sessionId: String)

    suspend fun listSessions(cwd: String?): List<SessionSummary>

    fun currentSessionId(): String?

    suspend fun prompt(text: String)

    suspend fun cancelPrompt()

    fun dispose()
}
