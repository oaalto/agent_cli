package com.oaalto.agent.worktree.resume

import com.oaalto.agent.settings.AgentSettingsState
import kotlin.test.Test
import kotlin.test.assertEquals

class PtyResumeStrategyTest {
    @Test
    fun `returns continue args for cursor-style resume binaries`() {
        assertEquals(listOf("--continue"), baseArgsFor("cursor-agent"))
        assertEquals(listOf("--continue"), baseArgsFor("agent"))
        assertEquals(listOf("--continue"), baseArgsFor("claude"))
        assertEquals(listOf("--continue"), baseArgsFor("opencode"))
    }

    @Test
    fun `returns provider specific resume args for gemini and codex`() {
        assertEquals(listOf("--resume"), baseArgsFor("gemini"))
        assertEquals(listOf("resume", "--last"), baseArgsFor("codex"))
    }

    @Test
    fun `returns null for unsupported executable`() {
        assertEquals(null, baseArgsFor("unsupported-cli"))
    }

    @Test
    fun `normalizes executable names from absolute paths and extensions`() {
        assertEquals(listOf("--continue"), baseArgsFor("""C:\tools\opencode.exe"""))
        assertEquals(listOf("--continue"), baseArgsFor("/usr/local/bin/opencode"))
    }

    @Test
    fun `resume false returns empty pty args`() {
        val plan =
            PtyResumeStrategy.prepareLaunch(
                ResumeContext(
                    configuration = configuration("cursor-agent"),
                    worktreeRecord = null,
                    workingDirectory = "/project",
                    resume = false,
                    cursorProbe = CursorResumeProbe.NoOp,
                ),
            )
        assertEquals(LaunchResumePlan.Pty(emptyList()), plan)
    }

    @Test
    fun `resume true returns probed pty args`() {
        val plan =
            PtyResumeStrategy.prepareLaunch(
                ResumeContext(
                    configuration = configuration("cursor-agent"),
                    worktreeRecord = null,
                    workingDirectory = "/project",
                    resume = true,
                    cursorProbe =
                        CursorResumeProbe { request ->
                            request.arguments.filterNot { it == "--continue" }
                        },
                ),
            )
        assertEquals(LaunchResumePlan.Pty(emptyList()), plan)
    }

    private fun baseArgsFor(binaryPath: String): List<String>? =
        PtyResumeStrategy.baseResumeArguments(configuration(binaryPath))

    private fun configuration(binaryPath: String): AgentSettingsState.AgentCliConfiguration =
        AgentSettingsState.AgentCliConfiguration().apply {
            this.binaryPath = binaryPath
            launchMode = "PTY_PASSTHROUGH"
        }
}
