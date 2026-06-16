package com.oaalto.agent.worktree

import com.oaalto.agent.settings.AgentSettingsState
import com.oaalto.agent.worktree.resume.LaunchResumePlan
import com.oaalto.agent.worktree.resume.ResumeCapability
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WorktreeLaunchCoordinatorTest {
    @Test
    fun `pty resume context includes continue args`() {
        val configuration =
            AgentSettingsState.AgentCliConfiguration().apply {
                id = "cfg-pty"
                binaryPath = "cursor-agent"
                launchMode = "PTY_PASSTHROUGH"
            }
        val context =
            WorktreeLaunchCoordinator.buildResumeContext(
                configuration = configuration,
                worktreeRecord = null,
                worktreePath = "/repo/worktree",
                resume = true,
                projectBasePath = "/repo",
            )
        val plan = WorktreeLaunchCoordinator.strategyFor(configuration).prepareLaunch(context)
        assertEquals(LaunchResumePlan.Pty(listOf("--continue")), plan)
    }

    @Test
    fun `acp resume context loads stored session id`() {
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
                repositoryRootPath = "/repo",
                worktreePath = "/repo/worktree",
                branchName = "agent/a/1",
                acpSessionId = "session-99",
                createdAtEpochMs = 1,
                lastUsedAtEpochMs = 1,
                deleted = false,
            )
        val context =
            WorktreeLaunchCoordinator.buildResumeContext(
                configuration = configuration,
                worktreeRecord = record,
                worktreePath = "/repo/worktree",
                resume = true,
                projectBasePath = "/repo",
            )
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
}
