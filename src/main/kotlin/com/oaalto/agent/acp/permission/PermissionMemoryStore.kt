package com.oaalto.agent.acp.permission

import com.agentclientprotocol.model.ToolKind
import com.oaalto.agent.settings.AgentSettingsState

class PermissionMemoryStore(
    private val settings: AgentSettingsState = AgentSettingsState.getInstance(),
) {
    fun memoryKey(
        configurationId: String,
        toolKind: ToolKind,
    ): String = "$configurationId:${toolKind.name.lowercase()}"

    fun get(
        configurationId: String,
        toolKind: ToolKind,
    ): PermissionMemoryOutcome? =
        PermissionMemoryOutcome.fromRaw(
            settings.getPermissionMemory(memoryKey(configurationId, toolKind)),
        )

    fun set(
        configurationId: String,
        toolKind: ToolKind,
        outcome: PermissionMemoryOutcome,
    ) {
        settings.setPermissionMemory(memoryKey(configurationId, toolKind), outcome.name)
    }

    fun clear(
        configurationId: String,
        toolKind: ToolKind,
    ) {
        settings.clearPermissionMemory(memoryKey(configurationId, toolKind))
    }
}
