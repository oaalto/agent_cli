package com.oaalto.agent.acp

import com.agentclientprotocol.annotations.UnstableApi
import com.agentclientprotocol.model.AcpMethod
import com.agentclientprotocol.model.AgentCapabilities
import com.agentclientprotocol.model.Implementation
import com.agentclientprotocol.model.InitializeRequest
import com.agentclientprotocol.model.InitializeResponse
import com.agentclientprotocol.model.LATEST_PROTOCOL_VERSION
import com.agentclientprotocol.model.LoadSessionRequest
import com.agentclientprotocol.model.LoadSessionResponse
import com.agentclientprotocol.model.NewSessionRequest
import com.agentclientprotocol.model.NewSessionResponse
import com.agentclientprotocol.model.SessionId
import com.agentclientprotocol.protocol.Protocol
import com.agentclientprotocol.protocol.setRequestHandler

/**
 * Declarative in-memory agent for session-loop integration tests.
 *
 * Responds to `initialize` with empty auth, to `session/new` with [newSessionId],
 * and to `session/load` with a deterministic success for [loadSessionId].
 */
class ScriptedAcpAgent(
    private val protocol: Protocol,
    private val newSessionId: String = "scripted-session-1",
    private val loadSessionId: String = newSessionId,
) {
    val loadSessionIds = mutableListOf<String>()

    init {
        installHandlers()
    }

    @OptIn(UnstableApi::class)
    private fun installHandlers() {
        protocol.setRequestHandler(AcpMethod.AgentMethods.Initialize) { params: InitializeRequest ->
            InitializeResponse(
                protocolVersion = minOf(params.protocolVersion, LATEST_PROTOCOL_VERSION),
                agentCapabilities = AgentCapabilities(),
                authMethods = emptyList(),
                agentInfo = Implementation(name = "scripted-test-agent", version = "test"),
                _meta = params._meta,
            )
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
    }
}
