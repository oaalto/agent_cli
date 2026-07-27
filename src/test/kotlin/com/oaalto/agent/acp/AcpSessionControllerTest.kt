package com.oaalto.agent.acp

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

        override suspend fun start(request: AcpSessionStartRequest): AcpSessionStartResult =
            AcpSessionStartResult(null, "Started a new session.")

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
