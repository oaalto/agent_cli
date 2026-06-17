package com.oaalto.agent.acp

import com.agentclientprotocol.model.McpServer
import com.oaalto.agent.AgentLaunchContext
import com.oaalto.agent.settings.AgentSettingsState

internal data class AcpLaunchRequest(
    val binaryPath: String,
    val arguments: List<String>,
    val configuration: AgentSettingsState.AgentCliConfiguration,
    val launchContext: AgentLaunchContext,
    val projectContext: AgentProjectContext,
    val environmentVariables: Map<String, String>,
    val mcpServers: List<McpServer>,
    val exposeMcp: Boolean,
)
