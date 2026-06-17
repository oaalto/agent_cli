package com.oaalto.agent.settings

data class AgentConfigRow(
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

internal fun AgentSettingsState.AgentCliConfiguration.toRow(isDefault: Boolean): AgentConfigRow =
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

internal fun normalizeExecutionTarget(value: String): String {
    val normalized = value.trim().uppercase()
    return AgentSettingsState.ExecutionTarget.entries
        .firstOrNull { it.name == normalized }
        ?.name
        ?: AgentSettingsState.ExecutionTarget.LOCAL.name
}

internal fun normalizeLaunchMode(value: String): String {
    if (LaunchMode.entries.any { it.displayLabel == value.trim() }) {
        return LaunchMode.fromDisplayLabel(value).name
    }
    return LaunchMode.from(value).name
}
