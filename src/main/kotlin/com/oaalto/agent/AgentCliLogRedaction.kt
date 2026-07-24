package com.oaalto.agent

import com.oaalto.agent.acp.AcpLaunchPlan

object AgentCliLogRedaction {
    const val REDACTED = "[REDACTED]"

    private val SECRET_KEY_MARKERS =
        listOf(
            "PASSWORD",
            "SECRET",
            "TOKEN",
            "API_KEY",
            "APIKEY",
            "BEARER",
            "AUTHORIZATION",
        )

    fun isSecretEnvKey(key: String): Boolean {
        val upper = key.uppercase()
        return SECRET_KEY_MARKERS.any { upper.contains(it) }
    }

    fun redactEnvironmentVariables(env: Map<String, String>): Map<String, String> =
        env.mapValues { (key, value) ->
            if (isSecretEnvKey(key)) REDACTED else value
        }

    fun formatEnvironmentVariablesForDebug(env: Map<String, String>): String =
        redactEnvironmentVariables(env).entries.joinToString(", ") { (key, value) -> "$key=$value" }

    fun redactAuthToken(value: String): String = if (value.isBlank()) value else REDACTED

    fun redactSettingsFailureMessage(message: String): String {
        if (message.isBlank()) return message
        return Regex("""([^\s=:]+)\s*[=:]\s*(\S+)""").replace(message) { match ->
            val key = match.groupValues[1]
            if (isSecretEnvKey(key)) {
                "${match.groupValues[1]}=$REDACTED"
            } else {
                match.value
            }
        }
    }

    fun formatLaunchPlanForDebug(plan: AcpLaunchPlan): String =
        buildString {
            append("command=")
            append(plan.command)
            append(" processWorkingDirectory=")
            append(plan.processWorkingDirectory)
            append(" sessionWorkingDirectory=")
            append(plan.sessionWorkingDirectory)
            append(" environmentVariables={")
            append(formatEnvironmentVariablesForDebug(plan.environmentVariables))
            append("}")
            append(" mcpServers=")
            append(plan.sessionMcpServers().size)
            append(" exposeMcp=")
            append(plan.exposeMcp)
        }
}
