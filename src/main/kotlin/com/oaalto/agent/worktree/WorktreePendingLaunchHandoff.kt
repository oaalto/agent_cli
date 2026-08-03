package com.oaalto.agent.worktree

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.oaalto.agent.AgentCliLog
import com.oaalto.agent.AgentCliSessionContext
import com.oaalto.agent.AgentLaunchContext
import com.oaalto.agent.AgentVirtualFile
import com.oaalto.agent.settings.AgentSettingsState
import com.oaalto.agent.settings.LaunchMode

private val handoffLog = AgentCliLog.getInstance(WorktreePendingLaunchHandoff::class.java)

internal data class ScheduleLaunchSeams(
    val stateService: AgentWorktreeStateService,
    val openWorktreeProject: (String) -> Result<Unit>,
)

internal data class CompleteLaunchSeams(
    val stateService: AgentWorktreeStateService,
    val getConfiguration: (String) -> AgentSettingsState.AgentCliConfiguration?,
    val buildLaunchContext: (
        Project,
        AgentSettingsState.AgentCliConfiguration,
        String,
        Boolean,
    ) -> Result<AgentLaunchContext>,
    val openEditor: (Project, AgentSettingsState.AgentCliConfiguration, AgentLaunchContext) -> Unit,
    val showError: (Project, String) -> Unit,
    val runOnEdt: (Runnable) -> Unit,
)

object WorktreePendingLaunchHandoff {
    fun scheduleLaunch(
        originatingProject: Project,
        worktreePath: String,
        configurationId: String,
        configurationName: String,
        resume: Boolean,
    ): Result<Unit> =
        scheduleLaunch(
            worktreePath = worktreePath,
            configurationId = configurationId,
            configurationName = configurationName,
            resume = resume,
            seams =
                ScheduleLaunchSeams(
                    stateService = AgentWorktreeStateService.getInstance(),
                    openWorktreeProject = { path ->
                        AgentWorktreeService(originatingProject).openWorktreeProject(path)
                    },
                ),
        )

    internal fun scheduleLaunch(
        worktreePath: String,
        configurationId: String,
        configurationName: String,
        resume: Boolean,
        seams: ScheduleLaunchSeams,
    ): Result<Unit> {
        seams.stateService.enqueuePendingLaunch(
            worktreePath = worktreePath,
            configurationId = configurationId,
            configurationName = configurationName,
            resume = resume,
        )
        val openResult = seams.openWorktreeProject(worktreePath)
        if (openResult.isFailure) {
            seams.stateService.consumePendingLaunch(worktreePath)
        }
        return openResult
    }

    fun completePendingLaunchIfAny(project: Project) {
        completePendingLaunchIfAny(
            project = project,
            seams =
                CompleteLaunchSeams(
                    stateService = AgentWorktreeStateService.getInstance(),
                    getConfiguration = { configurationId ->
                        AgentSettingsState.getInstance().getConfigurationById(configurationId)
                    },
                    buildLaunchContext = { launchProject, configuration, pendingWorktreePath, resume ->
                        WorktreeLaunchCoordinator.buildLaunchContext(
                            project = launchProject,
                            configuration = configuration,
                            worktreePath = pendingWorktreePath,
                            resume = resume,
                            stateService = AgentWorktreeStateService.getInstance(),
                        )
                    },
                    openEditor = { editorProject, configuration, launchContext ->
                        FileEditorManager.getInstance(editorProject).openFile(
                            AgentVirtualFile(configuration.id, configuration.name, launchContext),
                            true,
                        )
                    },
                    showError = { errorProject, message ->
                        Messages.showErrorDialog(errorProject, message, "Run Agent")
                    },
                    runOnEdt = { runnable ->
                        ApplicationManager.getApplication().invokeLater(runnable, ModalityState.nonModal())
                    },
                ),
        )
    }

    internal fun completePendingLaunchIfAny(
        project: Project,
        seams: CompleteLaunchSeams,
    ) {
        val basePath = project.basePath ?: return
        val pendingLaunch = seams.stateService.consumePendingLaunch(basePath) ?: return

        seams.runOnEdt {
            if (project.isDisposed) return@runOnEdt
            val configuration = seams.getConfiguration(pendingLaunch.configurationId)
            if (configuration == null) {
                seams.showError(
                    project,
                    "The selected agent configuration for this worktree no longer exists.",
                )
                return@runOnEdt
            }

            val launchResult =
                seams.buildLaunchContext(
                    project,
                    configuration,
                    pendingLaunch.worktreePath,
                    pendingLaunch.resume,
                )
            if (launchResult.isSuccess) {
                seams.openEditor(project, configuration, launchResult.getOrThrow())
                seams.stateService.touch(pendingLaunch.worktreePath)
            } else {
                val reason = launchResult.exceptionOrNull()?.message ?: "Failed to resolve launch paths."
                handoffLog.error(
                    message = "Worktree launch resolution failed: $reason",
                    throwable = launchResult.exceptionOrNull(),
                    context =
                        AgentCliSessionContext(
                            configId = configuration.id,
                            launchMode = LaunchMode.from(configuration.launchMode),
                            worktreePath = pendingLaunch.worktreePath,
                        ),
                )
                seams.showError(project, reason)
            }
        }
    }
}
