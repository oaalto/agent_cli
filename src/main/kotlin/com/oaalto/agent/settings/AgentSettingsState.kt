package com.oaalto.agent.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import java.util.UUID

@Service(Service.Level.APP)
@State(name = "AgentSettingsState", storages = [Storage("agentSettings.xml")])
class AgentSettingsState : PersistentStateComponent<AgentSettingsState.State> {
    enum class ExecutionTarget {
        LOCAL,
        WSL,
    }

    class State {
        var configurations: MutableList<AgentCliConfiguration> = mutableListOf()
        var selectedConfigurationId: String? = null
        var permissionMemory: MutableMap<String, String> = mutableMapOf()
    }

    class AgentCliConfiguration {
        var id: String = UUID.randomUUID().toString()
        var name: String = "Agent"
        var launchMode: String = LaunchMode.PTY_PASSTHROUGH.name
        var binaryPath: String = ""
        var useNodeShellWrapper: Boolean = false
        var nodeWrapper: String = ""
        var arguments: String = ""
        var workingDirectory: String = ""
        var executionTarget: String = ExecutionTarget.LOCAL.name
        var wslDistribution: String = ""
        var useIdeaMcp: Boolean = false
        var useCustomMcp: Boolean = false
        var environmentVariables: LinkedHashMap<String, String> = linkedMapOf()
    }

    private var state = State()

    init {
        ensureValidState()
    }

    override fun getState(): State = state

    override fun loadState(state: State) {
        this.state = state
        ensureValidState()
    }

    fun getConfigurations(): List<AgentCliConfiguration> = state.configurations.map { it.copyOf() }

    fun getSelectedConfiguration(): AgentCliConfiguration? {
        val id = state.selectedConfigurationId
        val byId = if (id.isNullOrBlank()) null else state.configurations.firstOrNull { it.id == id }
        return (byId ?: state.configurations.firstOrNull())?.copyOf()
    }

    fun getConfigurationById(id: String): AgentCliConfiguration? =
        state.configurations
            .firstOrNull {
                it.id == id
            }?.copyOf()

    fun setSelectedConfiguration(id: String): Boolean {
        if (id.isBlank()) return false
        if (state.configurations.none { it.id == id }) return false
        state.selectedConfigurationId = id
        return true
    }

    fun updateConfigurations(
        configurations: List<AgentCliConfiguration>,
        selectedConfigurationId: String?,
    ) {
        state.configurations = configurations.map { it.copyOf() }.toMutableList()
        state.selectedConfigurationId = selectedConfigurationId
        ensureValidState()
    }

    fun getPermissionMemory(key: String): String? = state.permissionMemory[key]

    fun setPermissionMemory(
        key: String,
        value: String,
    ) {
        state.permissionMemory[key] = value
    }

    fun clearPermissionMemory(key: String) {
        state.permissionMemory.remove(key)
    }

    private fun ensureValidState() {
        state.configurations =
            state.configurations
                .map { sanitize(it) }
                .distinctBy { it.id }
                .toMutableList()

        if (state.configurations.isEmpty()) {
            state.selectedConfigurationId = null
            return
        }

        val selected = state.selectedConfigurationId
        if (selected.isNullOrBlank() || state.configurations.none { it.id == selected }) {
            state.selectedConfigurationId = state.configurations.first().id
        }
    }

    private fun sanitize(configuration: AgentCliConfiguration): AgentCliConfiguration {
        val sanitized = configuration.copyOf()
        if (sanitized.id.isBlank()) {
            sanitized.id = UUID.randomUUID().toString()
        }
        if (sanitized.name.isBlank()) {
            sanitized.name = "Agent"
        }
        sanitized.binaryPath = sanitized.binaryPath.trim()
        if (!sanitized.useNodeShellWrapper && sanitized.nodeWrapper.isNotBlank()) {
            sanitized.useNodeShellWrapper = true
        }
        sanitized.nodeWrapper = ""
        sanitized.arguments = sanitized.arguments.trim()
        sanitized.workingDirectory = sanitized.workingDirectory.trim()
        sanitized.executionTarget = executionTargetOrDefault(sanitized.executionTarget)
        sanitized.launchMode = launchModeOrDefault(sanitized.launchMode)
        sanitized.wslDistribution = sanitized.wslDistribution.trim()
        sanitized.environmentVariables =
            LinkedHashMap(
                sanitized.environmentVariables
                    .mapKeys { it.key.trim() }
                    .filterKeys { it.isNotBlank() }
                    .mapValues { it.value },
            )
        return sanitized
    }

    private fun executionTargetOrDefault(value: String): String =
        value.trim().uppercase().takeIf { raw ->
            ExecutionTarget.entries.any { it.name == raw }
        } ?: ExecutionTarget.LOCAL.name

    private fun launchModeOrDefault(value: String): String = LaunchMode.from(value).name

    private fun AgentCliConfiguration.copyOf(): AgentCliConfiguration =
        AgentCliConfiguration().also {
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

    companion object {
        fun getInstance(): AgentSettingsState = service()
    }
}
