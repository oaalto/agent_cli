package com.oaalto.agent.worktree

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.intellij.openapi.ui.Messages
import com.oaalto.agent.AgentLaunchContext
import com.oaalto.agent.AgentVirtualFile
import com.oaalto.agent.settings.AgentSettingsState

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

            val launchContext =
                AgentLaunchContext(
                    workingDirectoryOverride = pendingLaunch.worktreePath,
                    additionalArguments =
                        if (pendingLaunch.resume) {
                            AgentWorktreeService.resumeArgumentsForConfiguration(configuration).orEmpty()
                        } else {
                            emptyList()
                        },
                    worktreeId = state.getRecordByPath(pendingLaunch.worktreePath)?.id,
                )
            FileEditorManager.getInstance(project).openFile(
                AgentVirtualFile(configuration.id, configuration.name, launchContext),
                true,
            )
            state.touch(pendingLaunch.worktreePath)
        }, ModalityState.nonModal())
    }
}
