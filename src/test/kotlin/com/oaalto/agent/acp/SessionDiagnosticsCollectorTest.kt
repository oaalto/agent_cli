package com.oaalto.agent.acp

import com.oaalto.agent.AgentCliCorrelationToken
import com.oaalto.agent.AgentCliSessionContext
import com.oaalto.agent.settings.LaunchMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SessionDiagnosticsCollectorTest {
    @Test
    fun `clipboard bundle includes required fields`() {
        val collector = SessionDiagnosticsCollector()
        collector.record("a3f2", "connection failed")
        val context =
            AgentCliSessionContext(
                configId = "pi-config",
                sessionId = "sess-1",
                launchMode = LaunchMode.ACP_CLIENT,
                worktreePath = "/tmp/worktree",
            )

        val bundle =
            collector.formatClipboardBundle(
                context = context,
                worktreeLabel = "/tmp/worktree",
            )

        assertTrue(bundle.contains("configId: pi-config"))
        assertTrue(bundle.contains("sessionId: sess-1"))
        assertTrue(bundle.contains("launchMode: ACP_CLIENT"))
        assertTrue(bundle.contains("worktree: /tmp/worktree"))
        assertTrue(bundle.contains(AgentCliCorrelationToken.format("a3f2")))
        assertTrue(bundle.contains("connection failed"))
    }

    @Test
    fun `worktree label falls back to current-project`() {
        assertEquals(
            "current-project",
            SessionDiagnosticsCollector.worktreeLabel(LaunchMode.ACP_CLIENT, null),
        )
    }
}
