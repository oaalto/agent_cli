package com.oaalto.agent.worktree

import com.oaalto.agent.settings.AgentSettingsState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AgentWorktreeServiceTest {
    @Test
    fun `returns continue args for cursor-style resume binaries`() {
        assertEquals(listOf("--continue"), resumeArgsFor("cursor-agent"))
        assertEquals(listOf("--continue"), resumeArgsFor("agent"))
        assertEquals(listOf("--continue"), resumeArgsFor("claude"))
        assertEquals(listOf("--continue"), resumeArgsFor("opencode"))
    }

    @Test
    fun `returns provider specific resume args for gemini and codex`() {
        assertEquals(listOf("--resume"), resumeArgsFor("gemini"))
        assertEquals(listOf("resume", "--last"), resumeArgsFor("codex"))
    }

    @Test
    fun `returns null for unsupported executable`() {
        assertNull(resumeArgsFor("unsupported-cli"))
    }

    @Test
    fun `normalizes executable names from absolute paths and extensions`() {
        assertEquals(listOf("--continue"), resumeArgsFor("""C:\tools\opencode.exe"""))
        assertEquals(listOf("--continue"), resumeArgsFor("/usr/local/bin/opencode"))
    }

    private fun resumeArgsFor(binaryPath: String): List<String>? =
        AgentWorktreeService.resumeArgumentsForConfiguration(
            AgentSettingsState.AgentCliConfiguration().apply {
                this.binaryPath = binaryPath
            },
        )
}
