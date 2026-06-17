package com.oaalto.agent.settings

import java.util.UUID

internal object AgentSettingsStateSupport {
    fun ensureValidState(state: AgentSettingsState.State) {
        state.configurations =
            state.configurations
                .map { sanitize(it) }
                .distinctBy { it.id }
                .toMutableList()

        if (state.configurations.isEmpty()) {
            state.defaultConfigurationId = null
            return
        }

        val defaultId = state.defaultConfigurationId
        if (defaultId.isNullOrBlank() || state.configurations.none { it.id == defaultId }) {
            state.defaultConfigurationId = state.configurations.first().id
        }
    }

    fun sanitize(configuration: AgentSettingsState.AgentCliConfiguration): AgentSettingsState.AgentCliConfiguration {
        val sanitized = configuration.copyOf()
        if (sanitized.id.isBlank()) {
            sanitized.id = UUID.randomUUID().toString()
        }
        if (sanitized.name.isBlank()) {
            sanitized.name = "Agent"
        }
        sanitized.binaryPath = sanitized.binaryPath.trim()
        if (!sanitized.useNodeShellWrapper && sanitized.nodeWrapper.isNotBlank()) {
            sanitized.useNodeShellWrapper = true
        }
        sanitized.nodeWrapper = ""
        sanitized.arguments = sanitized.arguments.trim()
        sanitized.workingDirectory = sanitized.workingDirectory.trim()
        sanitized.executionTarget = executionTargetOrDefault(sanitized.executionTarget)
        sanitized.launchMode = launchModeOrDefault(sanitized.launchMode)
        sanitized.wslDistribution = sanitized.wslDistribution.trim()
        sanitized.environmentVariables =
            LinkedHashMap(
                sanitized.environmentVariables
                    .mapKeys { it.key.trim() }
                    .filterKeys { it.isNotBlank() }
                    .mapValues { it.value },
            )
        return sanitized
    }

    private fun executionTargetOrDefault(value: String): String =
        value.trim().uppercase().takeIf { raw ->
            AgentSettingsState.ExecutionTarget.entries.any { it.name == raw }
        } ?: AgentSettingsState.ExecutionTarget.LOCAL.name

    private fun launchModeOrDefault(value: String): String = LaunchMode.from(value).name

    private fun AgentSettingsState.AgentCliConfiguration.copyOf(): AgentSettingsState.AgentCliConfiguration =
        AgentSettingsState.AgentCliConfiguration().also {
            it.id = id
            it.name = name
            it.launchMode = launchMode
            it.binaryPath = binaryPath
            it.useNodeShellWrapper = useNodeShellWrapper
            it.nodeWrapper = nodeWrapper
            it.arguments = arguments
            it.workingDirectory = workingDirectory
            it.executionTarget = executionTarget
            it.wslDistribution = wslDistribution
            it.useIdeaMcp = useIdeaMcp
            it.useCustomMcp = useCustomMcp
            it.environmentVariables = LinkedHashMap(environmentVariables)
        }
}

internal fun AgentSettingsState.AgentCliConfiguration.copyOf(): AgentSettingsState.AgentCliConfiguration =
    AgentSettingsState.AgentCliConfiguration().also {
        it.id = id
        it.name = name
        it.launchMode = launchMode
        it.binaryPath = binaryPath
        it.useNodeShellWrapper = useNodeShellWrapper
        it.nodeWrapper = nodeWrapper
        it.arguments = arguments
        it.workingDirectory = workingDirectory
        it.executionTarget = executionTarget
        it.wslDistribution = wslDistribution
        it.useIdeaMcp = useIdeaMcp
        it.useCustomMcp = useCustomMcp
        it.environmentVariables = LinkedHashMap(environmentVariables)
    }
