package com.oaalto.agent

import com.oaalto.agent.acp.AcpLaunchPlan
import com.oaalto.agent.settings.LaunchMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AgentCliLogRedactionTest {
    @Test
    fun `redacts known secret env keys`() {
        val env =
            mapOf(
                "API_KEY" to "secret-value",
                "AUTH_TOKEN" to "token-value",
                "PLAIN" to "visible",
            )
        val redacted = AgentCliLogRedaction.redactEnvironmentVariables(env)

        assertEquals(AgentCliLogRedaction.REDACTED, redacted["API_KEY"])
        assertEquals(AgentCliLogRedaction.REDACTED, redacted["AUTH_TOKEN"])
        assertEquals("visible", redacted["PLAIN"])
    }

    @Test
    fun `debug env string never contains raw secret values`() {
        val formatted =
            AgentCliLogRedaction.formatEnvironmentVariablesForDebug(
                mapOf(
                    "PASSWORD" to "hunter2",
                    "HOME" to "/home/user",
                ),
            )

        assertFalse(formatted.contains("hunter2"))
        assertTrue(formatted.contains("HOME=/home/user"))
        assertTrue(formatted.contains(AgentCliLogRedaction.REDACTED))
    }

    @Test
    fun `launch plan debug string redacts secret env values`() {
        val formatted =
            AgentCliLogRedaction.formatLaunchPlanForDebug(
                AcpLaunchPlan(
                    command = listOf("agent"),
                    processWorkingDirectory = "/repo",
                    sessionWorkingDirectory = "/repo",
                    environmentVariables =
                        mapOf(
                            "TOKEN" to "abc123",
                            "PATH" to "/bin",
                        ),
                ),
            )

        assertFalse(formatted.contains("abc123"))
        assertTrue(formatted.contains("PATH=/bin"))
        assertTrue(formatted.contains(AgentCliLogRedaction.REDACTED))
    }

    @Test
    fun `settings failure message redacts secret key values`() {
        val message = "Invalid env API_KEY=secret-token for agent foo"
        val redacted = AgentCliLogRedaction.redactSettingsFailureMessage(message)

        assertFalse(redacted.contains("secret-token"))
        assertTrue(redacted.contains(AgentCliLogRedaction.REDACTED))
    }

    @Test
    fun `auth token redaction replaces non-blank values`() {
        assertEquals(AgentCliLogRedaction.REDACTED, AgentCliLogRedaction.redactAuthToken("bearer-secret"))
        assertEquals("", AgentCliLogRedaction.redactAuthToken(""))
    }
}

class AgentCliSessionContextTest {
    @Test
    fun `context prefix omits unknown fields`() {
        assertNull(formatAgentCliSessionContextPrefix(null))
        assertNull(formatAgentCliSessionContextPrefix(AgentCliSessionContext()))
        assertEquals(
            "[configId=cfg-1 sessionId=sess-1]",
            formatAgentCliSessionContextPrefix(
                AgentCliSessionContext(
                    configId = "cfg-1",
                    sessionId = "sess-1",
                ),
            ),
        )
    }

    @Test
    fun `formatted message prefixes available context`() {
        val message =
            formatAgentCliLogMessage(
                message = "session opened",
                context =
                    AgentCliSessionContext(
                        configId = "cfg-a",
                        launchMode = LaunchMode.ACP_CLIENT,
                        worktreePath = "/repo/wt",
                    ),
            )

        assertTrue(message.startsWith("[configId=cfg-a launchMode=ACP_CLIENT worktreePath=/repo/wt]"))
        assertTrue(message.endsWith("session opened"))
    }

    @Test
    fun `virtual file maps to session context`() {
        val file =
            AgentVirtualFile(
                configurationId = "cfg-1",
                configurationName = "Test",
                launchContext =
                    com.oaalto.agent.AgentLaunchContext(
                        workingDirectoryOverride = "/repo/wt",
                    ),
            )

        val context = file.toAgentCliSessionContext(LaunchMode.PTY_PASSTHROUGH)

        assertEquals("cfg-1", context.configId)
        assertEquals(LaunchMode.PTY_PASSTHROUGH, context.launchMode)
        assertEquals("/repo/wt", context.worktreePath)
    }
}
