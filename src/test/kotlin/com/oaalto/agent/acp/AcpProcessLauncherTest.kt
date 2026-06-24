package com.oaalto.agent.acp

import com.oaalto.agent.AgentLaunchContext
import com.oaalto.agent.settings.AgentSettingsState
import com.oaalto.agent.settings.LaunchMode
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AcpProcessLauncherTest {
    @Test
    fun `builds local launch command without resume flags`() {
        val projectDir = Files.createTempDirectory("acp-launch-project").toFile()
        projectDir.deleteOnExit()
        val binaryPath = "agent"

        val configuration =
            AgentSettingsState.AgentCliConfiguration().apply {
                name = "Pi"
                this.binaryPath = binaryPath
                arguments = "--continue --model fast"
                workingDirectory = projectDir.absolutePath
                executionTarget = AgentSettingsState.ExecutionTarget.LOCAL.name
            }

        val plan =
            AcpProcessLauncher
                .buildLaunchPlan(
                    projectContext = AgentProjectContext(basePath = projectDir.absolutePath),
                    configuration = configuration,
                    launchContext = AgentLaunchContext(additionalArguments = listOf("--continue")),
                ).getOrThrow()

        assertEquals(listOf(binaryPath, "acp", "--model", "fast"), plan.command)
        assertEquals(projectDir.absolutePath, plan.processWorkingDirectory)
        assertEquals(projectDir.absolutePath, plan.sessionWorkingDirectory)
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

    @Test
    fun `passes configured environment variables and leaves mcp empty when toggles are off`() {
        val hostBasePath =
            Files
                .createTempDirectory("acp-launch-env")
                .toFile()
                .also { it.deleteOnExit() }
                .absolutePath
        val configuration =
            AgentSettingsState.AgentCliConfiguration().apply {
                name = "ACP"
                binaryPath = "agent"
                workingDirectory = hostBasePath
                executionTarget = AgentSettingsState.ExecutionTarget.LOCAL.name
                environmentVariables = linkedMapOf("API_KEY" to "secret")
            }

        val plan =
            AcpProcessLauncher
                .buildLaunchPlan(
                    projectContext = AgentProjectContext(basePath = hostBasePath),
                    configuration = configuration,
                    launchContext = AgentLaunchContext(),
                ).getOrThrow()

        assertEquals(mapOf("API_KEY" to "secret"), plan.environmentVariables)
        assertEquals(emptyList(), plan.mcpServers)
        assertFalse(plan.exposeMcp)
    }

    @Test
    fun `embeds environment variables into wsl command`() {
        val hostBasePath = System.getProperty("user.home")
        val configuration =
            AgentSettingsState.AgentCliConfiguration().apply {
                name = "Pi"
                binaryPath = "pi"
                arguments = ""
                workingDirectory = "/home/olli/project"
                executionTarget = AgentSettingsState.ExecutionTarget.WSL.name
                environmentVariables = linkedMapOf("API_KEY" to "secret")
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
                "--cd",
                "/home/olli/project",
                "--",
                "env",
                "API_KEY=secret",
                "pi",
            ),
            plan.command,
        )
        assertTrue(plan.environmentVariables.isEmpty())
    }

    @Test
    fun `marks expose mcp when bridge resolves servers`() {
        val configuration =
            AgentSettingsState.AgentCliConfiguration().apply {
                name = "ACP"
                binaryPath = "agent"
                workingDirectory = System.getProperty("user.home")
                launchMode = LaunchMode.ACP_CLIENT.name
                useCustomMcp = true
            }
        val bridge =
            object : com.oaalto.agent.acp.mcp.McpCapabilityBridge {
                override fun resolveServers(configuration: AgentSettingsState.AgentCliConfiguration) =
                    listOf(
                        com.agentclientprotocol.model.McpServer.Stdio(
                            "custom",
                            "npx",
                            emptyList(),
                            emptyList(),
                        ),
                    )

                override fun shouldExposeMcp(configuration: AgentSettingsState.AgentCliConfiguration) = true
            }

        val plan =
            AcpProcessLauncher
                .buildLaunchPlan(
                    projectContext = AgentProjectContext(basePath = System.getProperty("user.home")),
                    configuration = configuration,
                    launchContext = AgentLaunchContext(),
                    mcpCapabilityBridge = bridge,
                ).getOrThrow()

        assertTrue(plan.exposeMcp)
        assertEquals(1, plan.mcpServers.size)
    }
}
