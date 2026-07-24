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
import com.oaalto.agent.AgentCliLog
import com.oaalto.agent.AgentCliLogRedaction
import com.oaalto.agent.settings.acpjson.AcpJsonExporter
import com.oaalto.agent.settings.acpjson.AcpJsonImportDraft
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
                DetailPanelBindings(
                    model = model,
                    table = table,
                    ideaMcp = ideaMcp,
                    customMcp = customMcp,
                    envTable = environmentEditor.table,
                    envModel = environmentEditor.model,
                    mcpScopeHint = hints.mcpScopeHint,
                    mcpPluginsHint = hints.mcpPluginsHint,
                    envHint = hints.envHint,
                ),
            )

            rootPanel =
                JPanel(BorderLayout()).apply {
                    border = JBUI.Borders.empty(AgentConfigsTableColumns.SETTINGS_PANEL_INSET)
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
        return requireNotNull(rootPanel)
    }

    private fun wireDetailPanelControls(bindings: DetailPanelBindings) {
        bindings.table.selectionModel.addListSelectionListener { event: ListSelectionEvent ->
            if (event.valueIsAdjusting || syncingDetailPanel) return@addListSelectionListener
            val selected = bindings.table.selectedRow
            AgentSettingsDetailPanelSupport.persistDetailPanelToRow(bindings, detailPanelRowIndex, syncingDetailPanel)
            detailPanelRowIndex = selected
            AgentSettingsDetailPanelSupport.syncDetailPanelFromSelection(
                bindings,
                setSyncingDetailPanel = { syncingDetailPanel = it },
            )
            AgentSettingsDetailPanelSupport.updateDetailPanelAvailability(bindings)
        }

        bindings.model.addTableModelListener {
            if (syncingDetailPanel) return@addTableModelListener
            AgentSettingsDetailPanelSupport.updateDetailPanelAvailability(bindings)
        }

        val persistCurrentRow = {
            AgentSettingsDetailPanelSupport.persistDetailPanelToSelection(bindings, syncingDetailPanel)
        }
        bindings.ideaMcp.addActionListener { persistCurrentRow() }
        bindings.customMcp.addActionListener { persistCurrentRow() }
        bindings.envModel.addTableModelListener { persistCurrentRow() }
    }

    private fun refreshDetailPanel() {
        val bindings = detailPanelBindings() ?: return
        AgentSettingsDetailPanelSupport.syncDetailPanelFromSelection(
            bindings,
            setSyncingDetailPanel = { syncingDetailPanel = it },
        )
        detailPanelRowIndex = bindings.table.selectedRow
        AgentSettingsDetailPanelSupport.updateIdeaMcpAvailability(bindings.ideaMcp, bindings.mcpPluginsHint)
        AgentSettingsDetailPanelSupport.updateDetailPanelAvailability(bindings)
    }

    private fun detailPanelBindings(): DetailPanelBindings? =
        listOf(
            tableModel,
            table,
            ideaMcpCheckbox,
            customMcpCheckbox,
            environmentTable,
            environmentTableModel,
            mcpScopeHintLabel,
            mcpHintLabel,
            envHintLabel,
        ).any { it == null }.let { hasNull ->
            if (hasNull) {
                null
            } else {
                DetailPanelBindings(
                    model = tableModel as AgentConfigsTableModel,
                    table = table as JBTable,
                    ideaMcp = ideaMcpCheckbox as JCheckBox,
                    customMcp = customMcpCheckbox as JCheckBox,
                    envTable = environmentTable as JBTable,
                    envModel = environmentTableModel as EnvironmentVariablesTableModel,
                    mcpScopeHint = mcpScopeHintLabel as JBLabel,
                    mcpPluginsHint = mcpHintLabel as JBLabel,
                    envHint = envHintLabel as JBLabel,
                )
            }
        }

    override fun isModified(): Boolean {
        val bindings = detailPanelBindings() ?: return false
        AgentSettingsDetailPanelSupport.persistDetailPanelToSelection(bindings, syncingDetailPanel)
        val persisted =
            rowsFromState(
                AgentSettingsState.getInstance().getConfigurations(),
                AgentSettingsState.getInstance().getDefaultConfiguration()?.id,
            )
        return bindings.model.rowsSnapshot() != persisted
    }

    @Throws(ConfigurationException::class)
    override fun apply() {
        val bindings = detailPanelBindings() ?: return
        AgentSettingsDetailPanelSupport.persistDetailPanelToSelection(bindings, syncingDetailPanel)
        val rows = bindings.model.rowsSnapshot()
        AgentSettingsValidation.validateRows(rows)

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
        val rows = rowsFromState(settings.getConfigurations(), settings.getDefaultConfiguration()?.id)
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

    private fun importFromAcpJson(
        model: AgentConfigsTableModel,
        table: JBTable,
    ) {
        val descriptor =
            FileChooserDescriptorFactory
                .createSingleFileDescriptor("json")
                .withTitle("Import from acp.json")
        val file = FileChooser.chooseFile(descriptor, null, null) ?: return
        val drafts =
            readImportDrafts(table, file) ?: return
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

    private fun readImportDrafts(
        table: JBTable,
        file: com.intellij.openapi.vfs.VirtualFile,
    ): List<AcpJsonImportDraft>? {
        val text =
            runCatching { VfsUtil.loadText(file) }
                .getOrElse { throwable ->
                    val reason = throwable.message ?: "Failed to read ${file.path}"
                    agentCliLog.error(
                        message = "Settings import failed reading ${file.path}: ${sanitizeImportFailureMessage(
                            reason,
                        )}",
                    )
                    Messages.showErrorDialog(
                        table,
                        reason,
                        "Import failed",
                    )
                    return null
                }
        return AcpJsonImporter.parse(text).getOrElse { throwable ->
            val reason = throwable.message ?: "Invalid acp.json"
            agentCliLog.error(
                message = "Settings import failed parsing ${file.path}: ${sanitizeImportFailureMessage(reason)}",
            )
            Messages.showErrorDialog(
                table,
                reason,
                "Import failed",
            )
            null
        }
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
        if (hasEnvironmentValues && !confirmEnvironmentExport()) {
            return
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
            val reason = throwable.message ?: "Failed to write ${target.file.path}"
            agentCliLog.error(
                message =
                    "Settings export failed writing ${target.file.path}: " +
                        sanitizeImportFailureMessage(reason),
            )
            Messages.showErrorDialog(
                table,
                reason,
                "Export failed",
            )
        }
    }

    private fun confirmEnvironmentExport(): Boolean {
        val parent = table ?: return false
        val answer =
            Messages.showYesNoDialog(
                parent,
                "Export includes environment variable values in plaintext. Continue?",
                "Export to acp.json",
                Messages.getWarningIcon(),
            )
        return answer == Messages.YES
    }

    private fun sanitizeImportFailureMessage(message: String): String =
        AgentCliLogRedaction.redactSettingsFailureMessage(message)

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
        private val agentCliLog = AgentCliLog.getInstance(AgentSettingsConfigurable::class.java)
    }
}
