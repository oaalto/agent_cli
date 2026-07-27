package com.oaalto.agent.acp.transport

import com.agentclientprotocol.client.Client
import com.agentclientprotocol.protocol.Protocol
import com.oaalto.agent.AgentCliSessionContext
import com.oaalto.agent.acp.AcpLaunchPlan
import com.oaalto.agent.acp.AcpSessionListener

interface AcpProcessTransport {
    data class Connected(
        val protocol: Protocol,
        val client: Client,
    )

    suspend fun connect(
        launchPlan: AcpLaunchPlan,
        listener: AcpSessionListener,
        sessionLogContext: () -> AgentCliSessionContext,
    ): Connected

    fun dispose()
}
