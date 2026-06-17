package com.oaalto.agent.acp

import com.oaalto.agent.AgentCommandBuilder
import com.oaalto.agent.AgentLaunchContext
import com.oaalto.agent.AgentWslCommandRequest
import com.oaalto.agent.WorkingDirectoryResolver
import com.oaalto.agent.WslPathResolver
import com.oaalto.agent.acp.mcp.McpCapabilityBridge
import com.oaalto.agent.settings.AgentSettingsState
import java.nio.file.Files
import java.nio.file.Path
import java.util.Locale

object AcpProcessLauncher {
    fun buildLaunchPlan(
        projectContext: AgentProjectContext,
        configuration: AgentSettingsState.AgentCliConfiguration,
        launchContext: AgentLaunchContext,
        mcpCapabilityBridge: McpCapabilityBridge = McpCapabilityBridge.createDefault(),
    ): Result<AcpLaunchPlan> {
        val binaryPath = configuration.binaryPath.trim()
        if (binaryPath.isBlank()) {
            return Result.failure(
                IllegalStateException("Agent binary path is empty for configuration '${configuration.name}'."),
            )
        }

        val request =
            AcpLaunchRequest(
                binaryPath = binaryPath,
                arguments =
                    AcpLaunchArguments.resolve(
                        configuration = configuration,
                        launchContext = launchContext,
                    ),
                configuration = configuration,
                launchContext = launchContext,
                projectContext = projectContext,
                environmentVariables = LinkedHashMap(configuration.environmentVariables),
                mcpServers = mcpCapabilityBridge.resolveServers(configuration),
                exposeMcp = mcpCapabilityBridge.shouldExposeMcp(configuration),
            )
        return when (resolveExecutionTarget(configuration.executionTarget)) {
            AgentSettingsState.ExecutionTarget.LOCAL -> buildLocalPlan(request)
            AgentSettingsState.ExecutionTarget.WSL -> buildWslPlan(request)
        }
    }

    private fun buildLocalPlan(request: AcpLaunchRequest): Result<AcpLaunchPlan> {
        if (request.binaryPath.contains("/") && !Files.isExecutable(Path.of(request.binaryPath))) {
            return Result.failure(IllegalStateException("Agent binary is not executable:\n${request.binaryPath}"))
        }

        val workingDirectory =
            WorkingDirectoryResolver.resolve(
                configuredWorkingDirectory = request.configuration.workingDirectory,
                overrideWorkingDirectory = request.launchContext.workingDirectoryOverride,
                projectBasePath = request.projectContext.basePath,
            )
        return when {
            !Files.isDirectory(Path.of(workingDirectory)) ->
                Result.failure(IllegalStateException("Working directory does not exist:\n$workingDirectory"))
            else ->
                Result.success(
                    AcpLaunchPlan(
                        command =
                            AgentCommandBuilder.buildLocalCommand(
                                binaryPath = request.binaryPath,
                                arguments = request.arguments,
                                useNodeShellWrapper = request.configuration.useNodeShellWrapper,
                            ),
                        processWorkingDirectory = workingDirectory,
                        sessionWorkingDirectory = workingDirectory,
                        environmentVariables = request.environmentVariables,
                        mcpServers = request.mcpServers,
                        exposeMcp = request.exposeMcp,
                    ),
                )
        }
    }

    private fun buildWslPlan(request: AcpLaunchRequest): Result<AcpLaunchPlan> {
        val resolvedWslWorkingDirectory =
            WslPathResolver.resolveWslWorkingDirectory(
                configuredWorkingDirectory = request.configuration.workingDirectory,
                overrideWorkingDirectory = request.launchContext.workingDirectoryOverride,
                projectBasePath = request.projectContext.basePath,
            )
        val overrideValue =
            request.launchContext.workingDirectoryOverride
                ?.trim()
                .orEmpty()
        val configured = request.configuration.workingDirectory.trim()
        val basePath =
            request.projectContext.basePath
                ?.trim()
                .orEmpty()
        val rawPath =
            when {
                overrideValue.isNotBlank() -> overrideValue
                configured.isNotBlank() -> configured
                basePath.isNotBlank() -> basePath
                else -> ""
            }
        if (rawPath.isNotBlank() && WslPathResolver.mapToWslPath(rawPath) == null) {
            return Result.failure(
                IllegalStateException(
                    "Working directory could not be mapped to a WSL path:\n${request.configuration.workingDirectory}",
                ),
            )
        }

        val effectiveDistribution =
            request.configuration.wslDistribution
                .trim()
                .ifBlank { resolvedWslWorkingDirectory.inferredDistribution.orEmpty() }
        val hostWorkingDirectory = WslPathResolver.resolveHostWorkingDirectory(request.projectContext.basePath)
        val command =
            AgentCommandBuilder.buildWslCommand(
                AgentWslCommandRequest(
                    binaryPath = request.binaryPath,
                    arguments = request.arguments,
                    wslDistribution = effectiveDistribution,
                    wslWorkingDirectory = resolvedWslWorkingDirectory.linuxPath,
                    useNodeShellWrapper = request.configuration.useNodeShellWrapper,
                    environmentVariables = request.environmentVariables,
                ),
            )
        return Result.success(
            AcpLaunchPlan(
                command = command,
                processWorkingDirectory = hostWorkingDirectory,
                sessionWorkingDirectory = resolvedWslWorkingDirectory.linuxPath,
                environmentVariables = emptyMap(),
                mcpServers = request.mcpServers,
                exposeMcp = request.exposeMcp,
            ),
        )
    }

    private fun resolveExecutionTarget(rawTarget: String): AgentSettingsState.ExecutionTarget {
        val normalized = rawTarget.trim().uppercase(Locale.ROOT)
        return AgentSettingsState.ExecutionTarget.entries.firstOrNull { it.name == normalized }
            ?: AgentSettingsState.ExecutionTarget.LOCAL
    }
}
