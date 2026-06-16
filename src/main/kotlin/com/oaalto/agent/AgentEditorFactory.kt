package com.oaalto.agent

import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.project.Project
import com.oaalto.agent.acp.AcpAgentEditorStub
import com.oaalto.agent.pty.PtyAgentEditor
import com.oaalto.agent.settings.AgentSettingsState
import com.oaalto.agent.settings.LaunchMode

object AgentEditorFactory {
    fun editorTypeForLaunchMode(launchMode: LaunchMode): Class<out FileEditor> =
        when (launchMode) {
            LaunchMode.PTY_PASSTHROUGH -> PtyAgentEditor::class.java
            LaunchMode.ACP_CLIENT -> AcpAgentEditorStub::class.java
        }

    fun createEditor(
        project: Project,
        file: AgentVirtualFile,
    ): FileEditor {
        val configuration = AgentSettingsState.getInstance().getConfigurationById(file.configurationId)
        val launchMode = configuration?.let { LaunchMode.from(it.launchMode) } ?: LaunchMode.PTY_PASSTHROUGH
        return when (launchMode) {
            LaunchMode.PTY_PASSTHROUGH -> PtyAgentEditor(project, file)
            LaunchMode.ACP_CLIENT -> AcpAgentEditorStub(project, file)
        }
    }
}
