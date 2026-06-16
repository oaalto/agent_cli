package com.oaalto.agent.acp

import com.oaalto.agent.AgentCommandBuilder
import com.oaalto.agent.AgentLaunchContext
import com.oaalto.agent.settings.AgentSettingsState
import java.nio.file.Files
import java.nio.file.Path
import java.util.Locale

object AcpProcessLauncher {
    fun buildLaunchPlan(
        projectContext: AgentProjectContext,
        configuration: AgentSettingsState.AgentCliConfiguration,
        launchContext: AgentLaunchContext,
    ): Result<AcpLaunchPlan> {
        val binaryPath = configuration.binaryPath.trim()
        if (binaryPath.isBlank()) {
            return Result.failure(IllegalStateException("Agent binary path is empty for configuration '${configuration.name}'."))
        }

        val effectiveArguments =
            AcpLaunchArguments.resolve(
                configuration = configuration,
                launchContext = launchContext,
            )

        val executionTarget = resolveExecutionTarget(configuration.executionTarget)
        return when (executionTarget) {
            AgentSettingsState.ExecutionTarget.LOCAL ->
                buildLocalPlan(
                    binaryPath,
                    effectiveArguments,
                    configuration,
                    launchContext,
                    projectContext,
                )
            AgentSettingsState.ExecutionTarget.WSL ->
                buildWslPlan(
                    binaryPath,
                    effectiveArguments,
                    configuration,
                    launchContext,
                    projectContext,
                )
        }
    }

    private fun buildLocalPlan(
        binaryPath: String,
        arguments: List<String>,
        configuration: AgentSettingsState.AgentCliConfiguration,
        launchContext: AgentLaunchContext,
        projectContext: AgentProjectContext,
    ): Result<AcpLaunchPlan> {
        if (binaryPath.contains("/") && !Files.isExecutable(Path.of(binaryPath))) {
            return Result.failure(IllegalStateException("Agent binary is not executable:\n$binaryPath"))
        }

        val workingDirectory =
            resolveWorkingDirectory(
                configuredWorkingDirectory = configuration.workingDirectory,
                overrideWorkingDirectory = launchContext.workingDirectoryOverride,
                project = projectContext,
            )
        if (!Files.isDirectory(Path.of(workingDirectory))) {
            return Result.failure(IllegalStateException("Working directory does not exist:\n$workingDirectory"))
        }

        val command =
            AgentCommandBuilder.buildLocalCommand(
                binaryPath = binaryPath,
                arguments = arguments,
                useNodeShellWrapper = configuration.useNodeShellWrapper,
            )
        return Result.success(
            AcpLaunchPlan(
                command = command,
                processWorkingDirectory = workingDirectory,
                sessionWorkingDirectory = workingDirectory,
            ),
        )
    }

    private fun buildWslPlan(
        binaryPath: String,
        arguments: List<String>,
        configuration: AgentSettingsState.AgentCliConfiguration,
        launchContext: AgentLaunchContext,
        projectContext: AgentProjectContext,
    ): Result<AcpLaunchPlan> {
        val resolvedWslWorkingDirectory =
            resolveWslWorkingDirectory(
                configuredWorkingDirectory = configuration.workingDirectory,
                overrideWorkingDirectory = launchContext.workingDirectoryOverride,
                project = projectContext,
            ) ?: return Result.failure(
                IllegalStateException(
                    "Working directory could not be mapped to a WSL path:\n${configuration.workingDirectory}",
                ),
            )

        val effectiveDistribution =
            configuration.wslDistribution
                .trim()
                .ifBlank { resolvedWslWorkingDirectory.inferredDistribution.orEmpty() }
        val hostWorkingDirectory = resolveHostWorkingDirectory(projectContext)
        val command =
            AgentCommandBuilder.buildWslCommand(
                binaryPath = binaryPath,
                arguments = arguments,
                wslDistribution = effectiveDistribution,
                wslWorkingDirectory = resolvedWslWorkingDirectory.linuxPath,
                useNodeShellWrapper = configuration.useNodeShellWrapper,
            )
        return Result.success(
            AcpLaunchPlan(
                command = command,
                processWorkingDirectory = hostWorkingDirectory,
                sessionWorkingDirectory = resolvedWslWorkingDirectory.linuxPath,
            ),
        )
    }

    private fun resolveWorkingDirectory(
        configuredWorkingDirectory: String,
        overrideWorkingDirectory: String?,
        project: AgentProjectContext,
    ): String {
        val overrideValue = overrideWorkingDirectory?.trim().orEmpty()
        if (overrideValue.isNotBlank()) {
            return overrideValue
        }
        val configured = configuredWorkingDirectory.trim()
        return when {
            configured.isNotBlank() -> configured
            !project.basePath.isNullOrBlank() -> project.basePath!!
            else -> System.getProperty("user.home")
        }
    }

    private fun resolveExecutionTarget(rawTarget: String): AgentSettingsState.ExecutionTarget {
        val normalized = rawTarget.trim().uppercase(Locale.ROOT)
        return AgentSettingsState.ExecutionTarget.entries.firstOrNull { it.name == normalized }
            ?: AgentSettingsState.ExecutionTarget.LOCAL
    }

    private fun resolveWslWorkingDirectory(
        configuredWorkingDirectory: String,
        overrideWorkingDirectory: String?,
        project: AgentProjectContext,
    ): WslWorkingDirectory? {
        val overrideValue = overrideWorkingDirectory?.trim().orEmpty()
        if (overrideValue.isNotBlank()) {
            return mapToWslPath(overrideValue)
        }
        val configured = configuredWorkingDirectory.trim()
        if (configured.isNotBlank()) {
            return mapToWslPath(configured)
        }

        val basePath = project.basePath?.trim().orEmpty()
        if (basePath.isNotBlank()) {
            return mapToWslPath(basePath)
        }
        return WslWorkingDirectory(linuxPath = "/home", inferredDistribution = null)
    }

    private fun mapToWslPath(rawPath: String): WslWorkingDirectory? {
        val trimmed = rawPath.trim()
        if (trimmed.isBlank()) return null
        val windowsStylePath = trimmed.replace('/', '\\')
        UNC_WSL_PREFIXES
            .firstOrNull { prefix ->
                windowsStylePath.startsWith(prefix, ignoreCase = true)
            }?.let { prefix ->
                val withoutPrefix = windowsStylePath.substring(prefix.length)
                val segments = withoutPrefix.split('\\').filter { it.isNotBlank() }
                if (segments.isEmpty()) return null
                val inferredDistribution = segments.first()
                val linuxSegments = segments.drop(1)
                val linuxPath = if (linuxSegments.isEmpty()) "/" else "/" + linuxSegments.joinToString("/")
                return WslWorkingDirectory(
                    linuxPath = linuxPath,
                    inferredDistribution = inferredDistribution,
                )
            }

        if (trimmed.startsWith("/") || trimmed.startsWith("~")) {
            return WslWorkingDirectory(linuxPath = trimmed, inferredDistribution = null)
        }

        WINDOWS_DRIVE_PATH_REGEX.matchEntire(windowsStylePath)?.let { match ->
            val drive = match.groupValues[1].lowercase(Locale.ROOT)
            val rest = match.groupValues[2].replace('\\', '/').trim('/')
            return WslWorkingDirectory(
                linuxPath = if (rest.isBlank()) "/mnt/$drive" else "/mnt/$drive/$rest",
                inferredDistribution = null,
            )
        }

        if (!windowsStylePath.contains('\\')) {
            return WslWorkingDirectory(linuxPath = trimmed, inferredDistribution = null)
        }
        return null
    }

    private fun resolveHostWorkingDirectory(project: AgentProjectContext): String {
        val candidates =
            listOf(
                project.basePath,
                System.getProperty("user.home"),
                System.getProperty("java.io.tmpdir"),
            )
        return candidates
            .asSequence()
            .mapNotNull { it?.trim()?.takeIf(String::isNotBlank) }
            .firstOrNull { path ->
                kotlin.runCatching { Files.isDirectory(Path.of(path)) }.getOrDefault(false)
            }
            ?: System.getProperty("user.home")
    }

    private val UNC_WSL_PREFIXES = listOf("\\\\wsl.localhost\\", "\\\\wsl$\\")
    private val WINDOWS_DRIVE_PATH_REGEX = Regex("""^([A-Za-z]):\\(.*)$""")

    private data class WslWorkingDirectory(
        val linuxPath: String,
        val inferredDistribution: String?,
    )
}
