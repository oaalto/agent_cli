package com.oaalto.agent.settings

import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.components.JBLabel
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.JBUI
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.util.UUID
import javax.swing.DefaultCellEditor
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.ListSelectionModel

internal object AgentSettingsUiFactory {
    fun createConfigurationTable(model: AgentConfigsTableModel): JBTable =
        JBTable(model).apply {
            selectionModel.selectionMode = ListSelectionModel.SINGLE_SELECTION
            fillsViewportHeight = true
            columnModel.getColumn(2).cellEditor =
                DefaultCellEditor(
                    ComboBox(
                        LaunchMode.displayLabels().toTypedArray(),
                    ),
                )
            columnModel.getColumn(3).cellEditor =
                DefaultCellEditor(
                    ComboBox(
                        AgentSettingsState.ExecutionTarget.entries
                            .map { it.name }
                            .toTypedArray(),
                    ),
                )
        }

    fun createEnvironmentVariablesEditor(): EnvironmentVariablesEditor {
        val envModel = EnvironmentVariablesTableModel()
        val envTable =
            JBTable(envModel).apply {
                selectionModel.selectionMode = ListSelectionModel.SINGLE_SELECTION
                fillsViewportHeight = true
                rowHeight = JBUI.scale(22)
            }
        val envToolbar =
            ToolbarDecorator
                .createDecorator(envTable)
                .setAddAction { _ ->
                    envModel.addRow("", "")
                    val index = envModel.rowCount - 1
                    if (index >= 0) {
                        envTable.selectionModel.setSelectionInterval(index, index)
                    }
                }.setRemoveAction { _ ->
                    val selected = envTable.selectedRow
                    if (selected >= 0) {
                        envModel.removeRow(selected)
                    }
                }
        return EnvironmentVariablesEditor(
            model = envModel,
            table = envTable,
            panel = envToolbar.createPanel(),
        )
    }

    fun createDetailPanelHints(): DetailPanelHints {
        val mcpScopeHint =
            JBLabel("MCP toggles apply only when Launch Mode is ACP. PTY Passthrough ignores them.").apply {
                foreground = JBUI.CurrentTheme.Label.disabledForeground()
            }
        val mcpPluginsHint =
            JBLabel(
                "IntelliJ MCP requires JetBrains AI Assistant or the MCP Server plugin (Tools | MCP Server).",
            ).apply {
                foreground = JBUI.CurrentTheme.Label.disabledForeground()
            }
        val envHint =
            JBLabel(
                "Environment variables apply only when Launch Mode is ACP. PTY Passthrough ignores them.",
            ).apply {
                foreground = JBUI.CurrentTheme.Label.disabledForeground()
            }
        return DetailPanelHints(
            mcpScopeHint = mcpScopeHint,
            mcpPluginsHint = mcpPluginsHint,
            envHint = envHint,
        )
    }

    fun createDetailPanel(
        ideaMcp: JCheckBox,
        customMcp: JCheckBox,
        environmentPanel: JComponent,
        hints: DetailPanelHints,
    ): JPanel =
        JPanel(GridBagLayout()).apply {
            border = JBUI.Borders.emptyTop(8)
            var row = 0

            fun addRow(
                component: JComponent,
                gridFill: Int = GridBagConstraints.HORIZONTAL,
            ) {
                add(
                    component,
                    GridBagConstraints().apply {
                        gridx = 0
                        gridy = row++
                        weightx = 1.0
                        fill = gridFill
                        anchor = GridBagConstraints.WEST
                    },
                )
            }
            addRow(hints.mcpScopeHint)
            addRow(hints.mcpPluginsHint)
            addRow(ideaMcp)
            addRow(customMcp)
            addRow(hints.envHint)
            addRow(JBLabel("Environment variables"))
            addRow(environmentPanel, GridBagConstraints.BOTH)
        }

    fun createConfigurationsToolbar(
        model: AgentConfigsTableModel,
        table: JBTable,
        onImport: () -> Unit,
        onExport: () -> Unit,
    ) = ToolbarDecorator
        .createDecorator(table)
        .setAddAction { _ ->
            model.addRow(
                AgentConfigRow(
                    id = UUID.randomUUID().toString(),
                    name = "Agent ${model.rowCount + 1}",
                    launchMode = LaunchMode.PTY_PASSTHROUGH.name,
                    executionTarget = AgentSettingsState.ExecutionTarget.LOCAL.name,
                    wslDistribution = "",
                    binaryPath = "",
                    useNodeShellWrapper = false,
                    arguments = "",
                    workingDirectory = "",
                    useIdeaMcp = false,
                    useCustomMcp = false,
                    environmentVariables = emptyMap(),
                    isDefault = model.rowCount == 0,
                ),
            )
            val index = model.rowCount - 1
            if (index >= 0) {
                table.selectionModel.setSelectionInterval(index, index)
            }
        }.setRemoveAction { _ ->
            val selected = table.selectedRow
            if (selected >= 0) {
                model.removeRow(selected)
            }
        }.addExtraAction(
            object : com.intellij.openapi.actionSystem.AnAction("Import from acp.json") {
                override fun actionPerformed(e: com.intellij.openapi.actionSystem.AnActionEvent) {
                    onImport()
                }
            },
        ).addExtraAction(
            object : com.intellij.openapi.actionSystem.AnAction("Export to acp.json") {
                override fun actionPerformed(e: com.intellij.openapi.actionSystem.AnActionEvent) {
                    onExport()
                }
            },
        )
}
