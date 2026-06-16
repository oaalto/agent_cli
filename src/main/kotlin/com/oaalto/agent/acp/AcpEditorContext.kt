package com.oaalto.agent.acp

import com.intellij.openapi.project.Project
import com.oaalto.agent.AgentLaunchContext
import com.oaalto.agent.acp.auth.AuthPromptUi
import com.oaalto.agent.acp.permission.PermissionCoordinator
import com.oaalto.agent.acp.permission.PermissionMemoryStore
import com.oaalto.agent.acp.permission.PermissionPromptUi
import com.oaalto.agent.acp.terminal.TerminalSessionRegistry
import com.oaalto.agent.acp.ui.ShellPaneHost
import java.nio.file.Path

data class AcpEditorContext(
    val project: Project,
    val configurationId: String,
    val launchContext: AgentLaunchContext,
    val scopeRoot: Path,
    val listener: AcpSessionListener,
    val shellPaneHost: ShellPaneHost,
    val permissionPromptUi: PermissionPromptUi,
    val authPromptUi: AuthPromptUi,
    val permissionMemoryStore: PermissionMemoryStore = PermissionMemoryStore(),
    val terminalSessionRegistry: TerminalSessionRegistry = TerminalSessionRegistry(),
) {
    fun permissionCoordinator(): PermissionCoordinator =
        PermissionCoordinator(
            configurationId = configurationId,
            memoryStore = permissionMemoryStore,
            promptUi = permissionPromptUi,
        )
}
