package com.oaalto.agent.settings

import com.intellij.ui.components.JBLabel
import javax.swing.JCheckBox

internal object AgentSettingsDetailPanelSupport {
    fun updateIdeaMcpAvailability(
        ideaMcp: JCheckBox,
        hint: JBLabel,
    ) {
        val available = AiAssistantPresence.default.isAvailable()
        ideaMcp.toolTipText =
            if (available) {
                null
            } else {
                "Install and enable JetBrains AI Assistant or the MCP Server plugin to expose IntelliJ MCP tools."
            }
        hint.isVisible = false
    }

    fun syncDetailPanelFromSelection(
        bindings: DetailPanelBindings,
        setSyncingDetailPanel: (Boolean) -> Unit,
    ) {
        val selected = bindings.table.selectedRow
        if (selected !in 0 until bindings.model.rowCount) {
            setSyncingDetailPanel(true)
            bindings.ideaMcp.isSelected = false
            bindings.customMcp.isSelected = false
            bindings.envModel.setFromMap(emptyMap())
            setSyncingDetailPanel(false)
            return
        }
        val row = bindings.model.rowAt(selected)
        setSyncingDetailPanel(true)
        bindings.ideaMcp.isSelected = row.useIdeaMcp
        bindings.customMcp.isSelected = row.useCustomMcp
        bindings.envModel.setFromMap(row.environmentVariables)
        setSyncingDetailPanel(false)
    }

    fun updateDetailPanelAvailability(bindings: DetailPanelBindings) {
        val selected = bindings.table.selectedRow
        val acpClient =
            selected in 0 until bindings.model.rowCount &&
                LaunchMode.from(bindings.model.rowAt(selected).launchMode) == LaunchMode.ACP_CLIENT
        val ideaPluginsAvailable = AiAssistantPresence.default.isAvailable()
        bindings.ideaMcp.isEnabled = acpClient && ideaPluginsAvailable
        bindings.customMcp.isEnabled = acpClient
        bindings.envTable.isEnabled = acpClient
        bindings.mcpScopeHint.isVisible = !acpClient
        bindings.envHint.isVisible = !acpClient
        bindings.mcpPluginsHint.isVisible = acpClient && !ideaPluginsAvailable
    }

    fun persistDetailPanelToRow(
        bindings: DetailPanelBindings,
        rowIndex: Int,
        syncingDetailPanel: Boolean,
    ) {
        if (syncingDetailPanel || rowIndex !in 0 until bindings.model.rowCount) return
        bindings.model.updateRow(
            rowIndex,
            useIdeaMcp = bindings.ideaMcp.isSelected,
            useCustomMcp = bindings.customMcp.isSelected,
            environmentVariables = bindings.envModel.toMap(),
        )
    }

    fun persistDetailPanelToSelection(
        bindings: DetailPanelBindings,
        syncingDetailPanel: Boolean,
    ) {
        persistDetailPanelToRow(bindings, bindings.table.selectedRow, syncingDetailPanel)
    }
}
