package com.oaalto.agent.worktree

import com.oaalto.agent.settings.AgentSettingsState
import com.oaalto.agent.worktree.resume.LaunchResumePlan
import com.oaalto.agent.worktree.resume.ResumeCapability
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WorktreeLaunchCoordinatorTest {
    @Test
    fun `pty resume context includes continue args`() {
        val worktreeDir = Files.createTempDirectory("wt-pty").toFile().apply { deleteOnExit() }
        val projectDir = Files.createTempDirectory("wt-proj").toFile().apply { deleteOnExit() }
        val configuration =
            AgentSettingsState.AgentCliConfiguration().apply {
                id = "cfg-pty"
                binaryPath = "cursor-agent"
                launchMode = "PTY_PASSTHROUGH"
            }
        val context =
            WorktreeLaunchCoordinator
                .buildResumeContext(
                    configuration = configuration,
                    worktreeRecord = null,
                    worktreePath = worktreeDir.absolutePath,
                    resume = true,
                    projectBasePath = projectDir.absolutePath,
                ).getOrThrow()
        val plan = WorktreeLaunchCoordinator.strategyFor(configuration).prepareLaunch(context)
        assertEquals(LaunchResumePlan.Pty(listOf("--continue")), plan)
    }

    @Test
    fun `acp resume context loads stored session id`() {
        val worktreeDir = Files.createTempDirectory("wt-acp").toFile().apply { deleteOnExit() }
        val projectDir = Files.createTempDirectory("wt-acp-proj").toFile().apply { deleteOnExit() }
        val configuration =
            AgentSettingsState.AgentCliConfiguration().apply {
                id = "cfg-acp"
                binaryPath = "cursor-agent"
                launchMode = "ACP_CLIENT"
            }
        val record =
            AgentWorktreeStateService.ManagedWorktreeRecord(
                id = "record-1",
                configurationId = "cfg-acp",
                configurationName = "ACP Agent",
                repositoryRootPath = projectDir.absolutePath,
                worktreePath = worktreeDir.absolutePath,
                branchName = "agent/a/1",
                acpSessionId = "session-99",
                createdAtEpochMs = 1,
                lastUsedAtEpochMs = 1,
                deleted = false,
            )
        val context =
            WorktreeLaunchCoordinator
                .buildResumeContext(
                    configuration = configuration,
                    worktreeRecord = record,
                    worktreePath = worktreeDir.absolutePath,
                    resume = true,
                    projectBasePath = projectDir.absolutePath,
                ).getOrThrow()
        val plan = WorktreeLaunchCoordinator.strategyFor(configuration).prepareLaunch(context)
        assertEquals(LaunchResumePlan.AcpLoad("session-99"), plan)
    }

    @Test
    fun `resume capability is true for acp configurations`() {
        val configuration =
            AgentSettingsState.AgentCliConfiguration().apply {
                launchMode = "ACP_CLIENT"
                binaryPath = "unknown-agent"
            }
        assertTrue(ResumeCapability.canResumeSessions(configuration))
    }

    @Test
    fun `wsl resume context uses kernel resolved paths`() {
        val worktreeDir = Files.createTempDirectory("wt-wsl").toFile().apply { deleteOnExit() }
        val projectDir = Files.createTempDirectory("wt-wsl-proj").toFile().apply { deleteOnExit() }
        val configuration =
            AgentSettingsState.AgentCliConfiguration().apply {
                id = "cfg-wsl"
                binaryPath = "cursor-agent"
                launchMode = "PTY_PASSTHROUGH"
                executionTarget = AgentSettingsState.ExecutionTarget.WSL.name
                wslDistribution = "Ubuntu"
            }
        val context =
            WorktreeLaunchCoordinator
                .buildResumeContext(
                    configuration = configuration,
                    worktreeRecord = null,
                    worktreePath = worktreeDir.absolutePath,
                    resume = true,
                    projectBasePath = projectDir.absolutePath,
                ).getOrThrow()
        assertEquals(projectDir.absolutePath, context.workingDirectory)
        assertEquals("Ubuntu", context.wslDistribution)
        // Worktree path is a Windows drive path; kernel maps it to /mnt/c/... for WSL.
        assertTrue(
            context.wslWorkingDirectory!!.startsWith("/mnt/"),
            "expected /mnt/ mapped path, got: ${context.wslWorkingDirectory}",
        )
    }
}
