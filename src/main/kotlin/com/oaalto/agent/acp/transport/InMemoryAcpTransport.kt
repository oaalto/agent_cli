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
 *
 * When [agentConfigurer] is provided, it is invoked on the agent-side [Protocol] before
 * `start()` so integration tests can install scripted handlers without a subprocess.
 */
class InMemoryAcpTransport(
    private val scope: CoroutineScope,
    private val agentConfigurer: ((Protocol) -> Unit)? = null,
) : AcpProcessTransport {
    private var clientProtocol: Protocol? = null
    private var agentProtocol: Protocol? = null
    private var agentSideOutput: PipedOutputStream? = null

    /** True when both protocol handles are cleared; also true before the first [connect]. */
    val isDisposed: Boolean
        get() = clientProtocol == null && agentProtocol == null

    @Suppress("DEPRECATION")
    override suspend fun connect(
        launchPlan: AcpLaunchPlan,
        listener: AcpSessionListener,
        sessionLogContext: () -> AgentCliSessionContext,
    ): AcpProcessTransport.Connected {
        dispose()

        val agentInput = PipedInputStream()
        val agentOutput = PipedOutputStream(agentInput)
        agentSideOutput = agentOutput
        val clientInput = PipedInputStream()
        val clientOutput = PipedOutputStream(clientInput)

        agentConfigurer?.let { configure ->
            startAgentSide(
                clientInput = clientInput,
                agentOutput = agentOutput,
                configure = configure,
            )
        }

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
        clientProtocol = protocolInstance

        return AcpProcessTransport.Connected(protocolInstance, clientInstance)
    }

    @Suppress("DEPRECATION")
    private fun startAgentSide(
        clientInput: PipedInputStream,
        agentOutput: PipedOutputStream,
        configure: (Protocol) -> Unit,
    ) {
        val transport =
            StdioTransport(
                parentScope = scope,
                ioDispatcher = Dispatchers.IO,
                input = clientInput.asSource().buffered(),
                output = agentOutput.asSink().buffered(),
                name = "in-memory-acp-agent-stdio",
            )
        val protocolInstance = Protocol(scope, transport)
        configure(protocolInstance)
        protocolInstance.start()
        agentProtocol = protocolInstance
    }

    override fun dispose() {
        runCatching { clientProtocol?.close() }
        runCatching { agentProtocol?.close() }
        runCatching { agentSideOutput?.close() }
        clientProtocol = null
        agentProtocol = null
        agentSideOutput = null
    }
}
