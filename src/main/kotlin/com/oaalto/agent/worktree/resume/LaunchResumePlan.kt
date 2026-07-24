package com.oaalto.agent.worktree.resume

sealed class LaunchResumePlan {
    data class Pty(
        val extraArgs: List<String>,
    ) : LaunchResumePlan()

    data class AcpLoad(
        val sessionId: String,
    ) : LaunchResumePlan()

    data object AcpResolveSession : LaunchResumePlan()

    data object AcpNewSession : LaunchResumePlan()
}

data class SessionSummary(
    val sessionId: String,
    val cwd: String,
    val title: String?,
)
