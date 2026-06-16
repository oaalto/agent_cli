package com.oaalto.agent.acp

import com.oaalto.agent.AgentLaunchContext
import com.oaalto.agent.settings.AgentSettingsState
import kotlin.test.Test
import kotlin.test.assertEquals

class AcpProcessLauncherTest {
    @Test
    fun `builds local launch command without resume flags`() {
        val configuration =
            AgentSettingsState.AgentCliConfiguration().apply {
                name = "Pi"
                binaryPath = "C:\\bin\\agent.exe"
                arguments = "--continue --model fast"
                workingDirectory = "C:\\dev\\agent_cli"
                executionTarget = AgentSettingsState.ExecutionTarget.LOCAL.name
            }

        val plan =
            AcpProcessLauncher
                .buildLaunchPlan(
                    projectContext = AgentProjectContext(basePath = "C:\\dev\\agent_cli"),
                    configuration = configuration,
                    launchContext = AgentLaunchContext(additionalArguments = listOf("--continue")),
                ).getOrThrow()

        assertEquals(listOf("C:\\bin\\agent.exe", "acp", "--model", "fast"), plan.command)
        assertEquals("C:\\dev\\agent_cli", plan.processWorkingDirectory)
        assertEquals("C:\\dev\\agent_cli", plan.sessionWorkingDirectory)
    }

    @Test
    fun `builds wrapped wsl launch command matching command builder`() {
        val hostBasePath = System.getProperty("user.home")
        val configuration =
            AgentSettingsState.AgentCliConfiguration().apply {
                name = "Pi"
                binaryPath = "pi"
                arguments = ""
                workingDirectory = "/home/olli/project"
                executionTarget = AgentSettingsState.ExecutionTarget.WSL.name
                useNodeShellWrapper = true
                wslDistribution = "Ubuntu"
            }

        val plan =
            AcpProcessLauncher
                .buildLaunchPlan(
                    projectContext = AgentProjectContext(basePath = hostBasePath),
                    configuration = configuration,
                    launchContext = AgentLaunchContext(),
                ).getOrThrow()

        assertEquals(
            listOf(
                "wsl.exe",
                "--distribution",
                "Ubuntu",
                "--cd",
                "/home/olli/project",
                "--",
                "bash",
                "-ilc",
                "exec pi",
            ),
            plan.command,
        )
        assertEquals(hostBasePath, plan.processWorkingDirectory)
        assertEquals("/home/olli/project", plan.sessionWorkingDirectory)
    }
}
