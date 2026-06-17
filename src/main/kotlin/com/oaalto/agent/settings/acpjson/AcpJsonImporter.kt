package com.oaalto.agent.settings.acpjson

import com.oaalto.agent.settings.AgentSettingsState
import com.oaalto.agent.settings.LaunchMode
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

data class AcpJsonImportResult(
    val configurations: List<AgentSettingsState.AgentCliConfiguration>,
)

data class AcpJsonImportDraft(
    val name: String,
    val configuration: AgentSettingsState.AgentCliConfiguration,
)

object AcpJsonImporter {
    private val json = Json { ignoreUnknownKeys = true }

    fun parse(text: String): Result<List<AcpJsonImportDraft>> {
        if (text.isBlank()) {
            return Result.failure(IllegalArgumentException("acp.json is empty."))
        }
        return runCatching {
            val root = json.parseToJsonElement(text).jsonObject
            val agentServers =
                root["agent_servers"]?.jsonObject
                    ?: throw IllegalArgumentException("acp.json is missing the required 'agent_servers' object.")
            agentServers.map { (name, value) ->
                val entry = value.jsonObject
                AcpJsonImportDraft(
                    name = name.trim(),
                    configuration = toConfiguration(name, entry),
                )
            }
        }
    }

    fun merge(
        drafts: List<AcpJsonImportDraft>,
        existing: List<AgentSettingsState.AgentCliConfiguration>,
        overwriteNames: Set<String>,
    ): AcpJsonImportResult {
        val merged = existing.map { it.copyOf() }.toMutableList()
        drafts.forEach { draft ->
            val key = draft.name.lowercase()
            val currentIndex = merged.indexOfFirst { it.name.trim().lowercase() == key }
            if (currentIndex >= 0) {
                if (!overwriteNames.contains(draft.name)) {
                    return@forEach
                }
                val preservedId = merged[currentIndex].id
                merged[currentIndex] =
                    draft.configuration.copyOf().apply {
                        id = preservedId
                        name = draft.name
                    }
            } else {
                merged.add(
                    draft.configuration.copyOf().apply {
                        name = draft.name
                    },
                )
            }
        }
        return AcpJsonImportResult(configurations = merged)
    }

    private fun toConfiguration(
        name: String,
        entry: JsonObject,
    ): AgentSettingsState.AgentCliConfiguration {
        val command =
            entry["command"]?.jsonPrimitive?.content?.trim()
                ?: throw IllegalArgumentException("Agent '$name' is missing 'command'.")
        val argumentValues =
            entry["args"]?.jsonArray?.map { element -> element.jsonPrimitive.content }.orEmpty()
        val env =
            entry["env"]?.jsonObject?.mapValues { it.value.jsonPrimitive.content }.orEmpty()

        return AgentSettingsState.AgentCliConfiguration().apply {
            this.name = name
            launchMode = LaunchMode.ACP_CLIENT.name
            binaryPath = command
            arguments = argumentValues.joinToString(" ")
            useIdeaMcp = entry["use_idea_mcp"]?.jsonPrimitive?.booleanOrNull == true
            useCustomMcp = entry["use_custom_mcp"]?.jsonPrimitive?.booleanOrNull == true
            environmentVariables = LinkedHashMap(env)
            executionTarget =
                entry["execution_target"]
                    ?.jsonPrimitive
                    ?.content
                    ?.trim()
                    ?.uppercase()
                    ?: AgentSettingsState.ExecutionTarget.LOCAL.name
            wslDistribution =
                entry["wsl_distribution"]
                    ?.jsonPrimitive
                    ?.content
                    ?.trim()
                    .orEmpty()
            useNodeShellWrapper = entry["use_node_shell_wrapper"]?.jsonPrimitive?.booleanOrNull == true
        }
    }
}

private fun AgentSettingsState.AgentCliConfiguration.copyOf(): AgentSettingsState.AgentCliConfiguration =
    AgentSettingsState.AgentCliConfiguration().also {
        it.id = id
        it.name = name
        it.launchMode = launchMode
        it.binaryPath = binaryPath
        it.useNodeShellWrapper = useNodeShellWrapper
        it.nodeWrapper = nodeWrapper
        it.arguments = arguments
        it.workingDirectory = workingDirectory
        it.executionTarget = executionTarget
        it.wslDistribution = wslDistribution
        it.useIdeaMcp = useIdeaMcp
        it.useCustomMcp = useCustomMcp
        it.environmentVariables = LinkedHashMap(environmentVariables)
    }
