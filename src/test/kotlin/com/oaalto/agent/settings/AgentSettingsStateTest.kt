package com.oaalto.agent.settings

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AgentSettingsStateTest {
    @Test
    fun `legacy configuration without launchMode defaults to PTY_PASSTHROUGH`() {
        val state = AgentSettingsState()
        val legacy =
            AgentSettingsState.AgentCliConfiguration().apply {
                id = "legacy-id"
                name = "Legacy Agent"
                binaryPath = "/usr/bin/agent"
                launchMode = ""
            }
        state.loadState(
            AgentSettingsState.State().apply {
                configurations = mutableListOf(legacy)
                selectedConfigurationId = legacy.id
            },
        )

        assertEquals(LaunchMode.PTY_PASSTHROUGH.name, state.getConfigurations().single().launchMode)
    }

    @Test
    fun `launch mode survives save and load round-trip`() {
        val state = AgentSettingsState()
        val configuration =
            AgentSettingsState.AgentCliConfiguration().apply {
                id = "acp-id"
                name = "ACP Agent"
                binaryPath = "/usr/bin/agent"
                launchMode = LaunchMode.ACP_CLIENT.name
            }
        state.loadState(
            AgentSettingsState.State().apply {
                configurations = mutableListOf(configuration)
                selectedConfigurationId = configuration.id
            },
        )

        val persisted = state.getState()
        val reloaded = AgentSettingsState()
        reloaded.loadState(persisted)

        assertEquals(LaunchMode.ACP_CLIENT.name, reloaded.getConfigurations().single().launchMode)
    }

    @Test
    fun `new configuration defaults to PTY_PASSTHROUGH`() {
        val configuration = AgentSettingsState.AgentCliConfiguration()
        assertEquals(LaunchMode.PTY_PASSTHROUGH.name, configuration.launchMode)
    }

    @Test
    fun `mcp toggles and environment variables default off and empty`() {
        val configuration = AgentSettingsState.AgentCliConfiguration()
        assertFalse(configuration.useIdeaMcp)
        assertFalse(configuration.useCustomMcp)
        assertTrue(configuration.environmentVariables.isEmpty())
    }

    @Test
    fun `mcp toggles and environment variables round-trip through state`() {
        val state = AgentSettingsState()
        val configuration =
            AgentSettingsState.AgentCliConfiguration().apply {
                id = "acp-id"
                name = "ACP Agent"
                binaryPath = "/usr/bin/agent"
                launchMode = LaunchMode.ACP_CLIENT.name
                useIdeaMcp = true
                useCustomMcp = true
                environmentVariables = linkedMapOf("API_KEY" to "secret")
            }
        state.loadState(
            AgentSettingsState.State().apply {
                configurations = mutableListOf(configuration)
                selectedConfigurationId = configuration.id
            },
        )

        val persisted = state.getState()
        val reloaded = AgentSettingsState()
        reloaded.loadState(persisted)
        val loaded = reloaded.getConfigurations().single()

        assertTrue(loaded.useIdeaMcp)
        assertTrue(loaded.useCustomMcp)
        assertEquals(mapOf("API_KEY" to "secret"), loaded.environmentVariables)
    }
}
