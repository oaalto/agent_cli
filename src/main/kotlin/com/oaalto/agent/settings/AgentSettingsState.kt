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
    class State {
        var configurations: MutableList<AgentCliConfiguration> = mutableListOf()
        var selectedConfigurationId: String? = null
    }

    class AgentCliConfiguration {
        var id: String = UUID.randomUUID().toString()
        var name: String = "Agent"
        var binaryPath: String = ""
        var arguments: String = ""
        var workingDirectory: String = ""
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
        state.configurations.firstOrNull { it.id == id }?.copyOf()

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

    private fun ensureValidState() {
        state.configurations = state.configurations
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
        sanitized.arguments = sanitized.arguments.trim()
        sanitized.workingDirectory = sanitized.workingDirectory.trim()
        return sanitized
    }

    private fun AgentCliConfiguration.copyOf(): AgentCliConfiguration = AgentCliConfiguration().also {
        it.id = id
        it.name = name
        it.binaryPath = binaryPath
        it.arguments = arguments
        it.workingDirectory = workingDirectory
    }

    companion object {
        fun getInstance(): AgentSettingsState = service()
    }
}
