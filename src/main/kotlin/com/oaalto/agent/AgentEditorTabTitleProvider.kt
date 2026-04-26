package com.oaalto.agent

import com.intellij.openapi.fileEditor.impl.EditorTabTitleProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

class AgentEditorTabTitleProvider : EditorTabTitleProvider {
    override fun getEditorTabTitle(
        project: Project,
        file: VirtualFile,
    ): String? = if (file is AgentVirtualFile) file.name else null
}
