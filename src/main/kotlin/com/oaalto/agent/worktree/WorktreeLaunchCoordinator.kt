package com.oaalto.agent.worktree

import com.intellij.openapi.project.Project
import com.oaalto.agent.AgentLaunchContext
import com.oaalto.agent.WorkingDirectoryResolver
import com.oaalto.agent.WslPathResolver
import com.oaalto.agent.settings.AgentSettingsState
import com.oaalto.agent.settings.LaunchMode
import com.oaalto.agent.worktree.resume.AcpResumeStrategy
import com.oaalto.agent.worktree.resume.LaunchResumePlan
import com.oaalto.agent.worktree.resume.PtyResumeStrategy
import com.oaalto.agent.worktree.resume.ResumeContext
import com.oaalto.agent.worktree.resume.ResumeStrategy
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
    ): String =
        WorkingDirectoryResolver.resolve(
            configuredWorkingDirectory = configuration.workingDirectory,
            overrideWorkingDirectory = worktreePath,
            projectBasePath = projectBasePath,
        )

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
                else -> ""
            }
        val mapped =
            when {
                rawPath.isBlank() -> null
                else -> WslPathResolver.mapToWslPath(rawPath) ?: return null
            }
        val distribution =
            configuration.wslDistribution
                .trim()
                .ifBlank { mapped?.inferredDistribution.orEmpty() }
        return WslPaths(
            linuxPath = mapped?.linuxPath ?: "/home",
            distribution = distribution,
            hostWorkingDirectory = WslPathResolver.resolveHostWorkingDirectory(projectBasePath),
        )
    }

    private data class WslPaths(
        val linuxPath: String,
        val distribution: String,
        val hostWorkingDirectory: String,
    )
}
