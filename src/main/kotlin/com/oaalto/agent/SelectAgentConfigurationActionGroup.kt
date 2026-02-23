package com.oaalto.agent

import com.oaalto.agent.settings.AgentSettingsConfigurable
import com.oaalto.agent.settings.AgentSettingsState
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.DumbAwareToggleAction

class SelectAgentConfigurationActionGroup : ActionGroup(), DumbAware {
    override fun getChildren(event: AnActionEvent?): Array<AnAction> {
        val settings = AgentSettingsState.getInstance()
        val configurations = settings.getConfigurations()
        val actions = configurations.map { configuration ->
            object : DumbAwareToggleAction(configuration.name) {
                override fun isSelected(event: AnActionEvent): Boolean {
                    return settings.getSelectedConfiguration()?.id == configuration.id
                }

                override fun setSelected(event: AnActionEvent, state: Boolean) {
                    if (state) {
                        settings.setSelectedConfiguration(configuration.id)
                    }
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
                    ShowSettingsUtil.getInstance()
                        .showSettingsDialog(event.project, AgentSettingsConfigurable::class.java)
                }
            },
        )

        return actions.toTypedArray()
    }

    override fun update(event: AnActionEvent) {
        val settings = AgentSettingsState.getInstance()
        val selected = settings.getSelectedConfiguration()
        event.presentation.text = selected?.name ?: "Select Agent"
        event.presentation.description = "Choose the agent used by Run Agent"
        event.presentation.isEnabledAndVisible = event.project != null
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun displayTextInToolbar(): Boolean = true

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
}
