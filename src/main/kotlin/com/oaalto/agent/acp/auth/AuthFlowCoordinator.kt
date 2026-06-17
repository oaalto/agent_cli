@file:Suppress("OPT_IN_USAGE")

package com.oaalto.agent.acp.auth

import com.agentclientprotocol.agent.AgentInfo
import com.agentclientprotocol.client.Client
import com.agentclientprotocol.model.AuthMethod
import com.agentclientprotocol.model.AuthMethodId
import com.oaalto.agent.acp.AcpSessionListener
import com.oaalto.agent.acp.TranscriptRenderer
import com.oaalto.agent.acp.ui.ShellPaneHost

class AuthFlowCoordinator(
    private val client: Client,
    private val agentInfo: AgentInfo,
    private val shellPaneHost: ShellPaneHost,
    private val authPromptUi: AuthPromptUi,
    private val listener: AcpSessionListener,
) {
    suspend fun authenticateIfRequired(): Result<Unit> {
        val methods = agentInfo.authMethods
        if (methods.isEmpty()) {
            return Result.success(Unit)
        }
        val method = methods.first()
        return when {
            runAuthenticate(method.id).isSuccess -> Result.success(Unit)
            else -> authenticateWithMethod(method)
        }
    }

    private suspend fun authenticateWithMethod(method: AuthMethod): Result<Unit> =
        when (method) {
            is AuthMethod.TerminalAuth -> authenticateTerminal(method)
            is AuthMethod.AgentAuth -> authenticateAgent(method)
            is AuthMethod.EnvVarAuth -> {
                val link = method.link?.trim().orEmpty()
                if (link.isNotEmpty()) {
                    authenticateOAuth(method.name, method.description, link, method.id)
                } else {
                    Result.failure(
                        IllegalStateException(
                            "Environment-variable auth must be configured before launching the agent process.",
                        ),
                    )
                }
            }
            is AuthMethod.UnknownAuthMethod ->
                Result.failure(IllegalStateException("Unsupported auth method: ${method.type}"))
        }

    private suspend fun authenticateTerminal(method: AuthMethod.TerminalAuth): Result<Unit> {
        val command = method.args?.takeIf { it.isNotEmpty() } ?: listOf("login")
        shellPaneHost.startAuthCommand(command, method.env.orEmpty())
        when (authPromptUi.waitForTerminalAuthCompletion(method.name, method.description)) {
            AuthPromptResult.Cancelled ->
                return Result.failure(IllegalStateException("Authentication was cancelled."))
            AuthPromptResult.Continue -> Unit
        }
        return runAuthenticate(method.id)
    }

    private suspend fun authenticateAgent(method: AuthMethod.AgentAuth): Result<Unit> {
        val promptResult =
            if (AuthMethodSupport.agentAuthRequiresApiKey(method)) {
                authPromptUi.promptApiKey(method.name, method.description)
            } else {
                authPromptUi.waitForTerminalAuthCompletion(method.name, method.description)
            }
        when (promptResult) {
            AuthPromptResult.Cancelled ->
                return Result.failure(IllegalStateException("Authentication was cancelled."))
            AuthPromptResult.Continue -> Unit
        }
        return runAuthenticate(method.id)
    }

    private suspend fun authenticateOAuth(
        methodName: String,
        description: String?,
        link: String,
        methodId: AuthMethodId,
    ): Result<Unit> {
        when (authPromptUi.promptOAuthLink(methodName, description, link)) {
            AuthPromptResult.Cancelled ->
                return Result.failure(IllegalStateException("Authentication was cancelled."))
            AuthPromptResult.Continue -> Unit
        }
        return runAuthenticate(methodId)
    }

    private suspend fun runAuthenticate(methodId: AuthMethodId): Result<Unit> =
        runCatching {
            client.authenticate(methodId)
        }.map { Unit }.onFailure { throwable ->
            listener.onTranscriptLine(
                TranscriptRenderer.formatAuthFailure(
                    throwable.message ?: "Authentication failed.",
                ),
            )
        }
}
