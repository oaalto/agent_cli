package com.oaalto.agent.worktree

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.intellij.openapi.ui.Messages
import com.oaalto.agent.AgentCliLog
import com.oaalto.agent.AgentCliSessionContext
import com.oaalto.agent.AgentVirtualFile
import com.oaalto.agent.settings.AgentSettingsState
import com.oaalto.agent.settings.LaunchMode
import com.oaalto.agent.worktree.WorktreeLaunchCoordinator

private val agentCliLog = AgentCliLog.getInstance(AgentPendingLaunchStartupActivity::class.java)

class AgentPendingLaunchStartupActivity : StartupActivity.DumbAware {
    override fun runActivity(project: Project) {
        val basePath = project.basePath ?: return
        val state = AgentWorktreeStateService.getInstance()
        val pendingLaunch = state.consumePendingLaunch(basePath) ?: return

        ApplicationManager.getApplication().invokeLater({
            if (project.isDisposed) return@invokeLater
            val configuration = AgentSettingsState.getInstance().getConfigurationById(pendingLaunch.configurationId)
            if (configuration == null) {
                Messages.showErrorDialog(
                    project,
                    "The selected agent configuration for this worktree no longer exists.",
                    "Run Agent",
                )
                return@invokeLater
            }

            val launchResult =
                WorktreeLaunchCoordinator.buildLaunchContext(
                    project = project,
                    configuration = configuration,
                    worktreePath = pendingLaunch.worktreePath,
                    resume = pendingLaunch.resume,
                )
            if (launchResult.isSuccess) {
                val launchContext = launchResult.getOrThrow()
                FileEditorManager.getInstance(project).openFile(
                    AgentVirtualFile(configuration.id, configuration.name, launchContext),
                    true,
                )
                state.touch(pendingLaunch.worktreePath)
            } else {
                val reason = launchResult.exceptionOrNull()?.message ?: "Failed to resolve launch paths."
                agentCliLog.error(
                    message = "Worktree launch resolution failed: $reason",
                    throwable = launchResult.exceptionOrNull(),
                    context =
                        AgentCliSessionContext(
                            configId = configuration.id,
                            launchMode = LaunchMode.from(configuration.launchMode),
                            worktreePath = pendingLaunch.worktreePath,
                        ),
                )
                Messages.showErrorDialog(
                    project,
                    reason,
                    "Run Agent",
                )
            }
        }, ModalityState.nonModal())
    }
}
