package com.oaalto.agent.settings

import com.intellij.ui.components.JBLabel
import com.intellij.ui.table.JBTable
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JPanel

internal data class DetailPanelHints(
    val mcpScopeHint: JBLabel,
    val mcpPluginsHint: JBLabel,
    val envHint: JBLabel,
)

internal data class ObservationHelpPanel(
    val panel: JPanel,
    val hasBasePath: Boolean,
)

internal data class EnvironmentVariablesEditor(
    val model: EnvironmentVariablesTableModel,
    val table: JBTable,
    val panel: JComponent,
)

internal data class DetailPanelBindings(
    val model: AgentConfigsTableModel,
    val table: JBTable,
    val ideaMcp: JCheckBox,
    val customMcp: JCheckBox,
    val envTable: JBTable,
    val envModel: EnvironmentVariablesTableModel,
    val mcpScopeHint: JBLabel,
    val mcpPluginsHint: JBLabel,
    val envHint: JBLabel,
)
