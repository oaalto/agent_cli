package com.oaalto.agent.worktree.resume

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.CapturingProcessHandler
import com.oaalto.agent.AgentCommandBuilder
import com.oaalto.agent.AgentWslCommandRequest
import com.oaalto.agent.settings.AgentSettingsState
import java.nio.file.Files
import java.nio.file.Path
import java.util.Locale

fun interface CursorResumeProbe {
    fun applyIfNeeded(request: CursorResumeProbeRequest): List<String>

    companion object {
        val Default: CursorResumeProbe =
            CursorResumeProbe { request ->
                CursorResumeProbeLogic.applyIfNeeded(request)
            }

        val NoOp: CursorResumeProbe = CursorResumeProbe { request -> request.arguments }
    }
}

data class CursorResumeProbeRequest(
    val binaryPath: String,
    val arguments: List<String>,
    val executionTarget: AgentSettingsState.ExecutionTarget,
    val workingDirectory: String,
    val wslDistribution: String = "",
    val wslWorkingDirectory: String = "",
    val hostWorkingDirectory: String = "",
)

internal object CursorResumeProbeLogic {
    private const val RESUME_PROBE_TIMEOUT_MS = 15_000
    private const val NO_PREVIOUS_CHATS_MESSAGE = "No previous chats found"

    fun applyIfNeeded(request: CursorResumeProbeRequest): List<String> {
        if (!shouldUseCursorResumeFallback(request.binaryPath, request.arguments)) {
            return request.arguments
        }
        val output =
            when (request.executionTarget) {
                AgentSettingsState.ExecutionTarget.LOCAL ->
                    runLocalProbe(
                        binaryPath = request.binaryPath,
                        workingDirectory = request.workingDirectory,
                    )
                AgentSettingsState.ExecutionTarget.WSL ->
                    runWslProbe(
                        binaryPath = request.binaryPath,
                        wslDistribution = request.wslDistribution,
                        wslWorkingDirectory = request.wslWorkingDirectory,
                        hostWorkingDirectory = request.hostWorkingDirectory,
                    )
            }
        return when {
            output == null -> request.arguments
            containsNoPreviousChats(output) -> request.arguments.filterNot { it == "--continue" }
            else -> request.arguments
        }
    }

    private fun shouldUseCursorResumeFallback(
        binaryPath: String,
        runArguments: List<String>,
    ): Boolean {
        if (!runArguments.contains("--continue")) return false
        return when (executableName(binaryPath)) {
            "agent", "cursor-agent" -> true
            else -> false
        }
    }

    private fun runLocalProbe(
        binaryPath: String,
        workingDirectory: String,
    ): String? =
        runProcess(
            command = listOf(binaryPath, "resume"),
            workingDirectory = workingDirectory,
        )

    private fun runWslProbe(
        binaryPath: String,
        wslDistribution: String,
        wslWorkingDirectory: String,
        hostWorkingDirectory: String,
    ): String? {
        val probeCommand =
            AgentCommandBuilder.buildWslCommand(
                AgentWslCommandRequest(
                    binaryPath = binaryPath,
                    arguments = listOf("resume"),
                    wslDistribution = wslDistribution,
                    wslWorkingDirectory = wslWorkingDirectory,
                    useNodeShellWrapper = false,
                ),
            )
        return runProcess(
            command = probeCommand,
            workingDirectory = hostWorkingDirectory,
        )
    }

    private fun runProcess(
        command: List<String>,
        workingDirectory: String,
    ): String? {
        val resolvedWorkingDirectory =
            resolveExistingDirectory(workingDirectory) ?: return null
        return kotlin
            .runCatching {
                val commandLine =
                    GeneralCommandLine(command)
                        .withWorkingDirectory(Path.of(resolvedWorkingDirectory))
                CapturingProcessHandler(commandLine).runProcess(RESUME_PROBE_TIMEOUT_MS)
            }.getOrNull()
            ?.let { output ->
                buildString {
                    append(output.stdout)
                    if (output.stdout.isNotBlank() && output.stderr.isNotBlank()) append('\n')
                    append(output.stderr)
                }
            }
    }

    private fun resolveExistingDirectory(workingDirectory: String): String? {
        val trimmed = workingDirectory.trim()
        if (trimmed.isBlank()) return null
        return if (Files.isDirectory(Path.of(trimmed))) trimmed else null
    }

    private fun containsNoPreviousChats(output: String): Boolean =
        output.contains(NO_PREVIOUS_CHATS_MESSAGE, ignoreCase = true)

    private fun executableName(binaryPath: String): String {
        val fileName = binaryPath.trim().substringAfterLast('/').substringAfterLast('\\')
        return fileName.substringBeforeLast('.').lowercase(Locale.ROOT)
    }
}
