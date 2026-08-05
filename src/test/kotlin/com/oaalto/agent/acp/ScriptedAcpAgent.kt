package com.oaalto.agent.acp

import com.agentclientprotocol.annotations.UnstableApi
import com.agentclientprotocol.model.AcpMethod
import com.agentclientprotocol.model.AgentCapabilities
import com.agentclientprotocol.model.AuthMethod
import com.agentclientprotocol.model.AuthenticateRequest
import com.agentclientprotocol.model.AuthenticateResponse
import com.agentclientprotocol.model.ContentBlock
import com.agentclientprotocol.model.Implementation
import com.agentclientprotocol.model.InitializeRequest
import com.agentclientprotocol.model.InitializeResponse
import com.agentclientprotocol.model.LATEST_PROTOCOL_VERSION
import com.agentclientprotocol.model.LoadSessionRequest
import com.agentclientprotocol.model.LoadSessionResponse
import com.agentclientprotocol.model.NewSessionRequest
import com.agentclientprotocol.model.NewSessionResponse
import com.agentclientprotocol.model.PromptRequest
import com.agentclientprotocol.model.PromptResponse
import com.agentclientprotocol.model.SessionId
import com.agentclientprotocol.model.SessionNotification
import com.agentclientprotocol.model.SessionUpdate
import com.agentclientprotocol.model.StopReason
import com.agentclientprotocol.model.ToolCallId
import com.agentclientprotocol.model.ToolCallStatus
import com.agentclientprotocol.model.ToolKind
import com.agentclientprotocol.protocol.Protocol
import com.agentclientprotocol.protocol.acpFail
import com.agentclientprotocol.protocol.sendNotification
import com.agentclientprotocol.protocol.setRequestHandler
import kotlinx.coroutines.delay
import kotlin.time.Duration

/**
 * Declarative in-memory agent for session-loop integration tests.
 *
 * Responds to `initialize` with [authConfig], to `authenticate` per auth retry policy,
 * to `session/new` with [newSessionId],
 * to `session/load` with a deterministic success for [loadSessionId], and to
 * `session/prompt` with [promptUpdates] followed by a turn completion response.
 */
data class ScriptedAcpAgentAuthConfig(
    val authMethods: List<AuthMethod> = emptyList(),
    val authenticateAttemptsBeforeSuccess: Int = 0,
    val authenticateAlwaysFails: Boolean = false,
)

class ScriptedAcpAgent(
    private val protocol: Protocol,
    private val newSessionId: String = "scripted-session-1",
    private val loadSessionId: String = newSessionId,
    private val authConfig: ScriptedAcpAgentAuthConfig = ScriptedAcpAgentAuthConfig(),
    private val promptUpdates: List<SessionUpdate> = defaultPromptUpdates(),
    private val promptDelayBetweenUpdates: Duration = Duration.ZERO,
) {
    val loadSessionIds = mutableListOf<String>()
    val receivedPrompts = mutableListOf<String>()
    val authenticateCallCount = mutableListOf<String>()

    init {
        installHandlers()
    }

    @OptIn(UnstableApi::class)
    private fun installHandlers() {
        protocol.setRequestHandler(AcpMethod.AgentMethods.Initialize) { params: InitializeRequest ->
            InitializeResponse(
                protocolVersion = minOf(params.protocolVersion, LATEST_PROTOCOL_VERSION),
                agentCapabilities = AgentCapabilities(),
                authMethods = authConfig.authMethods,
                agentInfo = Implementation(name = "scripted-test-agent", version = "test"),
                _meta = params._meta,
            )
        }

        protocol.setRequestHandler(AcpMethod.AgentMethods.Authenticate) { params: AuthenticateRequest ->
            authenticateCallCount.add(params.methodId.value)
            if (
                authConfig.authenticateAlwaysFails ||
                authenticateCallCount.size <= authConfig.authenticateAttemptsBeforeSuccess
            ) {
                acpFail("authentication required")
            }
            AuthenticateResponse(_meta = params._meta)
        }

        protocol.setRequestHandler(AcpMethod.AgentMethods.SessionNew) { _: NewSessionRequest ->
            NewSessionResponse(
                sessionId = SessionId(newSessionId),
                modes = null,
                models = null,
                configOptions = null,
            )
        }

        protocol.setRequestHandler(AcpMethod.AgentMethods.SessionLoad) { params: LoadSessionRequest ->
            loadSessionIds.add(params.sessionId.value)
            check(params.sessionId.value == loadSessionId) {
                "unexpected load session id: ${params.sessionId.value}"
            }
            LoadSessionResponse(
                modes = null,
                models = null,
                configOptions = null,
            )
        }

        protocol.setRequestHandler(AcpMethod.AgentMethods.SessionPrompt) { params: PromptRequest ->
            receivedPrompts.add(extractPromptText(params.prompt))
            for (update in promptUpdates) {
                if (promptDelayBetweenUpdates > Duration.ZERO) {
                    delay(promptDelayBetweenUpdates)
                }
                protocol.sendNotification(
                    AcpMethod.ClientMethods.SessionUpdate,
                    SessionNotification(params.sessionId, update, params._meta),
                )
            }
            PromptResponse(stopReason = StopReason.END_TURN, _meta = params._meta)
        }
    }

    companion object {
        fun defaultPromptUpdates(): List<SessionUpdate> =
            listOf(
                SessionUpdate.AgentMessageChunk(content = ContentBlock.Text("scripted ")),
                SessionUpdate.AgentMessageChunk(content = ContentBlock.Text("reply")),
                SessionUpdate.ToolCall(
                    toolCallId = ToolCallId("scripted-tool-1"),
                    title = "read README",
                    kind = ToolKind.READ,
                    status = ToolCallStatus.IN_PROGRESS,
                ),
            )
    }

    private fun extractPromptText(content: List<ContentBlock>): String =
        content
            .mapNotNull { block ->
                when (block) {
                    is ContentBlock.Text -> block.text
                    else -> null
                }
            }.joinToString("")
}
