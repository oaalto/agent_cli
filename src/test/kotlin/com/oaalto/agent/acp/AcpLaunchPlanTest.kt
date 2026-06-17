package com.oaalto.agent.acp

import com.agentclientprotocol.model.McpServer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AcpLaunchPlanTest {
    @Test
    fun `session mcp servers are empty when expose mcp is false`() {
        val plan =
            AcpLaunchPlan(
                command = listOf("agent"),
                processWorkingDirectory = "/tmp",
                sessionWorkingDirectory = "/tmp",
                mcpServers =
                    listOf(
                        McpServer.Stdio("custom", "npx", emptyList(), emptyList()),
                    ),
                exposeMcp = false,
            )

        assertTrue(plan.sessionMcpServers().isEmpty())
    }

    @Test
    fun `session mcp servers pass through when expose mcp is true`() {
        val servers =
            listOf(
                McpServer.Stdio("custom", "npx", emptyList(), emptyList()),
            )
        val plan =
            AcpLaunchPlan(
                command = listOf("agent"),
                processWorkingDirectory = "/tmp",
                sessionWorkingDirectory = "/tmp",
                mcpServers = servers,
                exposeMcp = true,
            )

        assertEquals(servers, plan.sessionMcpServers())
    }
}
