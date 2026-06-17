package com.oaalto.agent.worktree.resume

import com.oaalto.agent.settings.AgentSettingsState
import java.util.Locale

object PtyResumeStrategy : ResumeStrategy {
    override fun prepareLaunch(context: ResumeContext): LaunchResumePlan {
        val baseArgs = if (context.resume) baseResumeArguments(context.configuration) else null
        if (baseArgs == null) {
            return LaunchResumePlan.Pty(emptyList())
        }
        val executionTarget = resolveExecutionTarget(context.configuration.executionTarget)
        val probed =
            context.cursorProbe.applyIfNeeded(
                CursorResumeProbeRequest(
                    binaryPath = context.configuration.binaryPath,
                    arguments = baseArgs,
                    executionTarget = executionTarget,
                    workingDirectory = context.workingDirectory,
                    wslDistribution = context.wslDistribution,
                    wslWorkingDirectory = context.wslWorkingDirectory.orEmpty(),
                    hostWorkingDirectory = context.hostWorkingDirectory,
                ),
            )
        return LaunchResumePlan.Pty(probed)
    }

    fun baseResumeArguments(configuration: AgentSettingsState.AgentCliConfiguration): List<String>? =
        when (executableName(configuration.binaryPath)) {
            "cursor-agent", "agent", "claude" -> listOf("--continue")
            "opencode" -> listOf("--continue")
            "gemini" -> listOf("--resume")
            "codex" -> listOf("resume", "--last")
            else -> null
        }

    fun canResume(configuration: AgentSettingsState.AgentCliConfiguration): Boolean =
        baseResumeArguments(configuration) != null

    private fun resolveExecutionTarget(rawTarget: String): AgentSettingsState.ExecutionTarget {
        val normalized = rawTarget.trim().uppercase(Locale.ROOT)
        return AgentSettingsState.ExecutionTarget.entries.firstOrNull { it.name == normalized }
            ?: AgentSettingsState.ExecutionTarget.LOCAL
    }

    private fun executableName(binaryPath: String): String {
        val normalizedPath = binaryPath.trim()
        if (normalizedPath.isBlank()) return ""
        val fileName = normalizedPath.substringAfterLast('/').substringAfterLast('\\')
        return fileName.substringBeforeLast('.').lowercase(Locale.ROOT)
    }
}
