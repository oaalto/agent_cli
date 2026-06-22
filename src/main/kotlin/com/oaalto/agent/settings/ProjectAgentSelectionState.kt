package com.oaalto.agent.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
@State(name = "AgentProjectSelectionState", storages = [Storage(StoragePathMacros.WORKSPACE_FILE)])
class ProjectAgentSelectionState(
    project: Project,
) : PersistentStateComponent<ProjectAgentSelectionState.State> {
    // Project is retained as service context; basePath is used for diagnostic logging
    private val projectBasePath: String? = project.basePath

    class State {
        var selectedConfigurationId: String? = null
    }

    private var state = State()

    override fun getState(): State = state

    override fun loadState(state: State) {
        this.state = state
    }

    fun getSelectedConfigurationId(): String? = state.selectedConfigurationId?.takeIf { it.isNotBlank() }

    fun setSelectedConfigurationId(id: String?) {
        state.selectedConfigurationId = id?.takeIf { it.isNotBlank() }
    }

    /** Returns the project base path for diagnostic purposes. */
    fun getProjectBasePath(): String? = projectBasePath
}
