package com.oaalto.agent.worktree

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AgentWorktreeStateServiceTest {
    @Test
    fun `round trips acpSessionId on managed worktree records`() {
        val service = AgentWorktreeStateService()
        val record =
            service.saveRecord(
                configurationId = "cfg-a",
                configurationName = "Agent A",
                repositoryRootPath = "/repo",
                worktreePath = "/repo/worktree-a",
                branchName = "agent/a/1",
            )
        assertNull(record.acpSessionId)

        assertTrue(service.setAcpSessionId(record.id, "session-1"))
        val updated = service.getRecordByPath("/repo/worktree-a")
        assertEquals("session-1", updated?.acpSessionId)

        val reloaded = AgentWorktreeStateService()
        reloaded.loadState(service.state)
        assertEquals("session-1", reloaded.getRecordByPath("/repo/worktree-a")?.acpSessionId)
    }

    @Test
    fun `stores independent session ids per worktree`() {
        val service = AgentWorktreeStateService()
        val first =
            service.saveRecord(
                configurationId = "cfg-a",
                configurationName = "Agent A",
                repositoryRootPath = "/repo",
                worktreePath = "/repo/worktree-a",
                branchName = "agent/a/1",
            )
        val second =
            service.saveRecord(
                configurationId = "cfg-a",
                configurationName = "Agent A",
                repositoryRootPath = "/repo",
                worktreePath = "/repo/worktree-b",
                branchName = "agent/a/2",
            )

        service.setAcpSessionId(first.id, "session-a")
        service.setAcpSessionId(second.id, "session-b")

        assertEquals("session-a", service.getRecordByPath(first.worktreePath)?.acpSessionId)
        assertEquals("session-b", service.getRecordByPath(second.worktreePath)?.acpSessionId)
    }

    @Test
    fun `clears session id when configuration changes on existing worktree`() {
        val service = AgentWorktreeStateService()
        val record =
            service.saveRecord(
                configurationId = "cfg-a",
                configurationName = "Agent A",
                repositoryRootPath = "/repo",
                worktreePath = "/repo/worktree-a",
                branchName = "agent/a/1",
            )
        service.setAcpSessionId(record.id, "session-a")

        service.saveRecord(
            configurationId = "cfg-b",
            configurationName = "Agent B",
            repositoryRootPath = "/repo",
            worktreePath = "/repo/worktree-a",
            branchName = "agent/a/1",
        )

        assertNull(service.getRecordByPath(record.worktreePath)?.acpSessionId)
    }

    @Test
    fun `clears session id when worktree is deleted`() {
        val service = AgentWorktreeStateService()
        val record =
            service.saveRecord(
                configurationId = "cfg-a",
                configurationName = "Agent A",
                repositoryRootPath = "/repo",
                worktreePath = "/repo/worktree-a",
                branchName = "agent/a/1",
            )
        service.setAcpSessionId(record.id, "session-a")

        service.markDeletedById(record.id)

        val stored = service.state.records.first { it.id == record.id }
        assertTrue(stored.deleted)
        assertEquals("", stored.acpSessionId)
    }

    @Test
    fun `pending launch round trip preserves fields`() {
        val service = AgentWorktreeStateService()
        service.enqueuePendingLaunch(
            worktreePath = "/repo/worktree-a",
            configurationId = "cfg-a",
            configurationName = "Agent A",
            resume = true,
        )

        val pending = service.consumePendingLaunch("/repo/worktree-a")

        assertEquals("/repo/worktree-a", pending?.worktreePath)
        assertEquals("cfg-a", pending?.configurationId)
        assertEquals("Agent A", pending?.configurationName)
        assertTrue(pending?.resume ?: false)
        assertTrue((pending?.createdAtEpochMs ?: 0L) > 0L)
    }

    @Test
    fun `consume removes pending launch`() {
        val service = AgentWorktreeStateService()
        service.enqueuePendingLaunch(
            worktreePath = "/repo/worktree-a",
            configurationId = "cfg-a",
            configurationName = "Agent A",
            resume = false,
        )

        service.consumePendingLaunch("/repo/worktree-a")

        assertNull(service.consumePendingLaunch("/repo/worktree-a"))
    }

    @Test
    fun `re-enqueue replaces duplicate pending launch`() {
        val service = AgentWorktreeStateService()
        service.enqueuePendingLaunch(
            worktreePath = "/repo/worktree-a",
            configurationId = "cfg-a",
            configurationName = "Agent A",
            resume = false,
        )
        service.enqueuePendingLaunch(
            worktreePath = "/repo/worktree-a",
            configurationId = "cfg-b",
            configurationName = "Agent B",
            resume = true,
        )
        assertEquals(1, service.state.pendingLaunches.size)

        val pending = service.consumePendingLaunch("/repo/worktree-a")

        assertEquals("cfg-b", pending?.configurationId)
        assertEquals("Agent B", pending?.configurationName)
        assertTrue(pending?.resume ?: false)
        assertEquals(0, service.state.pendingLaunches.size)
    }
}
