package com.oaalto.agent.acp.mcp

import com.agentclientprotocol.model.EnvVariable
import com.agentclientprotocol.model.McpServer
import com.oaalto.agent.settings.AgentSettingsState
import com.oaalto.agent.settings.LaunchMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class McpCapabilityBridgeTest {
    @Test
    fun `resolves user and idea servers only for acp client mode`() {
        val ideaServer = McpServer.Stdio("jetbrains", "java", emptyList(), emptyList())
        val customServer = McpServer.Stdio("custom", "npx", listOf("-y", "server"), emptyList())
        val bridge =
            DefaultMcpCapabilityBridge(
                ideaMcpServerSource = IdeaMcpServerSource { ideaServer },
                userMcpServerSource = UserMcpServerSource { listOf(customServer) },
            )
        val configuration =
            AgentSettingsState.AgentCliConfiguration().apply {
                launchMode = LaunchMode.ACP_CLIENT.name
                useIdeaMcp = true
                useCustomMcp = true
            }

        val servers = bridge.resolveServers(configuration)

        assertEquals(2, servers.size)
        assertTrue(bridge.shouldExposeMcp(configuration))
    }

    @Test
    fun `pty passthrough ignores mcp toggles`() {
        val bridge =
            DefaultMcpCapabilityBridge(
                ideaMcpServerSource = IdeaMcpServerSource { error("should not be called") },
                userMcpServerSource = UserMcpServerSource { error("should not be called") },
            )
        val configuration =
            AgentSettingsState.AgentCliConfiguration().apply {
                launchMode = LaunchMode.PTY_PASSTHROUGH.name
                useIdeaMcp = true
                useCustomMcp = true
            }

        assertTrue(bridge.resolveServers(configuration).isEmpty())
        assertFalse(bridge.shouldExposeMcp(configuration))
    }

    @Test
    fun `does not expose mcp when toggles are off`() {
        val ideaServer = McpServer.Stdio("jetbrains", "java", emptyList(), emptyList())
        val bridge =
            DefaultMcpCapabilityBridge(
                ideaMcpServerSource = IdeaMcpServerSource { ideaServer },
                userMcpServerSource = UserMcpServerSource { emptyList() },
            )
        val configuration =
            AgentSettingsState.AgentCliConfiguration().apply {
                launchMode = LaunchMode.ACP_CLIENT.name
            }

        assertTrue(bridge.resolveServers(configuration).isEmpty())
        assertFalse(bridge.shouldExposeMcp(configuration))
    }

    @Test
    fun `includes only enabled server sources`() {
        val ideaServer = McpServer.Stdio("jetbrains", "java", emptyList(), emptyList())
        val customServer =
            McpServer.Stdio(
                "custom",
                "npx",
                listOf("-y", "server"),
                listOf(EnvVariable("TOKEN", "abc")),
            )
        val bridge =
            DefaultMcpCapabilityBridge(
                ideaMcpServerSource = IdeaMcpServerSource { ideaServer },
                userMcpServerSource = UserMcpServerSource { listOf(customServer) },
            )
        val configuration =
            AgentSettingsState.AgentCliConfiguration().apply {
                launchMode = LaunchMode.ACP_CLIENT.name
                useCustomMcp = true
            }

        val servers = bridge.resolveServers(configuration)

        assertEquals(1, servers.size)
        val stdio = servers.single() as McpServer.Stdio
        assertEquals("custom", stdio.name)
        assertEquals("npx", stdio.command)
    }
}
