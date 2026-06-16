package com.oaalto.agent.acp

import com.oaalto.agent.worktree.resume.SessionSummary
import kotlin.test.Test
import kotlin.test.assertTrue

class AcpSessionControllerTest {
    @Test
    fun `dispose cancels active prompt work`() {
        val controller = RecordingSessionController()

        controller.dispose()

        assertTrue(controller.cancelled)
        assertTrue(controller.disposed)
    }

    private class RecordingSessionController : AcpSessionController {
        var cancelled = false
        var disposed = false

        override suspend fun connect(
            launchPlan: AcpLaunchPlan,
            editorContext: AcpEditorContext,
        ) = Unit

        override suspend fun newSession() = Unit

        override suspend fun loadSession(sessionId: String) = Unit

        override suspend fun listSessions(cwd: String?): List<SessionSummary> = emptyList()

        override fun currentSessionId(): String? = null

        override suspend fun prompt(text: String) = Unit

        override suspend fun cancelPrompt() {
            cancelled = true
        }

        override fun dispose() {
            cancelled = true
            disposed = true
        }
    }
}
