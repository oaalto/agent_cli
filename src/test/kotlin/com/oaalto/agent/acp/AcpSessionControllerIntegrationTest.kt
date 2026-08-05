package com.oaalto.agent.acp

import com.oaalto.agent.acp.transcript.model.StructuredUpdate
import com.oaalto.agent.worktree.resume.LaunchResumePlan
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
                assertTrue(harness.scriptedAgent.loadSessionIds.isEmpty())
                assertTrue(harness.listener.errors.isEmpty())
                assertTrue(!harness.sessionPicker.pickCalled)
            } finally {
                harness.dispose()
            }
        }

    @Test
    fun `start with AcpLoad plan exercises session load and returns restoreTranscript`() =
        runBlocking {
            val loadSessionId = "resume-session-99"
            val harness =
                AcpSessionLoopTestHarness(
                    scriptedSessionId = "new-session-fallback",
                    loadSessionId = loadSessionId,
                )
            try {
                val result =
                    harness.controller.start(
                        harness.newSessionStartRequest(
                            resumePlan = LaunchResumePlan.AcpLoad(loadSessionId),
                        ),
                    )

                assertEquals(loadSessionId, result.sessionId)
                assertEquals("", result.statusMessage)
                assertEquals(true, result.restoreTranscript)
                assertEquals(listOf(loadSessionId), harness.scriptedAgent.loadSessionIds)
                assertTrue(harness.listener.errors.isEmpty())
                assertTrue(!harness.sessionPicker.pickCalled)
            } finally {
                harness.dispose()
            }
        }

    @Test
    fun `prompt delivers scripted session updates to recording listener in order`() =
        runBlocking {
            val harness = AcpSessionLoopTestHarness(scriptedSessionId = "prompt-session-1")
            try {
                harness.controller.start(harness.newSessionStartRequest())
                harness.controller.prompt("hello agent")

                assertEquals(listOf("hello agent"), harness.scriptedAgent.receivedPrompts)
                assertTrue(harness.listener.errors.isEmpty())

                val updates = harness.listener.structuredUpdates
                val textChunks =
                    updates.filterIsInstance<StructuredUpdate.AppendAgentText>().map { it.text }
                assertEquals(listOf("scripted ", "reply"), textChunks)

                val toolIndex =
                    updates.indexOfFirst { it is StructuredUpdate.StartOrUpdateToolCall }
                assertTrue(toolIndex >= 1)
                assertTrue(updates[toolIndex - 1] is StructuredUpdate.FinalizeAgentStream)

                assertTrue(updates.last() is StructuredUpdate.FinalizeAgentStream)
            } finally {
                harness.dispose()
            }
        }
}
