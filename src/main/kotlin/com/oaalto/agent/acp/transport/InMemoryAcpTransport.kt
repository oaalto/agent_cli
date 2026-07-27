package com.oaalto.agent.acp.transport

import com.agentclientprotocol.client.Client
import com.agentclientprotocol.protocol.Protocol
import com.agentclientprotocol.transport.StdioTransport
import com.oaalto.agent.AgentCliSessionContext
import com.oaalto.agent.acp.AcpLaunchPlan
import com.oaalto.agent.acp.AcpSessionListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered
import java.io.PipedInputStream
import java.io.PipedOutputStream

/**
 * Test adapter that wires [Protocol] over piped stdio streams without spawning a process.
 */
class InMemoryAcpTransport(
    private val scope: CoroutineScope,
) : AcpProcessTransport {
    private var protocol: Protocol? = null
    private var agentSideOutput: PipedOutputStream? = null

    @Suppress("DEPRECATION")
    override suspend fun connect(
        launchPlan: AcpLaunchPlan,
        listener: AcpSessionListener,
        sessionLogContext: () -> AgentCliSessionContext,
    ): AcpProcessTransport.Connected {
        dispose()

        val agentInput = PipedInputStream()
        agentSideOutput = PipedOutputStream(agentInput)
        val clientInput = PipedInputStream()
        val clientOutput = PipedOutputStream(clientInput)

        val transport =
            StdioTransport(
                parentScope = scope,
                ioDispatcher = Dispatchers.IO,
                input = agentInput.asSource().buffered(),
                output = clientOutput.asSink().buffered(),
                name = "in-memory-acp-stdio",
            )
        val protocolInstance = Protocol(scope, transport)
        val clientInstance = Client(protocolInstance)
        protocol = protocolInstance

        return AcpProcessTransport.Connected(protocolInstance, clientInstance)
    }

    override fun dispose() {
        runCatching { protocol?.close() }
        runCatching { agentSideOutput?.close() }
        protocol = null
        agentSideOutput = null
    }
}
