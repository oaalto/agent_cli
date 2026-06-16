package com.oaalto.agent

import com.oaalto.agent.acp.AcpAgentEditor
import com.oaalto.agent.pty.PtyAgentEditor
import com.oaalto.agent.settings.LaunchMode
import kotlin.test.Test
import kotlin.test.assertEquals

class AgentEditorFactoryTest {
    @Test
    fun `factory resolves PTY editor type for Terminal launch mode`() {
        assertEquals(PtyAgentEditor::class.java, AgentEditorFactory.editorTypeForLaunchMode(LaunchMode.PTY_PASSTHROUGH))
    }

    @Test
    fun `factory resolves ACP editor type for ACP launch mode`() {
        assertEquals(AcpAgentEditor::class.java, AgentEditorFactory.editorTypeForLaunchMode(LaunchMode.ACP_CLIENT))
    }
}
