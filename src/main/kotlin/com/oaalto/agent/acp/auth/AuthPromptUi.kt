package com.oaalto.agent.acp.auth

interface AuthPromptUi {
    suspend fun promptApiKey(
        methodName: String,
        description: String?,
    ): AuthPromptResult

    suspend fun promptOAuthLink(
        methodName: String,
        description: String?,
        link: String,
    ): AuthPromptResult

    suspend fun waitForTerminalAuthCompletion(
        methodName: String,
        description: String?,
    ): AuthPromptResult
}

sealed class AuthPromptResult {
    data object Continue : AuthPromptResult()

    data object Cancelled : AuthPromptResult()
}
