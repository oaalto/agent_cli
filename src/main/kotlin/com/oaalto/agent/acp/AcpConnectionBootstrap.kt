package com.oaalto.agent.acp

import com.agentclientprotocol.agent.AgentInfo
import com.agentclientprotocol.client.Client
import com.agentclientprotocol.client.ClientInfo
import com.agentclientprotocol.model.Implementation
import com.agentclientprotocol.protocol.Protocol
import com.oaalto.agent.acp.auth.AuthFlowCoordinator

class AcpConnectionBootstrap(
    private val listener: AcpSessionListener,
) {
    suspend fun initialize(
        protocol: Protocol,
        client: Client,
        editorContext: AcpEditorContext,
    ): AgentInfo {
        protocol.start()
        val capabilities =
            AcpClientCapabilities.build(AcpClientCapabilities.fullSupport)
        val info =
            client.initialize(
                ClientInfo(
                    capabilities = capabilities,
                    implementation = Implementation(name = "agent-cli-plugin", version = "3.0"),
                ),
            )
        val authCoordinator =
            AuthFlowCoordinator(
                client = client,
                agentInfo = info,
                shellPaneHost = editorContext.shellPaneHost,
                authPromptUi = editorContext.authPromptUi,
                listener = listener,
            )
        authCoordinator.authenticateIfRequired().getOrElse { throwable ->
            throw throwable
        }
        return info
    }
}
