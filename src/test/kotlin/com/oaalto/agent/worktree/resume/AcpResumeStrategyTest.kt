package com.oaalto.agent.worktree.resume

import com.oaalto.agent.settings.AgentSettingsState
import com.oaalto.agent.worktree.AgentWorktreeStateService
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class AcpResumeStrategyTest {
    @Test
    fun `resume false starts a new session`() {
        val plan =
            AcpResumeStrategy.prepareLaunch(
                context(
                    resume = false,
                    acpSessionId = "stored",
                ),
            )
        assertIs<LaunchResumePlan.AcpNewSession>(plan)
    }

    @Test
    fun `stored session id loads when resume is true`() {
        val plan =
            AcpResumeStrategy.prepareLaunch(
                context(
                    resume = true,
                    acpSessionId = "session-42",
                ),
            )
        assertEquals(LaunchResumePlan.AcpLoad("session-42"), plan)
    }

    @Test
    fun `missing session id opens picker plan`() {
        val plan =
            AcpResumeStrategy.prepareLaunch(
                context(
                    resume = true,
                    acpSessionId = null,
                ),
            )
        assertEquals(LaunchResumePlan.AcpResolveSession, plan)
    }

    @Test
    fun `configuration mismatch starts a new session instead of reusing id`() {
        val plan =
            AcpResumeStrategy.prepareLaunch(
                context(
                    resume = true,
                    configurationId = "cfg-b",
                    recordConfigurationId = "cfg-a",
                    acpSessionId = "session-42",
                ),
            )
        assertIs<LaunchResumePlan.AcpNewSession>(plan)
    }

    private fun context(
        resume: Boolean,
        acpSessionId: String? = null,
        configurationId: String = "cfg-a",
        recordConfigurationId: String = configurationId,
    ): ResumeContext {
        val record =
            AgentWorktreeStateService.ManagedWorktreeRecord(
                id = "record-1",
                configurationId = recordConfigurationId,
                configurationName = "Agent",
                repositoryRootPath = "/repo",
                worktreePath = "/repo/worktree",
                branchName = "agent/a/1",
                acpSessionId = acpSessionId,
                createdAtEpochMs = 1,
                lastUsedAtEpochMs = 1,
                deleted = false,
            )
        return ResumeContext(
            configuration =
                AgentSettingsState.AgentCliConfiguration().apply {
                    id = configurationId
                    launchMode = "ACP_CLIENT"
                },
            worktreeRecord = record,
            workingDirectory = "/repo/worktree",
            resume = resume,
        )
    }
}
