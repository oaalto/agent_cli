package com.oaalto.agent.worktree.resume

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AcpSessionResumeOrchestratorTest {
    @Test
    fun `PTY plan returns fallback with error message`() =
        runBlocking {
            val orchestrator = createOrchestrator()
            val result =
                orchestrator.openSession(
                    plan = LaunchResumePlan.Pty(emptyList()),
                    sessionWorkingDirectory = "/cwd",
                    worktreeRecordId = "record-1",
                )
            assertEquals(
                AcpSessionOpenResult.Fallback(
                    reason = "PTY resume is not supported in ACP mode",
                    sessionId = null,
                ),
                result,
            )
            // No other calls should be made
            assertTrue(orchestratorState.acpOps.newSessionCalls == 0)
            assertTrue(orchestratorState.acpOps.loadSessionIds.isEmpty())
            assertTrue(orchestratorState.acpOps.listSessionsCwds.isEmpty())
            assertTrue(orchestratorState.binder.persistCalls.isEmpty())
        }

    @Test
    fun `AcpLoad success persists and returns success`() =
        runBlocking {
            val orchestrator = createOrchestrator(loadResult = LoadResult.Success)
            val result =
                orchestrator.openSession(
                    plan = LaunchResumePlan.AcpLoad("session-42"),
                    sessionWorkingDirectory = "/cwd",
                    worktreeRecordId = "record-1",
                )
            assertEquals(AcpSessionOpenResult.Success("session-42", pickerShown = false), result)
            assertEquals(listOf("session-42"), orchestratorState.acpOps.loadSessionIds)
            assertEquals(1, orchestratorState.binder.persistCalls.size)
            assertEquals("record-1", orchestratorState.binder.persistCalls[0].recordId)
            assertEquals("session-42", orchestratorState.binder.persistCalls[0].sessionId)
        }

    @Test
    fun `AcpLoad failure falls through to list and pick`() =
        runBlocking {
            val orchestrator =
                createOrchestrator(
                    loadResult = LoadResult.Failure("load failed"),
                    sessions = listOf(SessionSummary("s1", "/cwd", "Session 1")),
                    pickerResult = "s1",
                )
            val result =
                orchestrator.openSession(
                    plan = LaunchResumePlan.AcpLoad("session-42"),
                    sessionWorkingDirectory = "/cwd",
                    worktreeRecordId = "record-1",
                )
            // Should have tried to load the stored session, then listed, then picker was shown
            // The picker's selected session also fails to load, so loadSession is called twice
            assertEquals(2, orchestratorState.acpOps.loadSessionIds.size)
            assertEquals("session-42", orchestratorState.acpOps.loadSessionIds[0])
            assertEquals("s1", orchestratorState.acpOps.loadSessionIds[1])
            assertEquals<List<String?>>(listOf("/cwd"), orchestratorState.acpOps.listSessionsCwds)
            assertTrue(orchestratorState.picker.pickCalled)
        }

    @Test
    fun `AcpResolveSession with empty list starts new session`() =
        runBlocking {
            val orchestrator = createOrchestrator(sessions = emptyList())
            val result =
                orchestrator.openSession(
                    plan = LaunchResumePlan.AcpResolveSession,
                    sessionWorkingDirectory = "/cwd",
                    worktreeRecordId = "record-1",
                )
            assertEquals(AcpSessionOpenResult.StartFresh, result)
            assertEquals(1, orchestratorState.acpOps.newSessionCalls)
            assertEquals(1, orchestratorState.binder.persistCalls.size)
            assertEquals("record-1", orchestratorState.binder.persistCalls[0].recordId)
            assertEquals("new-session-1", orchestratorState.binder.persistCalls[0].sessionId)
        }

    @Test
    fun `AcpResolveSession with non-empty list and picker returns null starts new session`() =
        runBlocking {
            val orchestrator =
                createOrchestrator(
                    sessions = listOf(SessionSummary("s1", "/cwd", "Session 1")),
                    pickerResult = null,
                )
            val result =
                orchestrator.openSession(
                    plan = LaunchResumePlan.AcpResolveSession,
                    sessionWorkingDirectory = "/cwd",
                    worktreeRecordId = "record-1",
                )
            assertEquals(AcpSessionOpenResult.StartFresh, result)
            assertEquals(1, orchestratorState.acpOps.newSessionCalls)
            assertEquals(1, orchestratorState.binder.persistCalls.size)
        }

    @Test
    fun `AcpResolveSession with picker returning session ID loads and persists`() =
        runBlocking {
            val orchestrator =
                createOrchestrator(
                    sessions = listOf(SessionSummary("s1", "/cwd", "Session 1")),
                    pickerResult = "s1",
                    loadResult = LoadResult.Success,
                )
            val result =
                orchestrator.openSession(
                    plan = LaunchResumePlan.AcpResolveSession,
                    sessionWorkingDirectory = "/cwd",
                    worktreeRecordId = "record-1",
                )
            assertEquals(AcpSessionOpenResult.Success("s1", pickerShown = true), result)
            assertEquals(listOf("s1"), orchestratorState.acpOps.loadSessionIds)
            assertEquals(1, orchestratorState.binder.persistCalls.size)
        }

    @Test
    fun `AcpResolveSession with picker returning session ID but load fails starts new session`() =
        runBlocking {
            val orchestrator =
                createOrchestrator(
                    sessions = listOf(SessionSummary("s1", "/cwd", "Session 1")),
                    pickerResult = "s1",
                    loadResult = LoadResult.Failure("load failed"),
                )
            val result =
                orchestrator.openSession(
                    plan = LaunchResumePlan.AcpResolveSession,
                    sessionWorkingDirectory = "/cwd",
                    worktreeRecordId = "record-1",
                )
            assertEquals(
                AcpSessionOpenResult.Fallback(
                    reason = "Failed to open session: load failed. Started a new session.",
                    sessionId = null,
                ),
                result,
            )
            assertEquals(1, orchestratorState.acpOps.newSessionCalls)
            assertEquals(1, orchestratorState.binder.persistCalls.size)
        }

    @Test
    fun `AcpNewSession starts new session and persists`() =
        runBlocking {
            val orchestrator = createOrchestrator()
            val result =
                orchestrator.openSession(
                    plan = LaunchResumePlan.AcpNewSession,
                    sessionWorkingDirectory = "/cwd",
                    worktreeRecordId = "record-1",
                )
            assertEquals(AcpSessionOpenResult.StartFresh, result)
            assertEquals(1, orchestratorState.acpOps.newSessionCalls)
            assertEquals(1, orchestratorState.binder.persistCalls.size)
        }

    @Test
    fun `AcpResolveSession with listSessions failure starts new session`() =
        runBlocking {
            val orchestrator =
                createOrchestrator(
                    loadResult = LoadResult.Failure("list failed"),
                )
            val result =
                orchestrator.openSession(
                    plan = LaunchResumePlan.AcpResolveSession,
                    sessionWorkingDirectory = "/cwd",
                    worktreeRecordId = "record-1",
                )
            assertEquals(AcpSessionOpenResult.StartFresh, result)
            assertEquals(1, orchestratorState.acpOps.newSessionCalls)
            assertEquals(1, orchestratorState.binder.persistCalls.size)
        }

    @Test
    fun `AcpNewSession with null worktreeRecordId does not persist`() =
        runBlocking {
            val orchestrator = createOrchestrator()
            val result =
                orchestrator.openSession(
                    plan = LaunchResumePlan.AcpNewSession,
                    sessionWorkingDirectory = "/cwd",
                    worktreeRecordId = null,
                )
            assertEquals(AcpSessionOpenResult.StartFresh, result)
            assertEquals(1, orchestratorState.acpOps.newSessionCalls)
            assertTrue(orchestratorState.binder.persistCalls.isEmpty())
        }

    @Test
    fun `AcpLoad with null worktreeRecordId does not persist`() =
        runBlocking {
            val orchestrator = createOrchestrator(loadResult = LoadResult.Success)
            val result =
                orchestrator.openSession(
                    plan = LaunchResumePlan.AcpLoad("session-42"),
                    sessionWorkingDirectory = "/cwd",
                    worktreeRecordId = null,
                )
            assertEquals(AcpSessionOpenResult.Success("session-42", pickerShown = false), result)
            assertEquals(0, orchestratorState.binder.persistCalls.size)
        }

    private fun createOrchestrator(
        loadResult: LoadResult = LoadResult.Success,
        sessions: List<SessionSummary> = emptyList(),
        pickerResult: String? = "s1",
    ): AcpSessionResumeOrchestrator {
        val acpOps = FakeAcpSessionOperations(loadResult = loadResult, sessions = sessions)
        val binder = FakeWorktreeSessionBinder()
        val picker = FakeSessionPicker(pickerResult)
        return AcpSessionResumeOrchestrator(acpOps, binder, picker).also {
            orchestratorState = OrchestratorState(acpOps, binder, picker)
        }
    }

    private sealed class LoadResult {
        data object Success : LoadResult()

        data class Failure(
            val message: String,
        ) : LoadResult()
    }

    private class FakeAcpSessionOperations(
        private val loadResult: LoadResult,
        private val sessions: List<SessionSummary>,
    ) : AcpSessionOperations {
        var newSessionCalls = 0
        val loadSessionIds = mutableListOf<String>()
        val listSessionsCwds = mutableListOf<String?>()
        var currentId: String? = "new-session-1"

        override suspend fun newSession() {
            newSessionCalls++
            currentId = "new-session-$newSessionCalls"
        }

        override suspend fun loadSession(sessionId: String) {
            loadSessionIds.add(sessionId)
            when (loadResult) {
                LoadResult.Success -> Unit
                is LoadResult.Failure -> error(loadResult.message)
            }
            currentId = sessionId
        }

        override suspend fun listSessions(cwd: String?): List<SessionSummary> {
            listSessionsCwds.add(cwd)
            return sessions
        }

        override fun currentSessionId(): String? = currentId
    }

    private class FakeWorktreeSessionBinder : WorktreeSessionBinder {
        data class PersistCall(
            val recordId: String,
            val sessionId: String,
        )

        val persistCalls = mutableListOf<PersistCall>()

        override fun persistSessionId(
            recordId: String,
            sessionId: String,
        ): Boolean {
            persistCalls.add(PersistCall(recordId, sessionId))
            return true
        }
    }

    private class FakeSessionPicker(
        private val result: String?,
    ) : SessionPicker {
        var pickCalled = false

        override suspend fun pickSession(candidates: List<SessionSummary>): String? {
            pickCalled = true
            return result
        }
    }

    private class OrchestratorState(
        val acpOps: FakeAcpSessionOperations,
        val binder: FakeWorktreeSessionBinder,
        val picker: FakeSessionPicker,
    )

    private lateinit var orchestratorState: OrchestratorState
}
