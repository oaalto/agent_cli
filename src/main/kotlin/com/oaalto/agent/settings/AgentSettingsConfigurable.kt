package com.oaalto.agent.settings

import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.fileChooser.FileChooserFactory
import com.intellij.openapi.fileChooser.FileSaverDescriptor
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.ui.components.JBLabel
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.JBUI
import com.oaalto.agent.settings.acpjson.AcpJsonExporter
import com.oaalto.agent.settings.acpjson.AcpJsonImporter
import java.awt.BorderLayout
import java.nio.charset.StandardCharsets
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.event.ListSelectionEvent

class AgentSettingsConfigurable : SearchableConfigurable {
    private var rootPanel: JPanel? = null
    private var table: JBTable? = null
    private var tableModel: AgentConfigsTableModel? = null
    private var ideaMcpCheckbox: JCheckBox? = null
    private var customMcpCheckbox: JCheckBox? = null
    private var environmentTable: JBTable? = null
    private var environmentTableModel: EnvironmentVariablesTableModel? = null
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
            val table = AgentSettingsUiFactory.createConfigurationTable(model)
            val ideaMcp = JCheckBox("Expose IntelliJ MCP server")
            val customMcp = JCheckBox("Expose user-configured MCP servers")
            val environmentEditor = AgentSettingsUiFactory.createEnvironmentVariablesEditor()
            val hints = AgentSettingsUiFactory.createDetailPanelHints()
            val detailPanel =
                AgentSettingsUiFactory.createDetailPanel(
                    ideaMcp = ideaMcp,
                    customMcp = customMcp,
                    environmentPanel = environmentEditor.panel,
                    hints = hints,
                )
            val toolbar =
                AgentSettingsUiFactory.createConfigurationsToolbar(
                    model = model,
                    table = table,
                    onImport = { importFromAcpJson(model, table) },
                    onExport = { exportToAcpJson(model) },
                )

            wireDetailPanelControls(
                model,
                table,
                ideaMcp,
                customMcp,
                environmentEditor.model,
                environmentEditor.table,
                hints.mcpScopeHint,
                hints.mcpPluginsHint,
                hints.envHint,
            )

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
            environmentTable = environmentEditor.table
            environmentTableModel = environmentEditor.model
            mcpScopeHintLabel = hints.mcpScopeHint
            mcpHintLabel = hints.mcpPluginsHint
            envHintLabel = hints.envHint
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
        envModel: EnvironmentVariablesTableModel,
        envTable: JBTable,
        mcpScopeHint: JBLabel,
        mcpPluginsHint: JBLabel,
        envHint: JBLabel,
    ) {
        table.selectionModel.addListSelectionListener { event: ListSelectionEvent ->
            if (event.valueIsAdjusting || syncingDetailPanel) return@addListSelectionListener
            val selected = table.selectedRow
            persistDetailPanelToRow(model, ideaMcp, customMcp, envModel, detailPanelRowIndex)
            detailPanelRowIndex = selected
            syncDetailPanelFromSelection(model, table, ideaMcp, customMcp, envModel)
            updateDetailPanelAvailability(
                model,
                table,
                ideaMcp,
                customMcp,
                envTable,
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
                envTable,
                mcpScopeHint,
                mcpPluginsHint,
                envHint,
            )
        }

        val persistCurrentRow = {
            persistDetailPanelToSelection(model, table, ideaMcp, customMcp, envModel)
        }
        ideaMcp.addActionListener { persistCurrentRow() }
        customMcp.addActionListener { persistCurrentRow() }
        envModel.addTableModelListener {
            persistCurrentRow()
        }
    }

    private fun refreshDetailPanel() {
        val bindings = detailPanelBindings() ?: return
        syncDetailPanelFromSelection(
            bindings.model,
            bindings.table,
            bindings.ideaMcp,
            bindings.customMcp,
            bindings.envModel,
        )
        detailPanelRowIndex = bindings.table.selectedRow
        updateIdeaMcpAvailability(bindings.ideaMcp, bindings.mcpPluginsHint)
        updateDetailPanelAvailability(
            bindings.model,
            bindings.table,
            bindings.ideaMcp,
            bindings.customMcp,
            bindings.envTable,
            bindings.mcpScopeHint,
            bindings.mcpPluginsHint,
            bindings.envHint,
        )
    }

    private fun detailPanelBindings(): DetailPanelBindings? {
        val model = tableModel
        val table = table
        val ideaMcp = ideaMcpCheckbox
        val customMcp = customMcpCheckbox
        if (model == null || table == null) {
            return null
        }
        if (ideaMcp == null || customMcp == null) {
            return null
        }
        val envTable = environmentTable
        val envModel = environmentTableModel
        val mcpScopeHint = mcpScopeHintLabel
        if (envTable == null || envModel == null || mcpScopeHint == null) {
            return null
        }
        val mcpPluginsHint = mcpHintLabel
        val envHint = envHintLabel
        if (mcpPluginsHint == null || envHint == null) {
            return null
        }
        return DetailPanelBindings(
            model = model,
            table = table,
            ideaMcp = ideaMcp,
            customMcp = customMcp,
            envTable = envTable,
            envModel = envModel,
            mcpScopeHint = mcpScopeHint,
            mcpPluginsHint = mcpPluginsHint,
            envHint = envHint,
        )
    }

    override fun isModified(): Boolean {
        val model = tableModel ?: return false
        persistDetailPanelToSelection(
            model,
            table ?: return false,
            ideaMcpCheckbox ?: return false,
            customMcpCheckbox ?: return false,
            environmentTableModel ?: return false,
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
            environmentTableModel ?: return,
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
        environmentTable = null
        environmentTableModel = null
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
        envModel: EnvironmentVariablesTableModel,
    ) {
        val selected = table.selectedRow
        if (selected !in 0 until model.rowCount) {
            syncingDetailPanel = true
            ideaMcp.isSelected = false
            customMcp.isSelected = false
            envModel.setFromMap(emptyMap())
            syncingDetailPanel = false
            return
        }
        val row = model.rowAt(selected)
        syncingDetailPanel = true
        ideaMcp.isSelected = row.useIdeaMcp
        customMcp.isSelected = row.useCustomMcp
        envModel.setFromMap(row.environmentVariables)
        syncingDetailPanel = false
    }

    private fun updateDetailPanelAvailability(
        model: AgentConfigsTableModel,
        table: JBTable,
        ideaMcp: JCheckBox,
        customMcp: JCheckBox,
        envTable: JBTable,
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
        envTable.isEnabled = acpClient
        mcpScopeHint.isVisible = !acpClient
        envHint.isVisible = !acpClient
        mcpPluginsHint.isVisible = acpClient && !ideaPluginsAvailable
    }

    private fun persistDetailPanelToRow(
        model: AgentConfigsTableModel,
        ideaMcp: JCheckBox,
        customMcp: JCheckBox,
        envModel: EnvironmentVariablesTableModel,
        rowIndex: Int,
    ) {
        if (syncingDetailPanel || rowIndex !in 0 until model.rowCount) return
        model.updateRow(
            rowIndex,
            useIdeaMcp = ideaMcp.isSelected,
            useCustomMcp = customMcp.isSelected,
            environmentVariables = envModel.toMap(),
        )
    }

    private fun persistDetailPanelToSelection(
        model: AgentConfigsTableModel,
        table: JBTable,
        ideaMcp: JCheckBox,
        customMcp: JCheckBox,
        envModel: EnvironmentVariablesTableModel,
    ) {
        persistDetailPanelToRow(model, ideaMcp, customMcp, envModel, table.selectedRow)
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

    companion object {
        const val ID: String = "com.oaalto.agent.settings"
    }
}
