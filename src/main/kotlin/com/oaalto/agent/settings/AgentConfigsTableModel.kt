package com.oaalto.agent.settings

import javax.swing.table.AbstractTableModel

class AgentConfigsTableModel : AbstractTableModel() {
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
            AgentConfigsTableColumns.DEFAULT, AgentConfigsTableColumns.NODE_WRAPPER -> java.lang.Boolean::class.java
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
            AgentConfigsTableColumns.DEFAULT -> row.isDefault
            AgentConfigsTableColumns.NAME -> row.name
            AgentConfigsTableColumns.LAUNCH_MODE -> LaunchMode.from(row.launchMode).displayLabel
            AgentConfigsTableColumns.EXECUTION_TARGET -> row.executionTarget
            AgentConfigsTableColumns.WSL_DISTRIBUTION -> row.wslDistribution
            AgentConfigsTableColumns.BINARY_PATH -> row.binaryPath
            AgentConfigsTableColumns.NODE_WRAPPER -> row.useNodeShellWrapper
            AgentConfigsTableColumns.ARGUMENTS -> row.arguments
            AgentConfigsTableColumns.WORKING_DIRECTORY -> row.workingDirectory
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
            AgentConfigsTableColumns.DEFAULT -> {
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
            AgentConfigsTableColumns.NAME -> row.name = (value as? String).orEmpty()
            AgentConfigsTableColumns.LAUNCH_MODE ->
                row.launchMode =
                    LaunchMode.fromDisplayLabel((value as? String).orEmpty()).name
            AgentConfigsTableColumns.EXECUTION_TARGET ->
                row.executionTarget =
                    (value as? String).orEmpty().trim().uppercase()
            AgentConfigsTableColumns.WSL_DISTRIBUTION -> row.wslDistribution = (value as? String).orEmpty()
            AgentConfigsTableColumns.BINARY_PATH -> row.binaryPath = (value as? String).orEmpty()
            AgentConfigsTableColumns.NODE_WRAPPER -> row.useNodeShellWrapper = (value as? Boolean) == true
            AgentConfigsTableColumns.ARGUMENTS -> row.arguments = (value as? String).orEmpty()
            AgentConfigsTableColumns.WORKING_DIRECTORY -> row.workingDirectory = (value as? String).orEmpty()
        }
        if (columnIndex != AgentConfigsTableColumns.DEFAULT) {
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
