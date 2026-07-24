package com.oaalto.agent.worktree

import com.intellij.openapi.project.Project
import com.oaalto.agent.AgentLaunchContext
import com.oaalto.agent.AgentLaunchResolver
import com.oaalto.agent.ResolvedLaunchInputs
import com.oaalto.agent.settings.AgentSettingsState
import com.oaalto.agent.settings.LaunchMode
import com.oaalto.agent.worktree.resume.AcpResumeStrategy
import com.oaalto.agent.worktree.resume.LaunchResumePlan
import com.oaalto.agent.worktree.resume.PtyResumeStrategy
import com.oaalto.agent.worktree.resume.ResumeContext
import com.oaalto.agent.worktree.resume.ResumeStrategy

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
    ): Result<AgentLaunchContext> {
        val record = worktreePath?.let { stateService.getRecordByPath(it) }
        val resumeContextResult =
            buildResumeContext(
                configuration = configuration,
                worktreeRecord = record,
                worktreePath = worktreePath,
                resume = resume,
                projectBasePath = project.basePath,
            )
        return resumeContextResult.map { resumeContext ->
            val plan = strategyFor(configuration).prepareLaunch(resumeContext)
            toLaunchContext(
                plan = plan,
                worktreePath = worktreePath,
                worktreeId = record?.id,
                resume = resume,
            )
        }
    }

    internal fun buildResumeContext(
        configuration: AgentSettingsState.AgentCliConfiguration,
        worktreeRecord: AgentWorktreeStateService.ManagedWorktreeRecord?,
        worktreePath: String?,
        resume: Boolean,
        projectBasePath: String?,
    ): Result<ResumeContext> {
        val resolvedInputs =
            AgentLaunchResolver.resolveLaunchInputs(
                configuration = configuration,
                projectBasePath = projectBasePath,
                workingDirectoryOverride = worktreePath,
            )
        return resolvedInputs.map { inputs ->
            when (inputs) {
                is ResolvedLaunchInputs.Local ->
                    ResumeContext(
                        configuration = configuration,
                        worktreeRecord = worktreeRecord,
                        workingDirectory = inputs.workingDirectory,
                        resume = resume,
                        wslWorkingDirectory = null,
                        wslDistribution = "",
                        hostWorkingDirectory = inputs.workingDirectory,
                    )
                is ResolvedLaunchInputs.Wsl ->
                    ResumeContext(
                        configuration = configuration,
                        worktreeRecord = worktreeRecord,
                        workingDirectory = inputs.hostWorkingDirectory,
                        resume = resume,
                        wslWorkingDirectory = inputs.linuxPath,
                        wslDistribution = inputs.wslDistribution,
                        hostWorkingDirectory = inputs.hostWorkingDirectory,
                    )
            }
        }
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
}
