package com.oaalto.agent.settings

import com.intellij.openapi.options.ConfigurationException

internal object AgentSettingsValidation {
    fun validateRows(rows: List<AgentConfigRow>) {
        val errors = rows.flatMapIndexed(::validateRow)
        val defaultError =
            if (rows.isNotEmpty() && rows.none { it.isDefault }) {
                listOf("Mark one configuration as default.")
            } else {
                emptyList()
            }
        val allErrors = errors + defaultError
        if (allErrors.isNotEmpty()) {
            throw ConfigurationException(allErrors.joinToString("\n"))
        }
    }

    private fun validateRow(
        index: Int,
        row: AgentConfigRow,
    ): List<String> =
        buildList {
            if (row.name.trim().isEmpty()) {
                add("Configuration #${index + 1} must have a name.")
            }
            if (row.binaryPath.trim().isEmpty()) {
                add("Configuration '${row.name}' must have a binary path.")
            }
            addInvalidExecutionTargetError(row)
            addInvalidLaunchModeError(row)
        }

    private fun MutableList<String>.addInvalidExecutionTargetError(row: AgentConfigRow) {
        val normalizedTarget = row.executionTarget.trim().uppercase()
        if (AgentSettingsState.ExecutionTarget.entries.none { it.name == normalizedTarget }) {
            add(
                "Configuration '${row.name}' has invalid execution target '${row.executionTarget}'. " +
                    "Allowed values: ${AgentSettingsState.ExecutionTarget.entries.joinToString { it.name }}.",
            )
        }
    }

    private fun MutableList<String>.addInvalidLaunchModeError(row: AgentConfigRow) {
        val normalizedLaunchMode = normalizeLaunchMode(row.launchMode)
        if (LaunchMode.entries.none { it.name == normalizedLaunchMode }) {
            add(
                "Configuration '${row.name}' has invalid launch mode '${row.launchMode}'. " +
                    "Allowed values: ${LaunchMode.displayLabels().joinToString()}.",
            )
        }
    }
}
