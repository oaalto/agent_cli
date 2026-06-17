package com.oaalto.agent.settings

import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.fileChooser.FileChooserFactory
import com.intellij.openapi.fileChooser.FileSaverDescriptor
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.JBUI
import com.oaalto.agent.settings.acpjson.AcpJsonExporter
import com.oaalto.agent.settings.acpjson.AcpJsonImporter
import java.awt.BorderLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.nio.charset.StandardCharsets
import java.util.UUID
import javax.swing.DefaultCellEditor
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.ListSelectionModel
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.event.ListSelectionEvent
import javax.swing.table.AbstractTableModel

class AgentSettingsConfigurable : SearchableConfigurable {
    private var rootPanel: JPanel? = null
    private var table: JBTable? = null
    private var tableModel: AgentConfigsTableModel? = null
    private var ideaMcpCheckbox: JCheckBox? = null
    private var customMcpCheckbox: JCheckBox? = null
    private var environmentArea: JBTextArea? = null
    private var mcpScopeHintLabel: JBLabel? = null
    private var mcpHintLabel: JBLabel? = null
    private var envHintLabel: JBLabel? = null
    private var syncingDetailPanel = false
    private var detailPanelRowIndex = -1

    override fun getId(): String = ID

    override fun getDisplayName(): String = "Agent CLI"

    override fun createComponent(): JComponent {
        if (rootPanel == null) {
            val model = AgentConfigsTableModel()
            val table =
                JBTable(model).apply {
                    selectionModel.selectionMode = ListSelectionModel.SINGLE_SELECTION
                    fillsViewportHeight = true
                }
            table.columnModel.getColumn(2).cellEditor =
                DefaultCellEditor(
                    ComboBox(
                        LaunchMode.displayLabels().toTypedArray(),
                    ),
                )
            table.columnModel.getColumn(3).cellEditor =
                DefaultCellEditor(
                    ComboBox(
                        AgentSettingsState.ExecutionTarget.entries
                            .map { it.name }
                            .toTypedArray(),
                    ),
                )

            val ideaMcp = JCheckBox("Expose IntelliJ MCP server")
            val customMcp = JCheckBox("Expose user-configured MCP servers")
            val envArea =
                JBTextArea().apply {
                    rows = 4
                    lineWrap = true
                }
            val hint =
                JBLabel("MCP toggles apply only when Launch Mode is ACP. PTY Passthrough ignores them.").apply {
                    foreground = JBUI.CurrentTheme.Label.disabledForeground()
                }
            val aiHint =
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

            val detailPanel =
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
                    addRow(hint)
                    addRow(aiHint)
                    addRow(ideaMcp)
                    addRow(customMcp)
                    addRow(envHint)
                    addRow(JBLabel("Environment variables (KEY=VALUE per line)"))
                    addRow(envArea, GridBagConstraints.BOTH)
                }

            val toolbar =
                ToolbarDecorator
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
                                importFromAcpJson(model, table)
                            }
                        },
                    ).addExtraAction(
                        object : com.intellij.openapi.actionSystem.AnAction("Export to acp.json") {
                            override fun actionPerformed(e: com.intellij.openapi.actionSystem.AnActionEvent) {
                                exportToAcpJson(model)
                            }
                        },
                    )

            wireDetailPanelControls(model, table, ideaMcp, customMcp, envArea, hint, aiHint, envHint)

            rootPanel =
                JPanel(BorderLayout()).apply {
                    border = JBUI.Borders.empty(8)
                    add(toolbar.createPanel(), BorderLayout.CENTER)
                    add(detailPanel, BorderLayout.SOUTH)
                }
            tableModel = model
            this.table = table
            ideaMcpCheckbox = ideaMcp
            customMcpCheckbox = customMcp
            environmentArea = envArea
            mcpScopeHintLabel = hint
            mcpHintLabel = aiHint
            envHintLabel = envHint
            if (model.rowCount > 0) {
                table.selectionModel.setSelectionInterval(0, 0)
                detailPanelRowIndex = 0
            }
            refreshDetailPanel()
        }
        return rootPanel!!
    }

    private fun wireDetailPanelControls(
        model: AgentConfigsTableModel,
        table: JBTable,
        ideaMcp: JCheckBox,
        customMcp: JCheckBox,
        envArea: JBTextArea,
        mcpScopeHint: JBLabel,
        mcpPluginsHint: JBLabel,
        envHint: JBLabel,
    ) {
        table.selectionModel.addListSelectionListener { event: ListSelectionEvent ->
            if (event.valueIsAdjusting || syncingDetailPanel) return@addListSelectionListener
            val selected = table.selectedRow
            persistDetailPanelToRow(model, ideaMcp, customMcp, envArea, detailPanelRowIndex)
            detailPanelRowIndex = selected
            syncDetailPanelFromSelection(model, table, ideaMcp, customMcp, envArea)
            updateDetailPanelAvailability(
                model,
                table,
                ideaMcp,
                customMcp,
                envArea,
                mcpScopeHint,
                mcpPluginsHint,
                envHint,
            )
        }

        model.addTableModelListener {
            if (syncingDetailPanel) return@addTableModelListener
            updateDetailPanelAvailability(
                model,
                table,
                ideaMcp,
                customMcp,
                envArea,
                mcpScopeHint,
                mcpPluginsHint,
                envHint,
            )
        }

        val persistCurrentRow = {
            persistDetailPanelToSelection(model, table, ideaMcp, customMcp, envArea)
        }
        ideaMcp.addActionListener { persistCurrentRow() }
        customMcp.addActionListener { persistCurrentRow() }
        envArea.document.addDocumentListener(
            object : DocumentListener {
                override fun insertUpdate(e: DocumentEvent) {
                    persistCurrentRow()
                }

                override fun removeUpdate(e: DocumentEvent) {
                    persistCurrentRow()
                }

                override fun changedUpdate(e: DocumentEvent) {
                    persistCurrentRow()
                }
            },
        )
    }

    private fun refreshDetailPanel() {
        val model = tableModel ?: return
        val table = table ?: return
        val ideaMcp = ideaMcpCheckbox ?: return
        val customMcp = customMcpCheckbox ?: return
        val envArea = environmentArea ?: return
        val mcpScopeHint = mcpScopeHintLabel ?: return
        val mcpPluginsHint = mcpHintLabel ?: return
        val envHint = envHintLabel ?: return

        syncDetailPanelFromSelection(model, table, ideaMcp, customMcp, envArea)
        detailPanelRowIndex = table.selectedRow
        updateIdeaMcpAvailability(ideaMcp, mcpPluginsHint)
        updateDetailPanelAvailability(
            model,
            table,
            ideaMcp,
            customMcp,
            envArea,
            mcpScopeHint,
            mcpPluginsHint,
            envHint,
        )
    }

    override fun isModified(): Boolean {
        val model = tableModel ?: return false
        persistDetailPanelToSelection(
            model,
            table ?: return false,
            ideaMcpCheckbox ?: return false,
            customMcpCheckbox ?: return false,
            environmentArea ?: return false,
        )
        val persisted =
            rowsFromState(
                AgentSettingsState.getInstance().getConfigurations(),
                AgentSettingsState.getInstance().getSelectedConfiguration()?.id,
            )
        return model.rowsSnapshot() != persisted
    }

    @Throws(ConfigurationException::class)
    override fun apply() {
        val model = tableModel ?: return
        persistDetailPanelToSelection(
            model,
            table ?: return,
            ideaMcpCheckbox ?: return,
            customMcpCheckbox ?: return,
            environmentArea ?: return,
        )
        val rows = model.rowsSnapshot()
        validateRows(rows)

        val defaultId = rows.firstOrNull { it.isDefault }?.id
        val configurations =
            rows.map { row ->
                AgentSettingsState.AgentCliConfiguration().apply {
                    id = row.id
                    name = row.name.trim()
                    launchMode = normalizeLaunchMode(row.launchMode)
                    executionTarget = normalizeExecutionTarget(row.executionTarget)
                    wslDistribution = row.wslDistribution.trim()
                    binaryPath = row.binaryPath.trim()
                    useNodeShellWrapper = row.useNodeShellWrapper
                    arguments = row.arguments.trim()
                    workingDirectory = row.workingDirectory.trim()
                    useIdeaMcp = row.useIdeaMcp
                    useCustomMcp = row.useCustomMcp
                    environmentVariables = LinkedHashMap(row.environmentVariables)
                }
            }
        AgentSettingsState.getInstance().updateConfigurations(configurations, defaultId)
    }

    override fun reset() {
        val model = tableModel ?: return
        val settings = AgentSettingsState.getInstance()
        val rows = rowsFromState(settings.getConfigurations(), settings.getSelectedConfiguration()?.id)
        model.setRows(rows)
        if (rows.isNotEmpty()) {
            table?.selectionModel?.setSelectionInterval(0, 0)
            detailPanelRowIndex = 0
        } else {
            detailPanelRowIndex = -1
        }
        refreshDetailPanel()
    }

    override fun disposeUIResources() {
        rootPanel = null
        table = null
        tableModel = null
        ideaMcpCheckbox = null
        customMcpCheckbox = null
        environmentArea = null
        mcpScopeHintLabel = null
        mcpHintLabel = null
        envHintLabel = null
        detailPanelRowIndex = -1
    }

    private fun updateIdeaMcpAvailability(
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

    private fun syncDetailPanelFromSelection(
        model: AgentConfigsTableModel,
        table: JBTable,
        ideaMcp: JCheckBox,
        customMcp: JCheckBox,
        envArea: JBTextArea,
    ) {
        val selected = table.selectedRow
        if (selected !in 0 until model.rowCount) {
            syncingDetailPanel = true
            ideaMcp.isSelected = false
            customMcp.isSelected = false
            envArea.text = ""
            syncingDetailPanel = false
            return
        }
        val row = model.rowAt(selected)
        syncingDetailPanel = true
        ideaMcp.isSelected = row.useIdeaMcp
        customMcp.isSelected = row.useCustomMcp
        envArea.text = EnvironmentVariableText.format(row.environmentVariables)
        syncingDetailPanel = false
    }

    private fun updateDetailPanelAvailability(
        model: AgentConfigsTableModel,
        table: JBTable,
        ideaMcp: JCheckBox,
        customMcp: JCheckBox,
        envArea: JBTextArea,
        mcpScopeHint: JBLabel,
        mcpPluginsHint: JBLabel,
        envHint: JBLabel,
    ) {
        val selected = table.selectedRow
        val acpClient =
            selected in 0 until model.rowCount &&
                LaunchMode.from(model.rowAt(selected).launchMode) == LaunchMode.ACP_CLIENT
        val ideaPluginsAvailable = AiAssistantPresence.default.isAvailable()
        ideaMcp.isEnabled = acpClient && ideaPluginsAvailable
        customMcp.isEnabled = acpClient
        envArea.isEnabled = acpClient
        mcpScopeHint.isVisible = !acpClient
        envHint.isVisible = !acpClient
        mcpPluginsHint.isVisible = acpClient && !ideaPluginsAvailable
    }

    private fun persistDetailPanelToRow(
        model: AgentConfigsTableModel,
        ideaMcp: JCheckBox,
        customMcp: JCheckBox,
        envArea: JBTextArea,
        rowIndex: Int,
    ) {
        if (syncingDetailPanel || rowIndex !in 0 until model.rowCount) return
        model.updateRow(
            rowIndex,
            useIdeaMcp = ideaMcp.isSelected,
            useCustomMcp = customMcp.isSelected,
            environmentVariables = EnvironmentVariableText.parse(envArea.text),
        )
    }

    private fun persistDetailPanelToSelection(
        model: AgentConfigsTableModel,
        table: JBTable,
        ideaMcp: JCheckBox,
        customMcp: JCheckBox,
        envArea: JBTextArea,
    ) {
        persistDetailPanelToRow(model, ideaMcp, customMcp, envArea, table.selectedRow)
    }

    private fun importFromAcpJson(
        model: AgentConfigsTableModel,
        table: JBTable,
    ) {
        val descriptor =
            FileChooserDescriptorFactory
                .createSingleFileDescriptor("json")
                .withTitle("Import from acp.json")
        val file =
            FileChooser.chooseFile(descriptor, null, null)
                ?: return
        val text =
            runCatching { VfsUtil.loadText(file) }
                .getOrElse { throwable ->
                    Messages.showErrorDialog(
                        table,
                        throwable.message ?: "Failed to read ${file.path}",
                        "Import failed",
                    )
                    return
                }
        val drafts =
            AcpJsonImporter.parse(text).getOrElse { throwable ->
                Messages.showErrorDialog(
                    table,
                    throwable.message ?: "Invalid acp.json",
                    "Import failed",
                )
                return
            }
        val existingRows = model.rowsSnapshot()
        val collisions =
            drafts
                .map { it.name }
                .filter { draftName ->
                    existingRows.any { it.name.equals(draftName, ignoreCase = true) }
                }
        val overwriteNames = resolveImportOverwriteNames(table, collisions)
        val merged =
            AcpJsonImporter.merge(
                drafts = drafts,
                existing = existingRows.map { it.toConfiguration() },
                overwriteNames = overwriteNames,
            )
        val selectedId = existingRows.firstOrNull { it.isDefault }?.id
        model.setRows(
            merged.configurations.mapIndexed { index, configuration ->
                configuration.toRow(
                    isDefault =
                        when {
                            !selectedId.isNullOrBlank() -> configuration.id == selectedId
                            else -> index == 0
                        },
                )
            },
        )
        if (model.rowCount > 0) {
            table.selectionModel.setSelectionInterval(0, 0)
            detailPanelRowIndex = 0
        } else {
            detailPanelRowIndex = -1
        }
        refreshDetailPanel()
    }

    private fun resolveImportOverwriteNames(
        table: JBTable,
        collisions: List<String>,
    ): Set<String> {
        if (collisions.isEmpty()) {
            return emptySet()
        }
        val answer =
            Messages.showYesNoDialog(
                table,
                "Configurations already exist for: ${collisions.joinToString(", ")}. Overwrite them?",
                "Import from acp.json",
                Messages.getQuestionIcon(),
            )
        return if (answer == Messages.YES) collisions.toSet() else emptySet()
    }

    private fun exportToAcpJson(model: AgentConfigsTableModel) {
        val configurations = model.rowsSnapshot().map { it.toConfiguration() }
        val hasEnvironmentValues =
            configurations.any { configuration ->
                configuration.environmentVariables.values.any { value -> value.isNotBlank() }
            }
        if (hasEnvironmentValues) {
            val parent = table ?: return
            val answer =
                Messages.showYesNoDialog(
                    parent,
                    "Export includes environment variable values in plaintext. Continue?",
                    "Export to acp.json",
                    Messages.getWarningIcon(),
                )
            if (answer != Messages.YES) {
                return
            }
        }
        val saver =
            FileSaverDescriptor(
                "Export to acp.json",
                "Save ACP agent_servers entries",
                "json",
            )
        val dialog = FileChooserFactory.getInstance().createSaveFileDialog(saver, null)
        val target = dialog.save((null as java.nio.file.Path?), "acp.json") ?: return
        val rows = model.rowsSnapshot()
        val json = AcpJsonExporter.export(rows.map { it.toConfiguration() })
        runCatching {
            target.file.writeText(json, StandardCharsets.UTF_8)
        }.onFailure { throwable ->
            Messages.showErrorDialog(
                table,
                throwable.message ?: "Failed to write ${target.file.path}",
                "Export failed",
            )
        }
    }

    private fun validateRows(rows: List<AgentConfigRow>) {
        rows.forEachIndexed { index, row ->
            if (row.name.trim().isEmpty()) {
                throw ConfigurationException("Configuration #${index + 1} must have a name.")
            }
            if (row.binaryPath.trim().isEmpty()) {
                throw ConfigurationException("Configuration '${row.name}' must have a binary path.")
            }
            val normalizedTarget = row.executionTarget.trim().uppercase()
            if (AgentSettingsState.ExecutionTarget.entries.none { it.name == normalizedTarget }) {
                throw ConfigurationException(
                    "Configuration '${row.name}' has invalid execution target '${row.executionTarget}'. " +
                        "Allowed values: ${AgentSettingsState.ExecutionTarget.entries.joinToString { it.name }}.",
                )
            }
            val normalizedLaunchMode = normalizeLaunchMode(row.launchMode)
            if (LaunchMode.entries.none { it.name == normalizedLaunchMode }) {
                throw ConfigurationException(
                    "Configuration '${row.name}' has invalid launch mode '${row.launchMode}'. " +
                        "Allowed values: ${LaunchMode.displayLabels().joinToString()}.",
                )
            }
        }
        if (rows.isNotEmpty() && rows.none { it.isDefault }) {
            throw ConfigurationException("Mark one configuration as default.")
        }
    }

    private fun normalizeExecutionTarget(value: String): String {
        val normalized = value.trim().uppercase()
        return AgentSettingsState.ExecutionTarget.entries
            .firstOrNull { it.name == normalized }
            ?.name
            ?: AgentSettingsState.ExecutionTarget.LOCAL.name
    }

    private fun normalizeLaunchMode(value: String): String {
        if (LaunchMode.entries.any { it.displayLabel == value.trim() }) {
            return LaunchMode.fromDisplayLabel(value).name
        }
        return LaunchMode.from(value).name
    }

    private fun rowsFromState(
        configurations: List<AgentSettingsState.AgentCliConfiguration>,
        selectedId: String?,
    ): List<AgentConfigRow> =
        configurations.mapIndexed { index, configuration ->
            configuration.toRow(
                isDefault =
                    when {
                        !selectedId.isNullOrBlank() -> configuration.id == selectedId
                        else -> index == 0
                    },
            )
        }

    private data class AgentConfigRow(
        var id: String,
        var name: String,
        var launchMode: String,
        var executionTarget: String,
        var wslDistribution: String,
        var binaryPath: String,
        var useNodeShellWrapper: Boolean,
        var arguments: String,
        var workingDirectory: String,
        var useIdeaMcp: Boolean,
        var useCustomMcp: Boolean,
        var environmentVariables: Map<String, String>,
        var isDefault: Boolean,
    ) {
        fun toConfiguration(): AgentSettingsState.AgentCliConfiguration =
            AgentSettingsState.AgentCliConfiguration().apply {
                id = this@AgentConfigRow.id
                name = this@AgentConfigRow.name
                launchMode = this@AgentConfigRow.launchMode
                executionTarget = this@AgentConfigRow.executionTarget
                wslDistribution = this@AgentConfigRow.wslDistribution
                binaryPath = this@AgentConfigRow.binaryPath
                useNodeShellWrapper = this@AgentConfigRow.useNodeShellWrapper
                arguments = this@AgentConfigRow.arguments
                workingDirectory = this@AgentConfigRow.workingDirectory
                useIdeaMcp = this@AgentConfigRow.useIdeaMcp
                useCustomMcp = this@AgentConfigRow.useCustomMcp
                environmentVariables = LinkedHashMap(this@AgentConfigRow.environmentVariables)
            }
    }

    private fun AgentSettingsState.AgentCliConfiguration.toRow(isDefault: Boolean): AgentConfigRow =
        AgentConfigRow(
            id = id,
            name = name,
            launchMode = LaunchMode.from(launchMode).name,
            executionTarget = normalizeExecutionTarget(executionTarget),
            wslDistribution = wslDistribution,
            binaryPath = binaryPath,
            useNodeShellWrapper = useNodeShellWrapper,
            arguments = arguments,
            workingDirectory = workingDirectory,
            useIdeaMcp = useIdeaMcp,
            useCustomMcp = useCustomMcp,
            environmentVariables = LinkedHashMap(environmentVariables),
            isDefault = isDefault,
        )

    private class AgentConfigsTableModel : AbstractTableModel() {
        private val rows = mutableListOf<AgentConfigRow>()
        private val columns =
            listOf(
                "Default",
                "Name",
                "Launch Mode",
                "Execution Target",
                "WSL Distribution",
                "Binary Path",
                "Node Wrapper",
                "Arguments",
                "Working Directory",
            )

        override fun getRowCount(): Int = rows.size

        override fun getColumnCount(): Int = columns.size

        override fun getColumnName(column: Int): String = columns[column]

        override fun getColumnClass(columnIndex: Int): Class<*> =
            when (columnIndex) {
                0, 6 -> java.lang.Boolean::class.java
                else -> String::class.java
            }

        override fun isCellEditable(
            rowIndex: Int,
            columnIndex: Int,
        ): Boolean = true

        override fun getValueAt(
            rowIndex: Int,
            columnIndex: Int,
        ): Any {
            val row = rows[rowIndex]
            return when (columnIndex) {
                0 -> row.isDefault
                1 -> row.name
                2 -> LaunchMode.from(row.launchMode).displayLabel
                3 -> row.executionTarget
                4 -> row.wslDistribution
                5 -> row.binaryPath
                6 -> row.useNodeShellWrapper
                7 -> row.arguments
                8 -> row.workingDirectory
                else -> ""
            }
        }

        override fun setValueAt(
            value: Any?,
            rowIndex: Int,
            columnIndex: Int,
        ) {
            val row = rows[rowIndex]
            when (columnIndex) {
                0 -> {
                    val newValue = (value as? Boolean) == true
                    if (newValue) {
                        rows.forEachIndexed { index, item ->
                            item.isDefault = index == rowIndex
                        }
                        fireTableDataChanged()
                    } else {
                        val defaultCount = rows.count { it.isDefault }
                        if (!(row.isDefault && defaultCount == 1)) {
                            row.isDefault = false
                        }
                        fireTableRowsUpdated(rowIndex, rowIndex)
                    }
                }
                1 -> row.name = (value as? String).orEmpty()
                2 -> row.launchMode = LaunchMode.fromDisplayLabel((value as? String).orEmpty()).name
                3 -> row.executionTarget = (value as? String).orEmpty().trim().uppercase()
                4 -> row.wslDistribution = (value as? String).orEmpty()
                5 -> row.binaryPath = (value as? String).orEmpty()
                6 -> row.useNodeShellWrapper = (value as? Boolean) == true
                7 -> row.arguments = (value as? String).orEmpty()
                8 -> row.workingDirectory = (value as? String).orEmpty()
            }
            if (columnIndex != 0) {
                fireTableCellUpdated(rowIndex, columnIndex)
            }
        }

        fun addRow(row: AgentConfigRow) {
            if (rows.isEmpty()) {
                row.isDefault = true
            } else if (row.isDefault) {
                rows.forEach { it.isDefault = false }
            }
            rows.add(row)
            val index = rows.lastIndex
            fireTableRowsInserted(index, index)
            ensureDefaultRow()
        }

        fun removeRow(index: Int) {
            if (index !in rows.indices) return
            val removedDefault = rows[index].isDefault
            rows.removeAt(index)
            fireTableRowsDeleted(index, index)
            if (removedDefault) {
                ensureDefaultRow()
                fireTableDataChanged()
            }
        }

        fun setRows(newRows: List<AgentConfigRow>) {
            rows.clear()
            rows.addAll(newRows.map { it.copy() })
            ensureDefaultRow()
            fireTableDataChanged()
        }

        fun rowsSnapshot(): List<AgentConfigRow> = rows.map { it.copy() }

        fun rowAt(index: Int): AgentConfigRow = rows[index]

        fun updateRow(
            index: Int,
            useIdeaMcp: Boolean,
            useCustomMcp: Boolean,
            environmentVariables: Map<String, String>,
        ) {
            if (index !in rows.indices) return
            rows[index].useIdeaMcp = useIdeaMcp
            rows[index].useCustomMcp = useCustomMcp
            rows[index].environmentVariables = LinkedHashMap(environmentVariables)
        }

        private fun ensureDefaultRow() {
            if (rows.isEmpty()) return
            val defaultIndices = rows.withIndex().filter { it.value.isDefault }.map { it.index }
            when {
                defaultIndices.isEmpty() -> rows.first().isDefault = true
                defaultIndices.size > 1 -> {
                    val keep = defaultIndices.first()
                    rows.forEachIndexed { index, row -> row.isDefault = index == keep }
                }
            }
        }
    }

    companion object {
        const val ID: String = "com.oaalto.agent.settings"
    }
}
