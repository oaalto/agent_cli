package com.oaalto.agent.acp.mcp

import com.agentclientprotocol.model.McpServer
import com.oaalto.agent.settings.AgentSettingsState
import com.oaalto.agent.settings.LaunchMode

interface McpCapabilityBridge {
    fun resolveServers(configuration: AgentSettingsState.AgentCliConfiguration): List<McpServer>

    fun shouldExposeMcp(configuration: AgentSettingsState.AgentCliConfiguration): Boolean

    companion object {
        fun createDefault(): McpCapabilityBridge =
            DefaultMcpCapabilityBridge(
                ideaMcpServerSource = ReflectiveIdeaMcpServerSource,
                userMcpServerSource = UserMcpConfigServerSource,
            )
    }
}

class DefaultMcpCapabilityBridge(
    private val ideaMcpServerSource: IdeaMcpServerSource,
    private val userMcpServerSource: UserMcpServerSource,
) : McpCapabilityBridge {
    override fun resolveServers(configuration: AgentSettingsState.AgentCliConfiguration): List<McpServer> {
        if (LaunchMode.from(configuration.launchMode) != LaunchMode.ACP_CLIENT) {
            return emptyList()
        }

        val servers = mutableListOf<McpServer>()
        if (configuration.useCustomMcp) {
            servers.addAll(userMcpServerSource.resolve())
        }
        if (configuration.useIdeaMcp) {
            ideaMcpServerSource.resolve()?.let(servers::add)
        }
        return servers
    }

    override fun shouldExposeMcp(configuration: AgentSettingsState.AgentCliConfiguration): Boolean {
        if (LaunchMode.from(configuration.launchMode) != LaunchMode.ACP_CLIENT) {
            return false
        }
        if (!configuration.useIdeaMcp && !configuration.useCustomMcp) {
            return false
        }
        return resolveServers(configuration).isNotEmpty()
    }
}
