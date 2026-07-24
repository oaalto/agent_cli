package com.oaalto.agent

import com.oaalto.agent.settings.AgentSettingsState
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AgentLaunchResolverTest {
    @Test
    fun `override wins over configured over project base`() {
        val tempDir = Files.createTempDirectory("resolver-precedence").toFile()
        tempDir.deleteOnExit()

        val config = AgentSettingsState.AgentCliConfiguration()
        config.workingDirectory = "configured-dir"

        val result =
            AgentLaunchResolver
                .resolveLaunchInputs(
                    configuration = config,
                    projectBasePath = "project-base",
                    workingDirectoryOverride = tempDir.absolutePath,
                )

        assertTrue(result.isSuccess)
        val inputs = result.getOrThrow()
        assertEquals(tempDir.absolutePath, inputs.workingDirectory)
    }

    @Test
    fun `configured wins over project base when no override`() {
        val tempDir = Files.createTempDirectory("resolver-configured").toFile()
        tempDir.deleteOnExit()

        val config = AgentSettingsState.AgentCliConfiguration()
        config.workingDirectory = tempDir.absolutePath

        val result =
            AgentLaunchResolver
                .resolveLaunchInputs(
                    configuration = config,
                    projectBasePath = "project-base",
                    workingDirectoryOverride = null,
                )

        assertTrue(result.isSuccess)
        assertEquals(tempDir.absolutePath, result.getOrThrow().workingDirectory)
    }

    @Test
    fun `local resolution returns Local variant with validated path`() {
        val tempDir = Files.createTempDirectory("resolver-local").toFile()
        tempDir.deleteOnExit()

        val config = AgentSettingsState.AgentCliConfiguration()
        config.workingDirectory = tempDir.absolutePath

        val result =
            AgentLaunchResolver
                .resolveLaunchInputs(
                    configuration = config,
                    projectBasePath = null,
                    workingDirectoryOverride = null,
                )

        assertTrue(result.isSuccess)
        val inputs = result.getOrThrow()
        assertTrue(inputs is ResolvedLaunchInputs.Local)
        assertEquals(tempDir.absolutePath, inputs.workingDirectory)
    }

    @Test
    fun `local failure returns failure for non existent directory`() {
        val config = AgentSettingsState.AgentCliConfiguration()
        config.workingDirectory = "nonexistent-directory-xyz"

        val result =
            AgentLaunchResolver
                .resolveLaunchInputs(
                    configuration = config,
                    projectBasePath = null,
                    workingDirectoryOverride = null,
                )

        assertTrue(result.isFailure)
    }

    @Test
    fun `wsl unc path maps correctly`() {
        val config = AgentSettingsState.AgentCliConfiguration()
        config.workingDirectory = "\\\\wsl.localhost\\Ubuntu\\home\\user\\project"
        config.wslDistribution = ""
        config.executionTarget = AgentSettingsState.ExecutionTarget.WSL.name

        val result =
            AgentLaunchResolver
                .resolveLaunchInputs(
                    configuration = config,
                    projectBasePath = null,
                    workingDirectoryOverride = null,
                )

        assertTrue(result.isSuccess)
        val inputs = result.getOrThrow() as ResolvedLaunchInputs.Wsl
        assertEquals("/home/user/project", inputs.linuxPath)
        assertEquals("Ubuntu", inputs.wslDistribution)
    }

    @Test
    fun `wsl linux path maps correctly`() {
        val config = AgentSettingsState.AgentCliConfiguration()
        config.workingDirectory = "/home/olli/project"
        config.wslDistribution = ""
        config.executionTarget = AgentSettingsState.ExecutionTarget.WSL.name

        val result =
            AgentLaunchResolver
                .resolveLaunchInputs(
                    configuration = config,
                    projectBasePath = null,
                    workingDirectoryOverride = null,
                )

        assertTrue(result.isSuccess)
        val inputs = result.getOrThrow() as ResolvedLaunchInputs.Wsl
        assertEquals("/home/olli/project", inputs.linuxPath)
    }

    @Test
    fun `wsl drive letter maps to mnt`() {
        val config = AgentSettingsState.AgentCliConfiguration()
        config.workingDirectory = "D:\\project"
        config.wslDistribution = ""
        config.executionTarget = AgentSettingsState.ExecutionTarget.WSL.name

        val result =
            AgentLaunchResolver
                .resolveLaunchInputs(
                    configuration = config,
                    projectBasePath = null,
                    workingDirectoryOverride = null,
                )

        assertTrue(result.isSuccess)
        val inputs = result.getOrThrow() as ResolvedLaunchInputs.Wsl
        assertEquals("/mnt/d/project", inputs.linuxPath)
    }

    @Test
    fun `wsl mapping failure returns failure with hint text`() {
        val config = AgentSettingsState.AgentCliConfiguration()
        config.workingDirectory = "some/random/not/mappable/path"
        config.wslDistribution = ""
        config.executionTarget = AgentSettingsState.ExecutionTarget.WSL.name

        val result =
            AgentLaunchResolver
                .resolveLaunchInputs(
                    configuration = config,
                    projectBasePath = null,
                    workingDirectoryOverride = null,
                )

        assertTrue(result.isFailure)
        val message = result.exceptionOrNull()?.message ?: error("Expected error message but got null")
        assertTrue(message.contains("could not be mapped to a WSL path"), "Missing main error text: $message")
        assertTrue(message.contains("/home/user/project"), "Missing Linux path hint: $message")
        assertTrue(message.contains("wsl.localhost"), "Missing UNC hint: $message")
        assertTrue(message.contains("D:\\project"), "Missing drive letter hint: $message")
    }

    @Test
    fun `blank raw path falls back to home`() {
        val config = AgentSettingsState.AgentCliConfiguration()
        config.executionTarget = AgentSettingsState.ExecutionTarget.WSL.name

        val result =
            AgentLaunchResolver
                .resolveLaunchInputs(
                    configuration = config,
                    projectBasePath = null,
                    workingDirectoryOverride = null,
                )

        assertTrue(result.isSuccess)
        val inputs = result.getOrThrow() as ResolvedLaunchInputs.Wsl
        assertEquals("/home", inputs.linuxPath)
    }

    @Test
    fun `distribution uses explicit config over inferred`() {
        val config = AgentSettingsState.AgentCliConfiguration()
        config.workingDirectory = "\\\\wsl.localhost\\Debian\\home"
        config.wslDistribution = "Ubuntu"
        config.executionTarget = AgentSettingsState.ExecutionTarget.WSL.name

        val result =
            AgentLaunchResolver
                .resolveLaunchInputs(
                    configuration = config,
                    projectBasePath = null,
                    workingDirectoryOverride = null,
                )

        assertTrue(result.isSuccess)
        val inputs = result.getOrThrow() as ResolvedLaunchInputs.Wsl
        assertEquals("Ubuntu", inputs.wslDistribution)
    }

    @Test
    fun `distribution inferred from unc when config blank`() {
        val config = AgentSettingsState.AgentCliConfiguration()
        config.workingDirectory = "\\\\wsl.localhost\\Debian\\home"
        config.wslDistribution = ""
        config.executionTarget = AgentSettingsState.ExecutionTarget.WSL.name

        val result =
            AgentLaunchResolver
                .resolveLaunchInputs(
                    configuration = config,
                    projectBasePath = null,
                    workingDirectoryOverride = null,
                )

        assertTrue(result.isSuccess)
        val inputs = result.getOrThrow() as ResolvedLaunchInputs.Wsl
        assertEquals("Debian", inputs.wslDistribution)
    }

    @Test
    fun `unknown execution target defaults to local`() {
        val tempDir = Files.createTempDirectory("resolver-unknown-target").toFile()
        tempDir.deleteOnExit()

        val config = AgentSettingsState.AgentCliConfiguration()
        config.workingDirectory = tempDir.absolutePath
        config.executionTarget = "INVALID_TARGET"

        val result =
            AgentLaunchResolver
                .resolveLaunchInputs(
                    configuration = config,
                    projectBasePath = null,
                    workingDirectoryOverride = null,
                )

        assertTrue(result.isSuccess)
        val inputs = result.getOrThrow()
        assertEquals(AgentSettingsState.ExecutionTarget.LOCAL, inputs.executionTarget)
        assertTrue(inputs is ResolvedLaunchInputs.Local)
    }

    @Test
    fun `host cwd falls back to user home when no valid project base`() {
        val config = AgentSettingsState.AgentCliConfiguration()
        config.workingDirectory = "\\\\wsl.localhost\\Ubuntu\\home"
        config.wslDistribution = ""
        config.executionTarget = AgentSettingsState.ExecutionTarget.WSL.name

        val result =
            AgentLaunchResolver
                .resolveLaunchInputs(
                    configuration = config,
                    projectBasePath = null,
                    workingDirectoryOverride = null,
                )

        assertTrue(result.isSuccess)
        val inputs = result.getOrThrow() as ResolvedLaunchInputs.Wsl
        assertEquals(System.getProperty("user.home"), inputs.hostWorkingDirectory)
    }
}
