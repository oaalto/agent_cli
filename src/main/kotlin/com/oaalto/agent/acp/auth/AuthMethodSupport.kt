package com.oaalto.agent.acp.auth

import com.agentclientprotocol.model.AuthMethod

object AuthMethodSupport {
    fun agentAuthRequiresApiKey(method: AuthMethod.AgentAuth): Boolean {
        val haystack = "${method.name} ${method.description.orEmpty()}".lowercase()
        if (usesExistingLoginCredentials(haystack)) {
            return false
        }
        return haystack.contains("api key") || haystack.contains("apikey")
    }

    fun formatAuthMessage(
        methodName: String,
        description: String?,
        actionLine: String,
    ): String =
        buildString {
            append("[auth] $methodName")
            val details = description?.trim().orEmpty()
            if (details.isNotEmpty()) {
                append('\n')
                append(details)
            }
            if (actionLine.isNotBlank()) {
                append('\n')
                append(actionLine)
            }
        }

    private fun usesExistingLoginCredentials(haystack: String): Boolean =
        haystack.contains("existing") &&
            (haystack.contains("login") || haystack.contains("credentials"))
}
