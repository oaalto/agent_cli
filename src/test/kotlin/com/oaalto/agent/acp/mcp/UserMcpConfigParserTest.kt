package com.oaalto.agent.acp.mcp

import com.agentclientprotocol.model.McpServer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UserMcpConfigParserTest {
    @Test
    fun `skips stdio servers with blank command`() {
        val servers =
            UserMcpConfigParser.readServersFromJson(
                """
                {
                  "mcpServers": {
                    "valid": { "command": "npx", "args": ["-y", "server"] },
                    "blank": { "command": "" }
                  }
                }
                """.trimIndent(),
            )

        assertEquals(1, servers.size)
        val stdio = servers.single() as McpServer.Stdio
        assertEquals("valid", stdio.name)
        assertEquals("npx", stdio.command)
    }

    @Test
    fun `skips http servers with blank url`() {
        val servers =
            UserMcpConfigParser.readServersFromJson(
                """
                {
                  "mcpServers": {
                    "broken": { "type": "http", "url": "" },
                    "remote": { "type": "http", "url": "https://example.com/mcp" }
                  }
                }
                """.trimIndent(),
            )

        assertEquals(1, servers.size)
        val http = servers.single() as McpServer.Http
        assertEquals("remote", http.name)
        assertEquals("https://example.com/mcp", http.url)
    }

    @Test
    fun `deduplicates servers by key`() {
        val first =
            McpServer.Stdio(
                name = "custom",
                command = "npx",
                args = listOf("-y", "server"),
                env = emptyList(),
            )
        val second =
            McpServer.Stdio(
                name = "custom",
                command = "npx",
                args = listOf("-y", "server"),
                env = emptyList(),
            )

        assertEquals(UserMcpConfigParser.serverKey(first), UserMcpConfigParser.serverKey(second))
        assertTrue(UserMcpConfigParser.serverKey(first).startsWith("stdio:"))
    }
}
