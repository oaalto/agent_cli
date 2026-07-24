package com.oaalto.agent

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.DumbAwareAction
import com.oaalto.agent.acp.AcpAgentEditor
import java.awt.datatransfer.StringSelection

class CopySessionDiagnosticsAction : DumbAwareAction() {
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val editor = FileEditorManager.getInstance(project).selectedEditor as? AcpAgentEditor ?: return
        val text = editor.buildDiagnosticsClipboardText()
        CopyPasteManager.getInstance().setContents(StringSelection(text))
    }

    override fun update(event: AnActionEvent) {
        val project = event.project
        val isAcpEditor =
            project?.let { FileEditorManager.getInstance(it).selectedEditor } is AcpAgentEditor
        event.presentation.isEnabledAndVisible = isAcpEditor
    }
}
