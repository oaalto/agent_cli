package com.oaalto.agent.worktree.resume

/**
 * Orchestrates ACP session-open execution after connect.
 *
 * Owns the full decision tree for resume: interpret [LaunchResumePlan], run load/list/pick/new-session
 * branches, enforce the worktree binding invariant, and return structured results.
 *
 * The orchestrator depends only on port interfaces — never on [com.oaalto.agent.acp.AcpSessionController],
 * [com.oaalto.agent.worktree.AgentWorktreeStateService], or Swing UI. This makes it fully testable
 * with fake ports.
 *
 * **Binding invariant:** When `worktreeRecordId` is non-null and a session is successfully opened
 * (load or new), call [WorktreeSessionBinder.persistSessionId] with `currentSessionId()`.
 */
class AcpSessionResumeOrchestrator(
    private val acpOps: AcpSessionOperations,
    private val binder: WorktreeSessionBinder,
    private val picker: SessionPicker,
) {
    /**
     * Open an ACP session according to the given plan.
     *
     * @param plan the resume plan produced by [com.oaalto.agent.worktree.resume.ResumeStrategy].
     * @param sessionWorkingDirectory the working directory for session listing.
     * @param worktreeRecordId the worktree record ID, or null if no worktree is bound.
     * @return a structured result describing what happened.
     */
    suspend fun openSession(
        plan: LaunchResumePlan,
        sessionWorkingDirectory: String,
        worktreeRecordId: String?,
    ): AcpSessionOpenResult =
        when (plan) {
            is LaunchResumePlan.Pty -> {
                AcpSessionOpenResult.Fallback(
                    reason = "PTY resume is not supported in ACP mode",
                    sessionId = null,
                )
            }
            is LaunchResumePlan.AcpLoad -> openFromLoad(plan.sessionId, sessionWorkingDirectory, worktreeRecordId)
            is LaunchResumePlan.AcpResolveSession -> openFromResolve(sessionWorkingDirectory, worktreeRecordId)
            LaunchResumePlan.AcpNewSession -> openNewSession(worktreeRecordId)
        }

    private suspend fun openFromLoad(
        sessionId: String,
        sessionWorkingDirectory: String,
        worktreeRecordId: String?,
    ): AcpSessionOpenResult {
        val loadResult = runCatching { acpOps.loadSession(sessionId) }
        if (loadResult.isSuccess) {
            persistIfNeeded(worktreeRecordId)
            return AcpSessionOpenResult.Success(sessionId, pickerShown = false)
        }

        // Load failed — fall through to list + pick
        val fallbackReason = loadResult.exceptionOrNull()?.message ?: "load failed"
        val listPickResult = listAndPick(sessionWorkingDirectory, worktreeRecordId)
        return when (listPickResult) {
            is AcpSessionOpenResult.Success -> listPickResult
            is AcpSessionOpenResult.Fallback -> listPickResult
            AcpSessionOpenResult.StartFresh -> openNewSession(worktreeRecordId)
        }
    }

    private suspend fun openFromResolve(
        sessionWorkingDirectory: String,
        worktreeRecordId: String?,
    ): AcpSessionOpenResult = listAndPick(sessionWorkingDirectory, worktreeRecordId)

    private suspend fun listAndPick(
        sessionWorkingDirectory: String,
        worktreeRecordId: String?,
    ): AcpSessionOpenResult {
        val sessions = runCatching { acpOps.listSessions(sessionWorkingDirectory) }.getOrDefault(emptyList())

        if (sessions.isEmpty()) {
            return openNewSession(worktreeRecordId)
        }

        return pickAndLoad(sessions, worktreeRecordId)
    }

    private suspend fun pickAndLoad(
        sessions: List<SessionSummary>,
        worktreeRecordId: String?,
    ): AcpSessionOpenResult {
        val selectedId = picker.pickSession(sessions)
        if (selectedId == null) {
            return openNewSession(worktreeRecordId)
        }

        // User picked a session — try to load it
        val loadResult = runCatching { acpOps.loadSession(selectedId) }
        if (loadResult.isSuccess) {
            persistIfNeeded(worktreeRecordId)
            return AcpSessionOpenResult.Success(selectedId, pickerShown = true)
        }

        // Selected session load failed — start fresh
        val fallbackReason = loadResult.exceptionOrNull()?.message ?: "load failed"
        return openNewSessionFallback(worktreeRecordId, fallbackReason)
    }

    private suspend fun openNewSession(worktreeRecordId: String?): AcpSessionOpenResult {
        runSafely { acpOps.newSession() }
        persistIfNeeded(worktreeRecordId)
        return AcpSessionOpenResult.StartFresh
    }

    private suspend fun openNewSessionFallback(
        worktreeRecordId: String?,
        reason: String,
    ): AcpSessionOpenResult {
        runSafely { acpOps.newSession() }
        persistIfNeeded(worktreeRecordId)
        return AcpSessionOpenResult.Fallback(
            reason = "Failed to open session: $reason. Started a new session.",
            sessionId = null,
        )
    }

    private fun persistIfNeeded(worktreeRecordId: String?) {
        if (worktreeRecordId.isNullOrBlank()) return
        val sessionId = acpOps.currentSessionId() ?: return
        binder.persistSessionId(worktreeRecordId, sessionId)
    }

    private suspend inline fun runSafely(block: suspend () -> Unit) {
        runCatching { block() }.onFailure { }
    }
}
