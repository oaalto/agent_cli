package com.oaalto.agent

import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorPolicy
import com.intellij.openapi.fileEditor.FileEditorProvider
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

class AgentFileEditorProvider : FileEditorProvider, DumbAware {
    override fun accept(project: Project, file: VirtualFile): Boolean = file is AgentVirtualFile

    override fun createEditor(project: Project, file: VirtualFile): FileEditor {
        require(file is AgentVirtualFile) { "Unsupported file type for Agent editor: ${file.javaClass.name}" }
        return AgentFileEditor(project, file)
    }

    override fun getEditorTypeId(): String = "agent-cli-editor"

    override fun getPolicy(): FileEditorPolicy = FileEditorPolicy.HIDE_DEFAULT_EDITOR
}
