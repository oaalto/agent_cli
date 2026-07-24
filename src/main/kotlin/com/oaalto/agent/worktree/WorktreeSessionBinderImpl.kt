package com.oaalto.agent.worktree

import com.oaalto.agent.worktree.resume.WorktreeSessionBinder

/**
 * Adapter that delegates [WorktreeSessionBinder] calls to [AgentWorktreeStateService].
 *
 * Uses the singleton instance — the adapter is a pass-through with no additional logic.
 */
class WorktreeSessionBinderImpl : WorktreeSessionBinder {
    override fun persistSessionId(
        recordId: String,
        sessionId: String,
    ): Boolean = AgentWorktreeStateService.getInstance().setAcpSessionId(recordId, sessionId)
}
