package com.oaalto.agent

import com.intellij.openapi.util.registry.Registry

internal object AgentCliRegistry {
    const val LOG_KEY = "agent_cli.log"
    const val DEBUG_KEY = "agent_cli.debug"

    fun readBoolean(key: String): Boolean = runCatching { Registry.get(key).asBoolean() }.getOrDefault(false)
}
