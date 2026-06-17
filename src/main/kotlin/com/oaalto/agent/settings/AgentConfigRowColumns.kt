package com.oaalto.agent.settings

internal object AgentConfigRowColumns {
    private val valueExtractors: List<(AgentConfigRow) -> Any> =
        listOf(
            { it.isDefault },
            { it.name },
            { LaunchMode.from(it.launchMode).displayLabel },
            { it.executionTarget },
            { it.wslDistribution },
            { it.binaryPath },
            { it.useNodeShellWrapper },
            { it.arguments },
            { it.workingDirectory },
        )

    fun valueAt(
        row: AgentConfigRow,
        columnIndex: Int,
    ): Any = valueExtractors.getOrNull(columnIndex)?.invoke(row) ?: ""

    fun setValueAt(
        row: AgentConfigRow,
        columnIndex: Int,
        value: Any?,
    ) {
        when (columnIndex) {
            AgentConfigsTableColumns.NAME -> row.name = (value as? String).orEmpty()
            AgentConfigsTableColumns.LAUNCH_MODE ->
                row.launchMode = LaunchMode.fromDisplayLabel((value as? String).orEmpty()).name
            AgentConfigsTableColumns.EXECUTION_TARGET ->
                row.executionTarget = (value as? String).orEmpty().trim().uppercase()
            AgentConfigsTableColumns.WSL_DISTRIBUTION -> row.wslDistribution = (value as? String).orEmpty()
            AgentConfigsTableColumns.BINARY_PATH -> row.binaryPath = (value as? String).orEmpty()
            AgentConfigsTableColumns.NODE_WRAPPER -> row.useNodeShellWrapper = (value as? Boolean) == true
            AgentConfigsTableColumns.ARGUMENTS -> row.arguments = (value as? String).orEmpty()
            AgentConfigsTableColumns.WORKING_DIRECTORY -> row.workingDirectory = (value as? String).orEmpty()
        }
    }
}
