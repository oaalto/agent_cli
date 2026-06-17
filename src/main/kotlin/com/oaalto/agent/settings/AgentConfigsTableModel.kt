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
    ): Any = AgentConfigRowColumns.valueAt(rows[rowIndex], columnIndex)

    override fun setValueAt(
        value: Any?,
        rowIndex: Int,
        columnIndex: Int,
    ) {
        val row = rows[rowIndex]
        if (columnIndex == AgentConfigsTableColumns.DEFAULT) {
            updateDefaultColumn(rowIndex, row, value)
        } else {
            AgentConfigRowColumns.setValueAt(row, columnIndex, value)
            fireTableCellUpdated(rowIndex, columnIndex)
        }
    }

    private fun updateDefaultColumn(
        rowIndex: Int,
        row: AgentConfigRow,
        value: Any?,
    ) {
        val newValue = (value as? Boolean) == true
        if (newValue) {
            rows.forEachIndexed { index, item ->
                item.isDefault = index == rowIndex
            }
            fireTableDataChanged()
            return
        }
        val defaultCount = rows.count { it.isDefault }
        if (!(row.isDefault && defaultCount == 1)) {
            row.isDefault = false
        }
        fireTableRowsUpdated(rowIndex, rowIndex)
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
