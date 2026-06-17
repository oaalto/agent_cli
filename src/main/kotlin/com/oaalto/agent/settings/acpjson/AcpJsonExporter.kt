package com.oaalto.agent.settings.acpjson

import com.intellij.util.execution.ParametersListUtil
import com.oaalto.agent.acp.AcpLaunchArguments
import com.oaalto.agent.settings.AgentSettingsState
import com.oaalto.agent.settings.LaunchMode
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

object AcpJsonExporter {
    private val json = Json { prettyPrint = true }

    fun export(configurations: List<AgentSettingsState.AgentCliConfiguration>): String {
        val document =
            buildJsonObject {
                putJsonObject("agent_servers") {
                    configurations
                        .filter { LaunchMode.from(it.launchMode) == LaunchMode.ACP_CLIENT }
                        .forEach { configuration ->
                            put(configuration.name, toAgentServerEntry(configuration))
                        }
                }
            }
        return json.encodeToString(JsonObject.serializer(), document)
    }

    private fun toAgentServerEntry(configuration: AgentSettingsState.AgentCliConfiguration): JsonObject {
        val arguments =
            AcpLaunchArguments.stripPtyResumeArguments(ParametersListUtil.parse(configuration.arguments))
        return buildJsonObject {
            put("command", configuration.binaryPath.trim())
            put(
                "args",
                buildJsonArray {
                    arguments.forEach { argument ->
                        add(kotlinx.serialization.json.JsonPrimitive(argument))
                    }
                },
            )
            putOptionalExportFields(configuration)
        }
    }

    private fun kotlinx.serialization.json.JsonObjectBuilder.putOptionalExportFields(
        configuration: AgentSettingsState.AgentCliConfiguration,
    ) {
        if (configuration.environmentVariables.isNotEmpty()) {
            putJsonObject("env") {
                configuration.environmentVariables.forEach { (key, value) -> put(key, value) }
            }
        }
        if (configuration.useIdeaMcp) {
            put("use_idea_mcp", true)
        }
        if (configuration.useCustomMcp) {
            put("use_custom_mcp", true)
        }
        val executionTarget = configuration.executionTarget.trim().uppercase()
        if (executionTarget.isNotBlank() && executionTarget != AgentSettingsState.ExecutionTarget.LOCAL.name) {
            put("execution_target", executionTarget)
        }
        val wslDistribution = configuration.wslDistribution.trim()
        if (wslDistribution.isNotBlank()) {
            put("wsl_distribution", wslDistribution)
        }
        if (configuration.useNodeShellWrapper) {
            put("use_node_shell_wrapper", true)
        }
    }
}
