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
        return when {
            saved.isNullOrBlank() -> {
                val seeded = resolveDefaultOrFirst()
                AgentConfigurationResolutionResult(seeded, seeded)
            }
            saved in validIds -> AgentConfigurationResolutionResult(saved, saved)
            else -> {
                val fallback = resolveDefaultOrFirst()
                AgentConfigurationResolutionResult(fallback, fallback)
            }
        }
    }
}
