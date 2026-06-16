package com.oaalto.agent.worktree.resume

sealed class LaunchResumePlan {
    data class Pty(
        val extraArgs: List<String>,
    ) : LaunchResumePlan()

    data class AcpLoad(
        val sessionId: String,
    ) : LaunchResumePlan()

    data class AcpPickSession(
        val candidates: List<SessionSummary>,
    ) : LaunchResumePlan()

    data object AcpNewSession : LaunchResumePlan()
}

data class SessionSummary(
    val sessionId: String,
    val cwd: String,
    val title: String?,
)
