package com.oaalto.agent

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.Messages
import com.oaalto.agent.settings.AgentSettingsConfigurable
import com.oaalto.agent.settings.AgentSettingsState

class OpenAgentEditorAction : DumbAwareAction() {
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val settings = AgentSettingsState.getInstance()
        val configuration = settings.getSelectedConfiguration()
        if (configuration == null) {
            Messages.showErrorDialog(project, "No agent configuration is available.", "Run Agent")
            ShowSettingsUtil.getInstance().showSettingsDialog(project, AgentSettingsConfigurable::class.java)
            return
        }
        if (configuration.binaryPath.isBlank()) {
            Messages.showErrorDialog(project, "The default agent configuration has an empty binary path.", "Run Agent")
            ShowSettingsUtil.getInstance().showSettingsDialog(project, AgentSettingsConfigurable::class.java)
            return
        }

        val editorManager = FileEditorManager.getInstance(project)
        editorManager.openFile(AgentVirtualFile(configuration.id, configuration.name), true)
    }

    override fun update(event: AnActionEvent) {
        val hasProject = event.getData(CommonDataKeys.PROJECT) != null
        event.presentation.isEnabledAndVisible = hasProject
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun displayTextInToolbar(): Boolean = true
}
