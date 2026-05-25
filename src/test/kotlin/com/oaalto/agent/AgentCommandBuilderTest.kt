package com.oaalto.agent

import kotlin.test.Test
import kotlin.test.assertEquals

class AgentCommandBuilderTest {
    @Test
    fun `builds direct local command when node shell wrapper is disabled`() {
        val command =
            AgentCommandBuilder.buildLocalCommand(
                binaryPath = "/usr/local/bin/codex",
                arguments = listOf("--continue"),
                useNodeShellWrapper = false,
            )

        assertEquals(listOf("/usr/local/bin/codex", "--continue"), command)
    }

    @Test
    fun `builds direct wsl command when node shell wrapper is disabled`() {
        val command =
            AgentCommandBuilder.buildWslCommand(
                binaryPath = "/usr/local/bin/codex",
                arguments = listOf("resume", "--last"),
                wslDistribution = "Ubuntu",
                wslWorkingDirectory = "/home/olli/project",
            )

        assertEquals(
            listOf(
                "wsl.exe",
                "--distribution",
                "Ubuntu",
                "--cd",
                "/home/olli/project",
                "--",
                "/usr/local/bin/codex",
                "resume",
                "--last",
            ),
            command,
        )
    }

    @Test
    fun `builds wrapped wsl command for node agents`() {
        val command =
            AgentCommandBuilder.buildWslCommand(
                binaryPath = "pi",
                arguments = listOf("--model", "fast mode"),
                wslDistribution = "",
                wslWorkingDirectory = "/home/olli/project",
                useNodeShellWrapper = true,
            )

        assertEquals(
            listOf(
                "wsl.exe",
                "--cd",
                "/home/olli/project",
                "--",
                "bash",
                "-ilc",
                "exec pi --model 'fast mode'",
            ),
            command,
        )
    }

    @Test
    fun `quotes wrapper payload arguments for posix shell`() {
        val command =
            AgentCommandBuilder.buildLocalCommand(
                binaryPath = "/home/olli/bin/pi cli",
                arguments = listOf("it's alive", ""),
                useNodeShellWrapper = true,
            )

        assertEquals(
            listOf(
                "bash",
                "-ilc",
                "exec '/home/olli/bin/pi cli' 'it'\"'\"'s alive' ''",
            ),
            command,
        )
    }
}
