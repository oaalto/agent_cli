package com.oaalto.agent.acp

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AcpSessionControllerIntegrationTest {
    @Test
    fun `start with new session plan completes connect bootstrap bind and startSession`() =
        runBlocking {
            val scriptedSessionId = "integration-session-42"
            val harness = AcpSessionLoopTestHarness(scriptedSessionId = scriptedSessionId)
            try {
                // connect → bootstrap (initialize) → bind → startSession (orchestrator new-session)
                val result =
                    harness.controller.start(
                        harness.newSessionStartRequest(),
                    )

                assertEquals(scriptedSessionId, result.sessionId)
                assertEquals("Started a new ACP session.", result.statusMessage)
                assertEquals(false, result.restoreTranscript)
                assertTrue(harness.listener.errors.isEmpty())
                assertTrue(!harness.sessionPicker.pickCalled)
            } finally {
                harness.dispose()
            }
        }
}
