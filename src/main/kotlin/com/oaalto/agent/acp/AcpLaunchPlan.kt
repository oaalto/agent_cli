package com.oaalto.agent.acp

import com.agentclientprotocol.model.McpServer

data class AcpLaunchPlan(
    val command: List<String>,
    val processWorkingDirectory: String,
    val sessionWorkingDirectory: String,
    val environmentVariables: Map<String, String> = emptyMap(),
    val mcpServers: List<McpServer> = emptyList(),
    val exposeMcp: Boolean = false,
) {
    fun sessionMcpServers(): List<McpServer> = if (exposeMcp) mcpServers else emptyList()
}
