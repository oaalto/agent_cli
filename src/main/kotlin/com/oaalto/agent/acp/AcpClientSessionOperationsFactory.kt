package com.oaalto.agent.acp

import com.agentclientprotocol.common.ClientSessionOperations
import com.oaalto.agent.acp.filesystem.IdeScopedFileSystemAccess
import com.oaalto.agent.acp.filesystem.ScopedFileSystemOperations
import com.oaalto.agent.acp.filesystem.SessionFilesystemOperationsImpl

/**
 * Visible composition root for all `ClientSessionOperations` dependencies.
 *
 * Replaces the inline companion `create()` that used to construct all
 * filesystem and permission dependencies inside `AcpClientSessionOperationsImpl`.
 * Reviewers can see what is wired when a session opens by reading this file.
 */
interface AcpClientSessionOperationsFactory {
    fun create(context: AcpEditorContext): ClientSessionOperations
}

class AcpDefaultClientSessionOperationsFactory(
    private val project: com.intellij.openapi.project.Project,
) : AcpClientSessionOperationsFactory {
    override fun create(context: AcpEditorContext): ClientSessionOperations {
        val scoped =
            ScopedFileSystemOperations.create(
                scopeRoot = context.scopeRoot,
                projectBasePath = project.basePath,
            )
        val vfs = IdeScopedFileSystemAccess(project)
        val permission = context.permissionCoordinator()
        val deep = SessionFilesystemOperationsImpl(scoped, vfs, permission)
        return AcpClientSessionOperationsImpl(
            editorContext = context,
            sessionFilesystemOperations = deep,
            permissionCoordinator = permission,
            terminalSessionRegistry = context.terminalSessionRegistry,
        )
    }
}
