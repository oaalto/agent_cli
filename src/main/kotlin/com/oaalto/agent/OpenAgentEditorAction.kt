package com.oaalto.agent

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.Messages
import com.oaalto.agent.settings.AgentConfigurationSelector
import com.oaalto.agent.settings.AgentSettingsConfigurable

class OpenAgentEditorAction : DumbAwareAction() {
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val configuration = AgentConfigurationSelector.getSelectedConfiguration(project)
        when {
            configuration == null -> {
                Messages.showErrorDialog(project, "No agent configuration is available.", "Run Agent")
                ShowSettingsUtil.getInstance().showSettingsDialog(project, AgentSettingsConfigurable::class.java)
            }
            configuration.binaryPath.isBlank() -> {
                Messages.showErrorDialog(
                    project,
                    "The default agent configuration has an empty binary path.",
                    "Run Agent",
                )
                ShowSettingsUtil.getInstance().showSettingsDialog(project, AgentSettingsConfigurable::class.java)
            }
            else ->
                FileEditorManager.getInstance(project).openFile(
                    AgentVirtualFile(configuration.id, configuration.name),
                    true,
                )
        }
    }

    override fun update(event: AnActionEvent) {
        val hasProject = event.getData(CommonDataKeys.PROJECT) != null
        event.presentation.isEnabledAndVisible = hasProject
        event.presentation.text = templatePresentation.text
    }
}
