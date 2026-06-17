package com.oaalto.agent.settings

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

object AgentConfigurationSelector {
    fun getSelectedConfiguration(project: Project): AgentSettingsState.AgentCliConfiguration? {
        val settings = AgentSettingsState.getInstance()
        val configurations = settings.getConfigurations()
        if (configurations.isEmpty()) {
            return null
        }

        val projectState = project.service<ProjectAgentSelectionState>()
        val result =
            AgentConfigurationResolution.resolve(
                AgentConfigurationResolutionInput(
                    configurationIds = configurations.map { it.id },
                    defaultConfigurationId = settings.getDefaultConfigurationId(),
                    projectSelectedConfigurationId = projectState.getSelectedConfigurationId(),
                ),
            )
        if (result.persistedProjectSelectionId != projectState.getSelectedConfigurationId()) {
            projectState.setSelectedConfigurationId(result.persistedProjectSelectionId)
        }
        return settings.getConfigurationById(result.configurationId.orEmpty())
    }

    fun setSelectedConfiguration(
        project: Project,
        id: String,
    ): Boolean {
        if (id.isBlank()) {
            return false
        }
        if (AgentSettingsState.getInstance().getConfigurationById(id) == null) {
            return false
        }
        project.service<ProjectAgentSelectionState>().setSelectedConfigurationId(id)
        return true
    }
}
