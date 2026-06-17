package com.oaalto.agent.pty

import com.oaalto.agent.settings.AgentSettingsState
import com.oaalto.agent.worktree.resume.CursorResumeProbe
import com.oaalto.agent.worktree.resume.CursorResumeProbeRequest
import java.util.Locale

internal fun resolvePtyConfiguration(
    configurationId: String,
    onMissing: () -> Unit,
): AgentSettingsState.AgentCliConfiguration? {
    val configuration = AgentSettingsState.getInstance().getConfigurationById(configurationId)
    if (configuration == null) {
        onMissing()
    }
    return configuration
}

internal fun resolvePtyExecutionTarget(rawTarget: String): AgentSettingsState.ExecutionTarget {
    val normalized = rawTarget.trim().uppercase(Locale.ROOT)
    return AgentSettingsState.ExecutionTarget.entries.firstOrNull { it.name == normalized }
        ?: AgentSettingsState.ExecutionTarget.LOCAL
}

internal fun applyCursorResumeFallbackForLocal(
    binaryPath: String,
    arguments: List<String>,
    workingDirectory: String,
): List<String> =
    CursorResumeProbe.Default.applyIfNeeded(
        CursorResumeProbeRequest(
            binaryPath = binaryPath,
            arguments = arguments,
            executionTarget = AgentSettingsState.ExecutionTarget.LOCAL,
            workingDirectory = workingDirectory,
        ),
    )

internal fun applyCursorResumeFallbackForWsl(
    binaryPath: String,
    arguments: List<String>,
    wslDistribution: String,
    wslWorkingDirectory: String,
    hostWorkingDirectory: String,
): List<String> =
    CursorResumeProbe.Default.applyIfNeeded(
        CursorResumeProbeRequest(
            binaryPath = binaryPath,
            arguments = arguments,
            executionTarget = AgentSettingsState.ExecutionTarget.WSL,
            workingDirectory = wslWorkingDirectory,
            wslDistribution = wslDistribution,
            wslWorkingDirectory = wslWorkingDirectory,
            hostWorkingDirectory = hostWorkingDirectory,
        ),
    )
