package com.oaalto.agent.worktree.resume

/**
 * Structured result from [AcpSessionResumeOrchestrator.openSession].
 *
 * The editor maps these variants to [com.oaalto.agent.acp.Transcript] messages.
 * The orchestrator returns these so the editor does not own orchestration logic.
 */
sealed class AcpSessionOpenResult {
    /**
     * Session opened successfully.
     *
     * @param sessionId the opened session ID.
     * @param pickerShown whether the session picker UI was shown to the user.
     */
    data class Success(
        val sessionId: String,
        val pickerShown: Boolean = false,
    ) : AcpSessionOpenResult()

    /**
     * Session open failed and a fallback was applied (e.g., started new session).
     *
     * @param reason user-visible explanation of what went wrong.
     * @param sessionId the session ID after fallback (may be null if no session was opened).
     */
    data class Fallback(
        val reason: String,
        val sessionId: String?,
    ) : AcpSessionOpenResult()

    /**
     * Started a fresh session (no picker, no load attempt).
     * Used when there are no candidates or the plan explicitly requests a new session.
     */
    data object StartFresh : AcpSessionOpenResult()
}
