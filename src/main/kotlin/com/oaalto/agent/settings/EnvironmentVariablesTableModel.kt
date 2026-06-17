package com.oaalto.agent.settings

import javax.swing.table.AbstractTableModel

class EnvironmentVariablesTableModel : AbstractTableModel() {
    private val rows = mutableListOf<EnvironmentVariableRow>()
    private val columns = listOf("Name", "Value")

    override fun getRowCount(): Int = rows.size

    override fun getColumnCount(): Int = columns.size

    override fun getColumnName(column: Int): String = columns[column]

    override fun isCellEditable(
        rowIndex: Int,
        columnIndex: Int,
    ): Boolean = true

    override fun getValueAt(
        rowIndex: Int,
        columnIndex: Int,
    ): Any =
        when (columnIndex) {
            0 -> rows[rowIndex].name
            1 -> rows[rowIndex].value
            else -> ""
        }

    override fun setValueAt(
        value: Any?,
        rowIndex: Int,
        columnIndex: Int,
    ) {
        when (columnIndex) {
            0 -> rows[rowIndex].name = (value as? String).orEmpty()
            1 -> rows[rowIndex].value = (value as? String).orEmpty()
        }
        fireTableCellUpdated(rowIndex, columnIndex)
    }

    fun addRow(
        name: String,
        value: String,
    ) {
        rows.add(EnvironmentVariableRow(name, value))
        val index = rows.lastIndex
        fireTableRowsInserted(index, index)
    }

    fun removeRow(index: Int) {
        if (index !in rows.indices) return
        rows.removeAt(index)
        fireTableRowsDeleted(index, index)
    }

    fun setFromMap(environmentVariables: Map<String, String>) {
        rows.clear()
        rows.addAll(
            environmentVariables.entries.map { (name, value) ->
                EnvironmentVariableRow(name, value)
            },
        )
        fireTableDataChanged()
    }

    fun toMap(): Map<String, String> {
        val result = linkedMapOf<String, String>()
        rows.forEach { row ->
            val name = row.name.trim()
            if (name.isNotEmpty()) {
                result[name] = row.value
            }
        }
        return result
    }

    private data class EnvironmentVariableRow(
        var name: String,
        var value: String,
    )
}
