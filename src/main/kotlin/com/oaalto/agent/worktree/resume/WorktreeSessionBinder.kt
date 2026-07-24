package com.oaalto.agent.worktree.resume

/**
 * Port for persisting the ACP session ID to a worktree record.
 *
 * Implemented by [com.oaalto.agent.worktree.WorktreeSessionBinderImpl] which delegates to
 * [com.oaalto.agent.worktree.AgentWorktreeStateService].
 *
 * The invariant: after any successful session open (load or new), the orchestrator calls
 * [persistSessionId] when a worktree record ID is present.
 */
interface WorktreeSessionBinder {
    /**
     * Persist the current session ID to the worktree record identified by [recordId].
     *
     * @return true if the record was found and updated, false otherwise (blank IDs, record not found).
     */
    fun persistSessionId(
        recordId: String,
        sessionId: String,
    ): Boolean
}
