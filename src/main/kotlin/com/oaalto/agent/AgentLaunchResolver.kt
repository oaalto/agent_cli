package com.oaalto.agent

import com.oaalto.agent.settings.AgentSettingsState
import java.nio.file.Files
import java.nio.file.Path

object AgentLaunchResolver {
    fun resolveLaunchInputs(
        configuration: AgentSettingsState.AgentCliConfiguration,
        projectBasePath: String?,
        workingDirectoryOverride: String?,
    ): Result<ResolvedLaunchInputs> {
        val executionTarget = AgentSettingsState.ExecutionTarget.from(configuration.executionTarget)
        return when (executionTarget) {
            AgentSettingsState.ExecutionTarget.LOCAL ->
                resolveLocal(
                    configuredWorkingDirectory = configuration.workingDirectory,
                    overrideWorkingDirectory = workingDirectoryOverride,
                    projectBasePath = projectBasePath,
                )
            AgentSettingsState.ExecutionTarget.WSL ->
                resolveWsl(
                    configuration = configuration,
                    projectBasePath = projectBasePath,
                    workingDirectoryOverride = workingDirectoryOverride,
                )
        }
    }

    private fun resolveLocal(
        configuredWorkingDirectory: String,
        overrideWorkingDirectory: String?,
        projectBasePath: String?,
    ): Result<ResolvedLaunchInputs.Local> {
        val workingDirectory =
            WorkingDirectoryResolver.resolve(
                configuredWorkingDirectory = configuredWorkingDirectory,
                overrideWorkingDirectory = overrideWorkingDirectory,
                projectBasePath = projectBasePath,
            )
        return when {
            !Files.isDirectory(Path.of(workingDirectory)) ->
                Result.failure(
                    IllegalStateException("Working directory does not exist:\n$workingDirectory"),
                )
            else -> Result.success(ResolvedLaunchInputs.Local(workingDirectory = workingDirectory))
        }
    }

    private fun resolveWsl(
        configuration: AgentSettingsState.AgentCliConfiguration,
        projectBasePath: String?,
        workingDirectoryOverride: String?,
    ): Result<ResolvedLaunchInputs.Wsl> {
        val overrideValue = workingDirectoryOverride?.trim().orEmpty()
        val configured = configuration.workingDirectory.trim()
        val basePath = projectBasePath?.trim().orEmpty()
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
                    "Working directory could not be mapped to a WSL path:\n" +
                        "$rawPath\n\n" +
                        "Use one of:\n" +
                        "- Linux path (for example /home/user/project)\n" +
                        "- WSL UNC path (for example \\\\wsl.localhost\\Ubuntu\\home\\user\\project)\n" +
                        "- Windows drive path (for example D:\\project)",
                ),
            )
        }

        val resolvedWslWorkingDirectory =
            WslPathResolver.resolveWslWorkingDirectory(
                configuredWorkingDirectory = configuration.workingDirectory,
                overrideWorkingDirectory = workingDirectoryOverride,
                projectBasePath = projectBasePath,
            )
        val effectiveDistribution =
            configuration.wslDistribution.trim().ifBlank { resolvedWslWorkingDirectory.inferredDistribution.orEmpty() }
        val hostWorkingDirectory = WslPathResolver.resolveHostWorkingDirectory(projectBasePath)
        return Result.success(
            ResolvedLaunchInputs.Wsl(
                workingDirectory = hostWorkingDirectory,
                linuxPath = resolvedWslWorkingDirectory.linuxPath,
                wslDistribution = effectiveDistribution,
                hostWorkingDirectory = hostWorkingDirectory,
            ),
        )
    }
}

sealed interface ResolvedLaunchInputs {
    val executionTarget: AgentSettingsState.ExecutionTarget
    val workingDirectory: String

    data class Local(
        override val workingDirectory: String,
    ) : ResolvedLaunchInputs {
        override val executionTarget: AgentSettingsState.ExecutionTarget = AgentSettingsState.ExecutionTarget.LOCAL
    }

    data class Wsl(
        override val workingDirectory: String,
        val linuxPath: String,
        val wslDistribution: String,
        val hostWorkingDirectory: String,
    ) : ResolvedLaunchInputs {
        override val executionTarget: AgentSettingsState.ExecutionTarget = AgentSettingsState.ExecutionTarget.WSL
    }
}
