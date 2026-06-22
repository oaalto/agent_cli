package com.oaalto.agent.settings

import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.extensions.PluginId

fun interface AiAssistantPresence {
    fun isAvailable(): Boolean

    companion object {
        val MCP_SERVER_PLUGIN_ID: PluginId = PluginId.getId("com.intellij.mcpServer")
        val AI_ASSISTANT_PLUGIN_ID: PluginId = PluginId.getId("com.intellij.ml.llm")

        fun fromPluginProbe(probe: (PluginId) -> Boolean): AiAssistantPresence =
            AiAssistantPresence {
                probe(MCP_SERVER_PLUGIN_ID) || probe(AI_ASSISTANT_PLUGIN_ID)
            }

        val default: AiAssistantPresence =
            fromPluginProbe { pluginId ->
                PluginManagerCore.getPluginSet().findEnabledPlugin(pluginId) != null
            }
    }
}
