package com.oaalto.agent.acp

import com.oaalto.agent.AgentCommandBuilder
import com.oaalto.agent.AgentLaunchContext
import com.oaalto.agent.AgentLaunchResolver
import com.oaalto.agent.AgentWslCommandRequest
import com.oaalto.agent.ResolvedLaunchInputs
import com.oaalto.agent.acp.mcp.McpCapabilityBridge
import com.oaalto.agent.settings.AgentSettingsState
import java.nio.file.Files
import java.nio.file.Path

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
        return AgentLaunchResolver
            .resolveLaunchInputs(
                configuration = configuration,
                projectBasePath = request.projectContext.basePath,
                workingDirectoryOverride = request.launchContext.workingDirectoryOverride,
            ).fold(
                onSuccess = { inputs ->
                    when (inputs) {
                        is ResolvedLaunchInputs.Local -> buildLocalPlan(request, inputs)
                        is ResolvedLaunchInputs.Wsl -> buildWslPlan(request, inputs)
                    }
                },
                onFailure = { failure ->
                    Result.failure<AcpLaunchPlan>(failure)
                },
            )
    }

    private fun buildLocalPlan(
        request: AcpLaunchRequest,
        inputs: ResolvedLaunchInputs.Local,
    ): Result<AcpLaunchPlan> {
        if (request.binaryPath.contains("/") && !Files.isExecutable(Path.of(request.binaryPath))) {
            return Result.failure(IllegalStateException("Agent binary is not executable:\n${request.binaryPath}"))
        }

        if (!Files.isDirectory(Path.of(inputs.workingDirectory))) {
            return Result.failure(
                IllegalStateException("Working directory does not exist:\n${inputs.workingDirectory}"),
            )
        }

        return Result.success(
            AcpLaunchPlan(
                command =
                    AgentCommandBuilder.buildLocalCommand(
                        binaryPath = request.binaryPath,
                        arguments = request.arguments,
                        useNodeShellWrapper = request.configuration.useNodeShellWrapper,
                    ),
                processWorkingDirectory = inputs.workingDirectory,
                sessionWorkingDirectory = inputs.workingDirectory,
                environmentVariables = request.environmentVariables,
                mcpServers = request.mcpServers,
                exposeMcp = request.exposeMcp,
            ),
        )
    }

    private fun buildWslPlan(
        request: AcpLaunchRequest,
        inputs: ResolvedLaunchInputs.Wsl,
    ): Result<AcpLaunchPlan> {
        val command =
            AgentCommandBuilder.buildWslCommand(
                AgentWslCommandRequest(
                    binaryPath = request.binaryPath,
                    arguments = request.arguments,
                    wslDistribution = inputs.wslDistribution,
                    wslWorkingDirectory = inputs.linuxPath,
                    useNodeShellWrapper = request.configuration.useNodeShellWrapper,
                    environmentVariables = request.environmentVariables,
                ),
            )
        return Result.success(
            AcpLaunchPlan(
                command = command,
                processWorkingDirectory = inputs.hostWorkingDirectory,
                sessionWorkingDirectory = inputs.linuxPath,
                environmentVariables = emptyMap(),
                mcpServers = request.mcpServers,
                exposeMcp = request.exposeMcp,
            ),
        )
    }
}
