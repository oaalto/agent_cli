package com.oaalto.agent.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import java.util.Locale
import java.util.UUID

@Service(Service.Level.APP)
@State(name = "AgentSettingsState", storages = [Storage("agentSettings.xml")])
class AgentSettingsState : PersistentStateComponent<AgentSettingsState.State> {
    enum class ExecutionTarget {
        LOCAL,
        WSL,
        ;

        companion object {
            fun from(raw: String): ExecutionTarget {
                val normalized = raw.trim().uppercase(Locale.ROOT)
                return entries.firstOrNull { it.name == normalized } ?: LOCAL
            }
        }
    }

    class State {
        var configurations: MutableList<AgentCliConfiguration> = mutableListOf()
        var defaultConfigurationId: String? = null
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
        AgentSettingsStateSupport.ensureValidState(state)
    }

    override fun getState(): State {
        state.selectedConfigurationId = null
        return state
    }

    override fun loadState(state: State) {
        if (state.defaultConfigurationId.isNullOrBlank() && !state.selectedConfigurationId.isNullOrBlank()) {
            state.defaultConfigurationId = state.selectedConfigurationId
        }
        state.selectedConfigurationId = null
        this.state = state
        AgentSettingsStateSupport.ensureValidState(this.state)
    }

    fun getConfigurations(): List<AgentCliConfiguration> = state.configurations.map { it.copyOf() }

    fun getDefaultConfigurationId(): String? = state.defaultConfigurationId

    fun getDefaultConfiguration(): AgentCliConfiguration? {
        val id = state.defaultConfigurationId
        val byId = if (id.isNullOrBlank()) null else state.configurations.firstOrNull { it.id == id }
        return (byId ?: state.configurations.firstOrNull())?.copyOf()
    }

    fun getConfigurationById(id: String): AgentCliConfiguration? =
        state.configurations
            .firstOrNull {
                it.id == id
            }?.copyOf()

    fun updateConfigurations(
        configurations: List<AgentCliConfiguration>,
        defaultConfigurationId: String?,
    ) {
        state.configurations = configurations.map { it.copyOf() }.toMutableList()
        state.defaultConfigurationId = defaultConfigurationId
        AgentSettingsStateSupport.ensureValidState(state)
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

    companion object {
        fun getInstance(): AgentSettingsState = service()
    }
}
