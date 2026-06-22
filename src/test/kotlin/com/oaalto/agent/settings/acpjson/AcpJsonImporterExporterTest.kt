package com.oaalto.agent.settings.acpjson

import com.oaalto.agent.settings.AgentSettingsState
import com.oaalto.agent.settings.LaunchMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private data class TestConfig(
    val name: String,
    val launchMode: LaunchMode = LaunchMode.ACP_CLIENT,
    val binaryPath: String,
    val arguments: String = "",
    val env: Map<String, String> = emptyMap(),
    val useIdeaMcp: Boolean = false,
    val useCustomMcp: Boolean = false,
    val executionTarget: String = AgentSettingsState.ExecutionTarget.LOCAL.name,
    val wslDistribution: String = "",
    val useNodeShellWrapper: Boolean = false,
)

class AcpJsonImporterExporterTest {
    @Test
    fun `exports only acp client configurations`() {
        val exported =
            AcpJsonExporter.export(
                listOf(
                    configuration(
                        TestConfig(
                            name = "PTY",
                            launchMode = LaunchMode.PTY_PASSTHROUGH,
                            binaryPath = "/bin/pty",
                        ),
                    ),
                    configuration(
                        TestConfig(
                            name = "ACP",
                            launchMode = LaunchMode.ACP_CLIENT,
                            binaryPath = "/bin/acp",
                            arguments = "--model fast",
                            env = mapOf("TOKEN" to "abc"),
                            useIdeaMcp = true,
                        ),
                    ),
                ),
            )

        assertTrue(exported.contains("\"ACP\""))
        assertFalse(exported.contains("\"PTY\""))
        assertTrue(exported.contains("\"command\": \"/bin/acp\""))
        assertTrue(exported.contains("\"TOKEN\": \"abc\""))
        assertTrue(exported.contains("\"use_idea_mcp\": true"))
    }

    @Test
    fun `imports agent servers into acp client configurations`() {
        val drafts =
            AcpJsonImporter
                .parse(
                    """
                    {
                      "agent_servers": {
                        "Pi": {
                          "command": "/usr/bin/pi",
                          "args": ["acp", "--model", "fast"],
                          "env": { "API_KEY": "secret" },
                          "use_custom_mcp": true
                        }
                      }
                    }
                    """.trimIndent(),
                ).getOrThrow()

        val imported = drafts.single()
        assertEquals("Pi", imported.name)
        assertEquals(LaunchMode.ACP_CLIENT.name, imported.configuration.launchMode)
        assertEquals("/usr/bin/pi", imported.configuration.binaryPath)
        assertEquals("acp --model fast", imported.configuration.arguments)
        assertEquals(mapOf("API_KEY" to "secret"), imported.configuration.environmentVariables)
        assertTrue(imported.configuration.useCustomMcp)
    }

    @Test
    fun `merge prompts overwrite only selected collisions`() {
        val existing =
            listOf(
                configuration(TestConfig(name = "Pi", binaryPath = "/old/pi")),
                configuration(TestConfig(name = "Other", binaryPath = "/other")),
            )
        val drafts =
            listOf(
                AcpJsonImportDraft(
                    name = "Pi",
                    configuration = configuration(TestConfig(name = "Pi", binaryPath = "/new/pi")),
                ),
                AcpJsonImportDraft(
                    name = "Fresh",
                    configuration = configuration(TestConfig(name = "Fresh", binaryPath = "/fresh")),
                ),
            )

        val merged = AcpJsonImporter.merge(drafts, existing, overwriteNames = setOf("Pi"))

        assertEquals(3, merged.configurations.size)
        assertEquals("/new/pi", merged.configurations.first { it.name == "Pi" }.binaryPath)
        assertEquals("/other", merged.configurations.first { it.name == "Other" }.binaryPath)
        assertEquals("/fresh", merged.configurations.first { it.name == "Fresh" }.binaryPath)
    }

    @Test
    fun `parse errors are actionable`() {
        val failure =
            AcpJsonImporter.parse(
                """
                { "agent_servers": { "Broken": { "args": [] } } }
                """.trimIndent(),
            )

        assertFailsWith<IllegalArgumentException> {
            failure.getOrThrow()
        }
    }

    @Test
    fun `exports wsl and node wrapper metadata`() {
        val exported =
            AcpJsonExporter.export(
                listOf(
                    configuration(
                        TestConfig(
                            name = "WSL",
                            binaryPath = "pi",
                            executionTarget = AgentSettingsState.ExecutionTarget.WSL.name,
                            wslDistribution = "Ubuntu",
                            useNodeShellWrapper = true,
                        ),
                    ),
                ),
            )

        assertTrue(exported.contains("\"execution_target\": \"WSL\""))
        assertTrue(exported.contains("\"wsl_distribution\": \"Ubuntu\""))
        assertTrue(exported.contains("\"use_node_shell_wrapper\": true"))
    }

    @Test
    fun `imports wsl and node wrapper metadata`() {
        val drafts =
            AcpJsonImporter
                .parse(
                    """
                    {
                      "agent_servers": {
                        "Pi": {
                          "command": "pi",
                          "args": ["acp"],
                          "execution_target": "WSL",
                          "wsl_distribution": "Ubuntu",
                          "use_node_shell_wrapper": true
                        }
                      }
                    }
                    """.trimIndent(),
                ).getOrThrow()

        val configuration = drafts.single().configuration
        assertEquals(AgentSettingsState.ExecutionTarget.WSL.name, configuration.executionTarget)
        assertEquals("Ubuntu", configuration.wslDistribution)
        assertTrue(configuration.useNodeShellWrapper)
    }

    private fun configuration(config: TestConfig): AgentSettingsState.AgentCliConfiguration =
        AgentSettingsState.AgentCliConfiguration().apply {
            name = config.name
            launchMode = config.launchMode.name
            binaryPath = config.binaryPath
            arguments = config.arguments
            environmentVariables = LinkedHashMap(config.env)
            useIdeaMcp = config.useIdeaMcp
            useCustomMcp = config.useCustomMcp
            executionTarget = config.executionTarget
            wslDistribution = config.wslDistribution
            useNodeShellWrapper = config.useNodeShellWrapper
        }
}
