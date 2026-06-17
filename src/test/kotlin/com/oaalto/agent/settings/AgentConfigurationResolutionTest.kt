package com.oaalto.agent.settings

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AgentConfigurationResolutionTest {
    @Test
    fun `empty catalog resolves to null`() {
        val result =
            AgentConfigurationResolution.resolve(
                AgentConfigurationResolutionInput(
                    configurationIds = emptyList(),
                    defaultConfigurationId = "default-id",
                    projectSelectedConfigurationId = null,
                ),
            )

        assertNull(result.configurationId)
        assertNull(result.persistedProjectSelectionId)
    }

    @Test
    fun `unset project selection seeds from default`() {
        val result =
            AgentConfigurationResolution.resolve(
                AgentConfigurationResolutionInput(
                    configurationIds = listOf("cursor-id", "pi-id"),
                    defaultConfigurationId = "pi-id",
                    projectSelectedConfigurationId = null,
                ),
            )

        assertEquals("pi-id", result.configurationId)
        assertEquals("pi-id", result.persistedProjectSelectionId)
    }

    @Test
    fun `unset project selection falls back to first config when default is invalid`() {
        val result =
            AgentConfigurationResolution.resolve(
                AgentConfigurationResolutionInput(
                    configurationIds = listOf("cursor-id", "pi-id"),
                    defaultConfigurationId = "missing-id",
                    projectSelectedConfigurationId = null,
                ),
            )

        assertEquals("cursor-id", result.configurationId)
        assertEquals("cursor-id", result.persistedProjectSelectionId)
    }

    @Test
    fun `valid project selection is preserved`() {
        val result =
            AgentConfigurationResolution.resolve(
                AgentConfigurationResolutionInput(
                    configurationIds = listOf("cursor-id", "pi-id"),
                    defaultConfigurationId = "pi-id",
                    projectSelectedConfigurationId = "cursor-id",
                ),
            )

        assertEquals("cursor-id", result.configurationId)
        assertEquals("cursor-id", result.persistedProjectSelectionId)
    }

    @Test
    fun `stale project selection rewrites to default`() {
        val result =
            AgentConfigurationResolution.resolve(
                AgentConfigurationResolutionInput(
                    configurationIds = listOf("cursor-id", "pi-id"),
                    defaultConfigurationId = "pi-id",
                    projectSelectedConfigurationId = "deleted-id",
                ),
            )

        assertEquals("pi-id", result.configurationId)
        assertEquals("pi-id", result.persistedProjectSelectionId)
    }
}
