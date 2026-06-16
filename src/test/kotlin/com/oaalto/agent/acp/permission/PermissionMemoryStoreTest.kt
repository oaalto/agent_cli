package com.oaalto.agent.acp.permission

import com.agentclientprotocol.model.ToolKind
import com.oaalto.agent.settings.AgentSettingsState
import kotlin.test.Test
import kotlin.test.assertEquals

class PermissionMemoryStoreTest {
    @Test
    fun `persists allow always across store instances`() {
        val settings = AgentSettingsState()
        val store = PermissionMemoryStore(settings)
        store.set("config-1", ToolKind.EDIT, PermissionMemoryOutcome.ALLOW_ALWAYS)

        val reloaded = PermissionMemoryStore(settings)
        assertEquals(PermissionMemoryOutcome.ALLOW_ALWAYS, reloaded.get("config-1", ToolKind.EDIT))
    }
}
