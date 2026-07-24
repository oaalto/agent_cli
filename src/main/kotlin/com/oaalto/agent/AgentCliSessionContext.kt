package com.oaalto.agent

import com.oaalto.agent.settings.LaunchMode

fun AgentVirtualFile.toAgentCliSessionContext(
    launchMode: LaunchMode,
    sessionId: String? = null,
): AgentCliSessionContext =
    AgentCliSessionContext(
        configId = configurationId,
        sessionId = sessionId,
        launchMode = launchMode,
        worktreePath = launchContext.workingDirectoryOverride,
    )

data class AgentCliSessionContext(
    val configId: String? = null,
    val sessionId: String? = null,
    val launchMode: LaunchMode? = null,
    val worktreePath: String? = null,
)

internal fun formatAgentCliLogMessage(
    message: String,
    context: AgentCliSessionContext?,
): String {
    val prefix = formatAgentCliSessionContextPrefix(context) ?: return message
    return "$prefix $message"
}

internal fun formatAgentCliSessionContextPrefix(context: AgentCliSessionContext?): String? {
    if (context == null) return null
    val parts = mutableListOf<String>()
    context.configId?.let { parts += "configId=$it" }
    context.sessionId?.let { parts += "sessionId=$it" }
    context.launchMode?.let { parts += "launchMode=${it.name}" }
    context.worktreePath?.let { parts += "worktreePath=$it" }
    return if (parts.isEmpty()) null else "[${parts.joinToString(" ")}]"
}
