package com.oaalto.agent.worktree

import com.intellij.openapi.project.Project
import com.oaalto.agent.AgentLaunchContext
import com.oaalto.agent.settings.AgentSettingsState
import com.oaalto.agent.settings.LaunchMode
import com.oaalto.agent.worktree.resume.AcpResumeStrategy
import com.oaalto.agent.worktree.resume.LaunchResumePlan
import com.oaalto.agent.worktree.resume.PtyResumeStrategy
import com.oaalto.agent.worktree.resume.ResumeContext
import com.oaalto.agent.worktree.resume.ResumeStrategy
import java.nio.file.Files
import java.nio.file.Path
import java.util.Locale

object WorktreeLaunchCoordinator {
    fun strategyFor(configuration: AgentSettingsState.AgentCliConfiguration): ResumeStrategy =
        when (LaunchMode.from(configuration.launchMode)) {
            LaunchMode.PTY_PASSTHROUGH -> PtyResumeStrategy
            LaunchMode.ACP_CLIENT -> AcpResumeStrategy
        }

    fun buildLaunchContext(
        project: Project,
        configuration: AgentSettingsState.AgentCliConfiguration,
        worktreePath: String?,
        resume: Boolean,
        stateService: AgentWorktreeStateService = AgentWorktreeStateService.getInstance(),
    ): AgentLaunchContext {
        val record = worktreePath?.let { stateService.getRecordByPath(it) }
        val resumeContext =
            buildResumeContext(
                configuration = configuration,
                worktreeRecord = record,
                worktreePath = worktreePath,
                resume = resume,
                projectBasePath = project.basePath,
            )
        val plan = strategyFor(configuration).prepareLaunch(resumeContext)
        return toLaunchContext(
            plan = plan,
            worktreePath = worktreePath,
            worktreeId = record?.id,
            resume = resume,
        )
    }

    internal fun buildResumeContext(
        configuration: AgentSettingsState.AgentCliConfiguration,
        worktreeRecord: AgentWorktreeStateService.ManagedWorktreeRecord?,
        worktreePath: String?,
        resume: Boolean,
        projectBasePath: String?,
    ): ResumeContext {
        val workingDirectory = resolveWorkingDirectory(projectBasePath, configuration, worktreePath)
        val executionTarget = resolveExecutionTarget(configuration.executionTarget)
        val wslPaths =
            if (executionTarget == AgentSettingsState.ExecutionTarget.WSL) {
                resolveWslPaths(projectBasePath, configuration, worktreePath)
            } else {
                null
            }
        return ResumeContext(
            configuration = configuration,
            worktreeRecord = worktreeRecord,
            workingDirectory = workingDirectory,
            resume = resume,
            wslWorkingDirectory = wslPaths?.linuxPath,
            wslDistribution = wslPaths?.distribution.orEmpty(),
            hostWorkingDirectory = wslPaths?.hostWorkingDirectory ?: workingDirectory,
        )
    }

    private fun toLaunchContext(
        plan: LaunchResumePlan,
        worktreePath: String?,
        worktreeId: String?,
        resume: Boolean,
    ): AgentLaunchContext {
        val additionalArguments =
            when (plan) {
                is LaunchResumePlan.Pty -> plan.extraArgs
                else -> emptyList()
            }
        return AgentLaunchContext(
            workingDirectoryOverride = worktreePath,
            additionalArguments = additionalArguments,
            worktreeId = worktreeId,
            resume = resume,
            resumePlan = plan,
        )
    }

    private fun resolveWorkingDirectory(
        projectBasePath: String?,
        configuration: AgentSettingsState.AgentCliConfiguration,
        worktreePath: String?,
    ): String {
        val override = worktreePath?.trim().orEmpty()
        if (override.isNotBlank()) return override
        val configured = configuration.workingDirectory.trim()
        return when {
            configured.isNotBlank() -> configured
            !projectBasePath.isNullOrBlank() -> projectBasePath
            else -> System.getProperty("user.home")
        }
    }

    private fun resolveExecutionTarget(rawTarget: String): AgentSettingsState.ExecutionTarget {
        val normalized = rawTarget.trim().uppercase(Locale.ROOT)
        return AgentSettingsState.ExecutionTarget.entries.firstOrNull { it.name == normalized }
            ?: AgentSettingsState.ExecutionTarget.LOCAL
    }

    private fun resolveWslPaths(
        projectBasePath: String?,
        configuration: AgentSettingsState.AgentCliConfiguration,
        worktreePath: String?,
    ): WslPaths? {
        val override = worktreePath?.trim().orEmpty()
        val configured = configuration.workingDirectory.trim()
        val rawPath =
            when {
                override.isNotBlank() -> override
                configured.isNotBlank() -> configured
                !projectBasePath.isNullOrBlank() -> projectBasePath
                else ->
                    return WslPaths(
                        linuxPath = "/home",
                        distribution = "",
                        hostWorkingDirectory = resolveHostWorkingDirectory(projectBasePath),
                    )
            }
        val mapped = mapToWslPath(rawPath) ?: return null
        val distribution =
            configuration.wslDistribution
                .trim()
                .ifBlank { mapped.inferredDistribution.orEmpty() }
        return WslPaths(
            linuxPath = mapped.linuxPath,
            distribution = distribution,
            hostWorkingDirectory = resolveHostWorkingDirectory(projectBasePath),
        )
    }

    private fun resolveHostWorkingDirectory(projectBasePath: String?): String {
        val candidates =
            listOf(
                projectBasePath,
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

    private fun mapToWslPath(rawPath: String): MappedWslPath? {
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
                return MappedWslPath(
                    linuxPath = linuxPath,
                    inferredDistribution = inferredDistribution,
                )
            }

        if (trimmed.startsWith("/") || trimmed.startsWith("~")) {
            return MappedWslPath(linuxPath = trimmed, inferredDistribution = null)
        }

        WINDOWS_DRIVE_PATH_REGEX.matchEntire(windowsStylePath)?.let { match ->
            val drive = match.groupValues[1].lowercase(Locale.ROOT)
            val rest = match.groupValues[2].replace('\\', '/').trim('/')
            return MappedWslPath(
                linuxPath = if (rest.isBlank()) "/mnt/$drive" else "/mnt/$drive/$rest",
                inferredDistribution = null,
            )
        }

        if (!windowsStylePath.contains('\\')) {
            return MappedWslPath(linuxPath = trimmed, inferredDistribution = null)
        }
        return null
    }

    private data class MappedWslPath(
        val linuxPath: String,
        val inferredDistribution: String?,
    )

    private data class WslPaths(
        val linuxPath: String,
        val distribution: String,
        val hostWorkingDirectory: String,
    )

    private val UNC_WSL_PREFIXES = listOf("\\\\wsl.localhost\\", "\\\\wsl$\\")
    private val WINDOWS_DRIVE_PATH_REGEX = Regex("""^([A-Za-z]):\\(.*)$""")
}
