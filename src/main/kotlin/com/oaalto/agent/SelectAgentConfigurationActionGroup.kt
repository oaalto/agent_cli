package com.oaalto.agent

import com.intellij.ide.ActivityTracker
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.DumbAwareToggleAction
import com.oaalto.agent.settings.AgentConfigurationSelector
import com.oaalto.agent.settings.AgentSettingsConfigurable
import com.oaalto.agent.settings.AgentSettingsState

class SelectAgentConfigurationActionGroup :
    ActionGroup(),
    DumbAware {
    override fun getChildren(event: AnActionEvent?): Array<AnAction> {
        val project = event?.project
        val settings = AgentSettingsState.getInstance()
        val configurations = settings.getConfigurations()
        val actions =
            configurations
                .map { configuration ->
                    object : DumbAwareToggleAction(configuration.name) {
                        override fun isSelected(event: AnActionEvent): Boolean {
                            val actionProject = event.project ?: return false
                            val selected = AgentConfigurationSelector.getSelectedConfiguration(actionProject)
                            return selected?.id == configuration.id
                        }

                        override fun setSelected(
                            event: AnActionEvent,
                            state: Boolean,
                        ) {
                            if (!state) {
                                return
                            }
                            val actionProject = event.project
                            if (actionProject == null) {
                                logger.warn(
                                    "Failed to select agent configuration '${configuration.id}' " +
                                        "from toolbar action: no project.",
                                )
                                return
                            }
                            val selected =
                                AgentConfigurationSelector.setSelectedConfiguration(
                                    actionProject,
                                    configuration.id,
                                )
                            if (!selected) {
                                logger.warn(
                                    "Failed to select agent configuration " +
                                        "'${configuration.id}' from toolbar action.",
                                )
                                return
                            }
                            ActivityTracker.getInstance().inc()
                        }
                    }
                }.toMutableList<AnAction>()

        if (actions.isEmpty()) {
            actions.add(
                object : DumbAwareAction("No agents configured") {
                    override fun actionPerformed(event: AnActionEvent) = Unit

                    override fun update(event: AnActionEvent) {
                        event.presentation.isEnabled = false
                    }
                },
            )
        }

        actions.add(
            object : DumbAwareAction("Manage Agents...") {
                override fun actionPerformed(event: AnActionEvent) {
                    ShowSettingsUtil
                        .getInstance()
                        .showSettingsDialog(project, AgentSettingsConfigurable::class.java)
                }
            },
        )

        return actions.toTypedArray()
    }

    override fun update(event: AnActionEvent) {
        val project = event.project
        val selected = project?.let { AgentConfigurationSelector.getSelectedConfiguration(it) }
        event.presentation.text = selected?.name ?: "Select Agent"
        event.presentation.description = "Choose the agent used by Run Agent"
        event.presentation.isEnabledAndVisible = project != null
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun displayTextInToolbar(): Boolean = true

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    companion object {
        private val logger = Logger.getInstance(SelectAgentConfigurationActionGroup::class.java)
    }
}
