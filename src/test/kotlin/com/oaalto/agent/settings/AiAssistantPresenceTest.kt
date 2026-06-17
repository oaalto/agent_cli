package com.oaalto.agent.settings

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AiAssistantPresenceTest {
    @Test
    fun `custom presence probe can be injected in tests`() {
        val available = AiAssistantPresence { true }
        val unavailable = AiAssistantPresence { false }

        assertTrue(available.isAvailable())
        assertFalse(unavailable.isAvailable())
    }

    @Test
    fun `enables idea mcp when either plugin is present`() {
        val bothEnabled = AiAssistantPresence.fromPluginProbe { true }
        val onlyMcpServer =
            AiAssistantPresence.fromPluginProbe { pluginId ->
                pluginId == AiAssistantPresence.MCP_SERVER_PLUGIN_ID
            }
        val onlyAiAssistant =
            AiAssistantPresence.fromPluginProbe { pluginId ->
                pluginId == AiAssistantPresence.AI_ASSISTANT_PLUGIN_ID
            }
        val neither =
            AiAssistantPresence.fromPluginProbe { false }

        assertTrue(bothEnabled.isAvailable())
        assertTrue(onlyMcpServer.isAvailable())
        assertTrue(onlyAiAssistant.isAvailable())
        assertFalse(neither.isAvailable())
    }
}
