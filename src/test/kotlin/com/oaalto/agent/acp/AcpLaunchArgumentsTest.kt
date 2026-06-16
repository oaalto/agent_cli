package com.oaalto.agent.acp

import com.oaalto.agent.AgentLaunchContext
import com.oaalto.agent.settings.AgentSettingsState
import kotlin.test.Test
import kotlin.test.assertEquals

class AcpLaunchArgumentsTest {
    @Test
    fun `injects acp subcommand for cursor agent binaries`() {
        val arguments =
            AcpLaunchArguments.resolve(
                configuration =
                    AgentSettingsState.AgentCliConfiguration().apply {
                        binaryPath = "cursor-agent"
                        arguments = ""
                    },
                launchContext = AgentLaunchContext(),
            )

        assertEquals(listOf("acp"), arguments)
    }

    @Test
    fun `does not duplicate acp when already configured`() {
        val arguments =
            AcpLaunchArguments.resolve(
                configuration =
                    AgentSettingsState.AgentCliConfiguration().apply {
                        binaryPath = "C:\\tools\\agent.exe"
                        arguments = "acp"
                    },
                launchContext = AgentLaunchContext(),
            )

        assertEquals(listOf("acp"), arguments)
    }

    @Test
    fun `strips pty resume flags and injects acp entry arguments`() {
        val arguments =
            AcpLaunchArguments.resolve(
                configuration =
                    AgentSettingsState.AgentCliConfiguration().apply {
                        binaryPath = "agent"
                        arguments = "--continue --model fast"
                    },
                launchContext = AgentLaunchContext(additionalArguments = listOf("--resume")),
            )

        assertEquals(listOf("acp", "--model", "fast"), arguments)
    }

    @Test
    fun `leaves unknown binaries unchanged aside from resume stripping`() {
        val arguments =
            AcpLaunchArguments.resolve(
                configuration =
                    AgentSettingsState.AgentCliConfiguration().apply {
                        binaryPath = "custom-agent"
                        arguments = "--continue --verbose"
                    },
                launchContext = AgentLaunchContext(),
            )

        assertEquals(listOf("--verbose"), arguments)
    }
}
