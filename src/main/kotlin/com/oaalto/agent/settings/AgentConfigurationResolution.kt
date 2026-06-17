package com.oaalto.agent.settings

internal data class AgentConfigurationResolutionInput(
    val configurationIds: List<String>,
    val defaultConfigurationId: String?,
    val projectSelectedConfigurationId: String?,
)

internal data class AgentConfigurationResolutionResult(
    val configurationId: String?,
    val persistedProjectSelectionId: String?,
)

internal object AgentConfigurationResolution {
    fun resolve(input: AgentConfigurationResolutionInput): AgentConfigurationResolutionResult {
        if (input.configurationIds.isEmpty()) {
            return AgentConfigurationResolutionResult(null, null)
        }

        val validIds = input.configurationIds.toSet()

        fun resolveDefaultOrFirst(): String {
            val defaultId = input.defaultConfigurationId
            if (!defaultId.isNullOrBlank() && defaultId in validIds) {
                return defaultId
            }
            return input.configurationIds.first()
        }

        val saved = input.projectSelectedConfigurationId
        if (saved.isNullOrBlank()) {
            val seeded = resolveDefaultOrFirst()
            return AgentConfigurationResolutionResult(seeded, seeded)
        }
        if (saved in validIds) {
            return AgentConfigurationResolutionResult(saved, saved)
        }
        val fallback = resolveDefaultOrFirst()
        return AgentConfigurationResolutionResult(fallback, fallback)
    }
}
