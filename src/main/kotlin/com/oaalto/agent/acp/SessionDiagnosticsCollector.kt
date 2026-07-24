package com.oaalto.agent.acp

import com.oaalto.agent.AgentCliCorrelationToken
import com.oaalto.agent.AgentCliSessionContext
import com.oaalto.agent.settings.LaunchMode

internal class SessionDiagnosticsCollector(
    private val maxRecentErrors: Int = 5,
) {
    private val recentErrors = ArrayDeque<RecordedError>()

    data class RecordedError(
        val token: String,
        val message: String,
    )

    fun record(
        token: String,
        message: String,
    ) {
        recentErrors.addLast(RecordedError(token, message))
        while (recentErrors.size > maxRecentErrors) {
            recentErrors.removeFirst()
        }
    }

    fun recentErrors(): List<RecordedError> = recentErrors.toList()

    fun formatClipboardBundle(
        context: AgentCliSessionContext,
        worktreeLabel: String,
    ): String {
        val lines = mutableListOf<String>()
        lines += "Agent CLI session diagnostics"
        context.configId?.let { lines += "configId: $it" }
        context.sessionId?.let { lines += "sessionId: $it" }
        context.launchMode?.let { lines += "launchMode: ${it.name}" }
        lines += "worktree: $worktreeLabel"
        val tokens = recentErrors.map { AgentCliCorrelationToken.format(it.token) }
        if (tokens.isNotEmpty()) {
            lines += "correlationTokens: ${tokens.joinToString(", ")}"
        }
        if (recentErrors.isNotEmpty()) {
            lines += "recentErrors:"
            recentErrors.forEach { error ->
                lines += "  - ${error.message} ${AgentCliCorrelationToken.format(error.token)}"
            }
        }
        return lines.joinToString("\n")
    }

    companion object {
        fun worktreeLabel(
            launchMode: LaunchMode,
            worktreePath: String?,
        ): String =
            worktreePath?.takeIf { it.isNotBlank() }
                ?: if (launchMode == LaunchMode.ACP_CLIENT) "current-project" else "n/a"
    }
}
