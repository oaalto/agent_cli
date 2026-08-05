package com.oaalto.agent.acp

import com.oaalto.agent.acp.transcript.model.StructuredUpdate
import com.oaalto.agent.worktree.resume.LaunchResumePlan
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

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

    @Test
    fun `dispose after successful start does not throw and disposes transport`() =
        runBlocking {
            val harness = AcpSessionLoopTestHarness(scriptedSessionId = "dispose-after-start")
            try {
                harness.controller.start(harness.newSessionStartRequest())

                harness.controller.dispose()

                assertTrue(harness.transport.isDisposed)
                assertTrue(harness.listener.errors.isEmpty())
            } finally {
                harness.dispose()
            }
        }

    @Test
    fun `dispose during slow prompt stream completes without hang`() =
        runBlocking {
            val harness =
                AcpSessionLoopTestHarness(
                    scriptedSessionId = "dispose-mid-prompt",
                    promptDelayBetweenUpdates = 200.milliseconds,
                )
            try {
                harness.controller.start(harness.newSessionStartRequest())

                val promptDeferred = async { harness.controller.prompt("slow prompt") }
                awaitFirstAgentTextChunk(harness)

                harness.controller.dispose()

                withTimeout(5.seconds) {
                    promptDeferred.await()
                }

                assertTrue(harness.transport.isDisposed)
                assertTrue(
                    harness.listener.structuredUpdates.any { it is StructuredUpdate.FinalizeAgentStream },
                )
            } finally {
                harness.dispose()
            }
        }

    @Test
    fun `cancelPrompt during slow prompt stream completes without hang`() =
        runBlocking {
            val harness =
                AcpSessionLoopTestHarness(
                    scriptedSessionId = "cancel-mid-prompt",
                    promptDelayBetweenUpdates = 200.milliseconds,
                )
            try {
                harness.controller.start(harness.newSessionStartRequest())

                val promptDeferred = async { harness.controller.prompt("slow prompt") }
                awaitFirstAgentTextChunk(harness)

                harness.controller.cancelPrompt()

                withTimeout(5.seconds) {
                    promptDeferred.await()
                }

                assertTrue(!harness.transport.isDisposed)
                assertTrue(
                    harness.listener.structuredUpdates.any { it is StructuredUpdate.FinalizeAgentStream },
                )
            } finally {
                harness.dispose()
            }
        }

    @Test
    fun `second start after dispose is blocked because controller scope is cancelled`() {
        runBlocking {
            val harness = AcpSessionLoopTestHarness(scriptedSessionId = "no-restart-after-dispose")
            try {
                harness.controller.start(harness.newSessionStartRequest())
                harness.controller.dispose()

                assertTrue(harness.transport.isDisposed)
                assertFailsWith<TimeoutCancellationException> {
                    withTimeout(2.seconds) {
                        harness.controller.start(harness.newSessionStartRequest())
                    }
                }
            } finally {
                harness.dispose()
            }
        }
    }

    private suspend fun awaitFirstAgentTextChunk(harness: AcpSessionLoopTestHarness) {
        withTimeout(5.seconds) {
            while (
                harness.listener.structuredUpdates.none { it is StructuredUpdate.AppendAgentText }
            ) {
                delay(25)
            }
        }
    }
}
