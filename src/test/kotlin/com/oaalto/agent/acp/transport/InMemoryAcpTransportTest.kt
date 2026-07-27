package com.oaalto.agent.acp.transport

import com.oaalto.agent.AgentCliSessionContext
import com.oaalto.agent.acp.AccumulatedUsage
import com.oaalto.agent.acp.AcpLaunchPlan
import com.oaalto.agent.acp.AcpSessionListener
import com.oaalto.agent.acp.StructuredUpdate
import com.oaalto.agent.settings.LaunchMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertNotNull

class InMemoryAcpTransportTest {
    @Test
    fun `connect returns protocol and client then dispose is safe`() =
        runBlocking {
            val scope = CoroutineScope(SupervisorJob())
            val transport = InMemoryAcpTransport(scope)
            val listener = NoOpSessionListener()

            val connected =
                transport.connect(
                    launchPlan =
                        AcpLaunchPlan(
                            command = listOf("noop"),
                            processWorkingDirectory = ".",
                            sessionWorkingDirectory = ".",
                        ),
                    listener = listener,
                    sessionLogContext = { testLogContext() },
                )

            assertNotNull(connected.protocol)
            assertNotNull(connected.client)
            transport.dispose()
            transport.dispose()
        }

    private fun testLogContext(): AgentCliSessionContext =
        AgentCliSessionContext(
            configId = null,
            sessionId = null,
            launchMode = LaunchMode.ACP_CLIENT,
            worktreePath = null,
        )

    private class NoOpSessionListener : AcpSessionListener {
        override fun onStructuredUpdate(update: StructuredUpdate) = Unit

        override fun onError(message: String) = Unit

        override fun onUsageUpdate(usage: AccumulatedUsage) = Unit
    }
}
