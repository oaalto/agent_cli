package com.oaalto.agent.settings

import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.util.UUID
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.ListSelectionModel
import javax.swing.table.AbstractTableModel

class AgentSettingsConfigurable : SearchableConfigurable {
    private var rootPanel: JPanel? = null
    private var table: JBTable? = null
    private var tableModel: AgentConfigsTableModel? = null

    override fun getId(): String = ID

    override fun getDisplayName(): String = "Agent CLI"

    override fun createComponent(): JComponent {
        if (rootPanel == null) {
            val model = AgentConfigsTableModel()
            val table = JBTable(model).apply {
                selectionModel.selectionMode = ListSelectionModel.SINGLE_SELECTION
                fillsViewportHeight = true
            }

            val toolbar = ToolbarDecorator.createDecorator(table)
                .setAddAction { _ ->
                    model.addRow(
                        AgentConfigRow(
                            id = UUID.randomUUID().toString(),
                            name = "Agent ${model.rowCount + 1}",
                            binaryPath = "",
                            arguments = "",
                            workingDirectory = "",
                            isDefault = model.rowCount == 0,
                        )
                    )
                    val index = model.rowCount - 1
                    if (index >= 0) {
                        table.selectionModel.setSelectionInterval(index, index)
                    }
                }
                .setRemoveAction { _ ->
                    val selected = table.selectedRow
                    if (selected >= 0) {
                        model.removeRow(selected)
                    }
                }

            rootPanel = JPanel(BorderLayout()).apply {
                border = JBUI.Borders.empty(8)
                add(toolbar.createPanel(), BorderLayout.CENTER)
            }
            tableModel = model
            this.table = table
        }
        return rootPanel!!
    }

    override fun isModified(): Boolean {
        val model = tableModel ?: return false
        val persisted = rowsFromState(AgentSettingsState.getInstance().getConfigurations(), AgentSettingsState.getInstance().getSelectedConfiguration()?.id)
        return model.rowsSnapshot() != persisted
    }

    @Throws(ConfigurationException::class)
    override fun apply() {
        val model = tableModel ?: return
        val rows = model.rowsSnapshot()
        validateRows(rows)

        val defaultId = rows.firstOrNull { it.isDefault }?.id
        val configurations = rows.map { row ->
            AgentSettingsState.AgentCliConfiguration().apply {
                id = row.id
                name = row.name.trim()
                binaryPath = row.binaryPath.trim()
                arguments = row.arguments.trim()
                workingDirectory = row.workingDirectory.trim()
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
        }
    }

    override fun disposeUIResources() {
        rootPanel = null
        table = null
        tableModel = null
    }

    private fun validateRows(rows: List<AgentConfigRow>) {
        rows.forEachIndexed { index, row ->
            if (row.name.trim().isEmpty()) {
                throw ConfigurationException("Configuration #${index + 1} must have a name.")
            }
            if (row.binaryPath.trim().isEmpty()) {
                throw ConfigurationException("Configuration '${row.name}' must have a binary path.")
            }
        }
        if (rows.isNotEmpty() && rows.none { it.isDefault }) {
            throw ConfigurationException("Mark one configuration as default.")
        }
    }

    private fun rowsFromState(
        configurations: List<AgentSettingsState.AgentCliConfiguration>,
        selectedId: String?,
    ): List<AgentConfigRow> {
        return configurations.mapIndexed { index, it ->
            AgentConfigRow(
                id = it.id,
                name = it.name,
                binaryPath = it.binaryPath,
                arguments = it.arguments,
                workingDirectory = it.workingDirectory,
                isDefault = when {
                    !selectedId.isNullOrBlank() -> it.id == selectedId
                    else -> index == 0
                },
            )
        }
    }

    private data class AgentConfigRow(
        var id: String,
        var name: String,
        var binaryPath: String,
        var arguments: String,
        var workingDirectory: String,
        var isDefault: Boolean,
    )

    private class AgentConfigsTableModel : AbstractTableModel() {
        private val rows = mutableListOf<AgentConfigRow>()
        private val columns = listOf("Default", "Name", "Binary Path", "Arguments", "Working Directory")

        override fun getRowCount(): Int = rows.size

        override fun getColumnCount(): Int = columns.size

        override fun getColumnName(column: Int): String = columns[column]

        override fun getColumnClass(columnIndex: Int): Class<*> {
            return if (columnIndex == 0) java.lang.Boolean::class.java else String::class.java
        }

        override fun isCellEditable(rowIndex: Int, columnIndex: Int): Boolean = true

        override fun getValueAt(rowIndex: Int, columnIndex: Int): Any {
            val row = rows[rowIndex]
            return when (columnIndex) {
                0 -> row.isDefault
                1 -> row.name
                2 -> row.binaryPath
                3 -> row.arguments
                4 -> row.workingDirectory
                else -> ""
            }
        }

        override fun setValueAt(value: Any?, rowIndex: Int, columnIndex: Int) {
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
                2 -> row.binaryPath = (value as? String).orEmpty()
                3 -> row.arguments = (value as? String).orEmpty()
                4 -> row.workingDirectory = (value as? String).orEmpty()
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
