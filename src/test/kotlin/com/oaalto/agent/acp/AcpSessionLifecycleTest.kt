package com.oaalto.agent.acp

import com.oaalto.agent.worktree.resume.AcpSessionOpenResult
import com.oaalto.agent.worktree.resume.AcpSessionOperations
import com.oaalto.agent.worktree.resume.LaunchResumePlan
import com.oaalto.agent.worktree.resume.SessionPicker
import com.oaalto.agent.worktree.resume.SessionSummary
import com.oaalto.agent.worktree.resume.WorktreeSessionBinder
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AcpSessionLifecycleTest {
    @Test
    fun `openSessionWithOps persists when worktreeRecordId is set`() =
        runBlocking {
            val binder = RecordingWorktreeSessionBinder()
            val lifecycle =
                AcpSessionLifecycle(
                    sessionOperationsFactory = UnusedSessionOperationsFactory,
                    worktreeSessionBinder = binder,
                )

            val result =
                lifecycle.openSessionWithOps(
                    sessionOperations = FakeAcpSessionOperations(),
                    resumePlan = LaunchResumePlan.AcpNewSession,
                    picker = NoOpSessionPicker,
                    worktreeRecordId = "record-1",
                    cwd = "/cwd",
                )

            assertEquals(AcpSessionOpenResult.StartFresh, result)
            assertEquals(1, binder.persistCalls.size)
            assertEquals("record-1", binder.persistCalls[0].recordId)
            assertEquals("new-session-1", binder.persistCalls[0].sessionId)
        }

    @Test
    fun `openSessionWithOps does not persist when worktreeRecordId is null`() =
        runBlocking {
            val binder = RecordingWorktreeSessionBinder()
            val lifecycle =
                AcpSessionLifecycle(
                    sessionOperationsFactory = UnusedSessionOperationsFactory,
                    worktreeSessionBinder = binder,
                )

            lifecycle.openSessionWithOps(
                sessionOperations = FakeAcpSessionOperations(),
                resumePlan = LaunchResumePlan.AcpNewSession,
                picker = NoOpSessionPicker,
                worktreeRecordId = null,
                cwd = "/cwd",
            )

            assertTrue(binder.persistCalls.isEmpty())
        }

    private class RecordingWorktreeSessionBinder : WorktreeSessionBinder {
        val persistCalls = mutableListOf<PersistCall>()

        override fun persistSessionId(
            recordId: String,
            sessionId: String,
        ): Boolean {
            persistCalls.add(PersistCall(recordId, sessionId))
            return true
        }

        data class PersistCall(
            val recordId: String,
            val sessionId: String,
        )
    }

    private class FakeAcpSessionOperations : AcpSessionOperations {
        override suspend fun newSession() = Unit

        override suspend fun loadSession(sessionId: String) = Unit

        override suspend fun listSessions(cwd: String?): List<SessionSummary> = emptyList()

        override fun currentSessionId(): String? = "new-session-1"
    }

    private object NoOpSessionPicker : SessionPicker {
        override suspend fun pickSession(candidates: List<SessionSummary>): String? = null
    }

    private object UnusedSessionOperationsFactory : AcpClientSessionOperationsFactory {
        override fun create(context: AcpEditorContext) = error("Unused in wiring test")
    }
}
