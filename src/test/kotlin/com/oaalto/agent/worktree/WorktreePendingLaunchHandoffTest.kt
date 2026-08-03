package com.oaalto.agent.worktree

import com.intellij.openapi.project.Project
import com.oaalto.agent.AgentLaunchContext
import com.oaalto.agent.settings.AgentSettingsState
import java.lang.reflect.Proxy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WorktreePendingLaunchHandoffTest {
    @Test
    fun `scheduleLaunch rolls back pending launch when open fails`() {
        val state = AgentWorktreeStateService()

        val result =
            WorktreePendingLaunchHandoff.scheduleLaunch(
                worktreePath = "/repo/worktree-a",
                configurationId = "cfg-a",
                configurationName = "Agent A",
                resume = true,
                seams =
                    ScheduleLaunchSeams(
                        stateService = state,
                        openWorktreeProject = { Result.failure(IllegalStateException("open failed")) },
                    ),
            )

        assertTrue(result.isFailure)
        assertNull(state.consumePendingLaunch("/repo/worktree-a"))
    }

    @Test
    fun `scheduleLaunch leaves pending launch when open succeeds`() {
        val state = AgentWorktreeStateService()

        val result =
            WorktreePendingLaunchHandoff.scheduleLaunch(
                worktreePath = "/repo/worktree-a",
                configurationId = "cfg-a",
                configurationName = "Agent A",
                resume = false,
                seams =
                    ScheduleLaunchSeams(
                        stateService = state,
                        openWorktreeProject = { Result.success(Unit) },
                    ),
            )

        assertTrue(result.isSuccess)
        val pending = state.consumePendingLaunch("/repo/worktree-a")
        assertEquals("cfg-a", pending?.configurationId)
        assertEquals("Agent A", pending?.configurationName)
        assertFalse(pending?.resume ?: true)
    }

    @Test
    fun `completePendingLaunchIfAny opens editor and touches record on success`() {
        val state = AgentWorktreeStateService()
        state.saveRecord(
            configurationId = "cfg-a",
            configurationName = "Agent A",
            repositoryRootPath = "/repo",
            worktreePath = "/repo/worktree-a",
            branchName = "agent/a/1",
        )
        val beforeTouch = state.getRecordByPath("/repo/worktree-a")?.lastUsedAtEpochMs ?: 0L
        state.enqueuePendingLaunch(
            worktreePath = "/repo/worktree-a",
            configurationId = "cfg-a",
            configurationName = "Agent A",
            resume = true,
        )
        val project = fakeProject("/repo/worktree-a")
        val configuration =
            AgentSettingsState.AgentCliConfiguration().apply {
                id = "cfg-a"
                name = "Agent A"
                binaryPath = "cursor-agent"
            }
        var editorOpened = false
        var openedResume = false

        WorktreePendingLaunchHandoff.completePendingLaunchIfAny(
            project = project,
            seams =
                CompleteLaunchSeams(
                    stateService = state,
                    getConfiguration = { configurationId ->
                        if (configurationId == "cfg-a") configuration else null
                    },
                    buildLaunchContext = { _, _, _, resume ->
                        openedResume = resume
                        Result.success(AgentLaunchContext(resume = resume))
                    },
                    openEditor = { _, _, _ -> editorOpened = true },
                    showError = { _, _ -> error("unexpected error dialog") },
                    runOnEdt = { it.run() },
                ),
        )

        assertTrue(editorOpened)
        assertTrue(openedResume)
        assertNull(state.consumePendingLaunch("/repo/worktree-a"))
        val afterTouch = state.getRecordByPath("/repo/worktree-a")?.lastUsedAtEpochMs ?: 0L
        assertTrue(afterTouch >= beforeTouch)
    }

    @Test
    fun `completePendingLaunchIfAny shows error when configuration is missing`() {
        val state = AgentWorktreeStateService()
        state.enqueuePendingLaunch(
            worktreePath = "/repo/worktree-a",
            configurationId = "missing",
            configurationName = "Missing",
            resume = false,
        )
        val project = fakeProject("/repo/worktree-a")
        var editorOpened = false
        var errorMessage: String? = null

        WorktreePendingLaunchHandoff.completePendingLaunchIfAny(
            project = project,
            seams =
                CompleteLaunchSeams(
                    stateService = state,
                    getConfiguration = { null },
                    buildLaunchContext = { _, _, _, _ -> Result.success(AgentLaunchContext()) },
                    openEditor = { _, _, _ -> editorOpened = true },
                    showError = { _, message -> errorMessage = message },
                    runOnEdt = { it.run() },
                ),
        )

        assertFalse(editorOpened)
        assertEquals(
            "The selected agent configuration for this worktree no longer exists.",
            errorMessage,
        )
    }

    @Test
    fun `completePendingLaunchIfAny consumes with normalized project base path`() {
        val state = AgentWorktreeStateService()
        state.enqueuePendingLaunch(
            worktreePath = "C:\\repo\\worktree-a",
            configurationId = "cfg-a",
            configurationName = "Agent A",
            resume = false,
        )
        val project = fakeProject("c:/repo/worktree-a")
        val configuration =
            AgentSettingsState.AgentCliConfiguration().apply {
                id = "cfg-a"
                name = "Agent A"
            }
        var consumed = false

        WorktreePendingLaunchHandoff.completePendingLaunchIfAny(
            project = project,
            seams =
                CompleteLaunchSeams(
                    stateService = state,
                    getConfiguration = { configuration },
                    buildLaunchContext = { _, _, _, _ -> Result.success(AgentLaunchContext()) },
                    openEditor = { _, _, _ -> consumed = true },
                    showError = { _, _ -> error("unexpected error dialog") },
                    runOnEdt = { it.run() },
                ),
        )

        assertTrue(consumed)
        assertNull(state.consumePendingLaunch("/repo/worktree-a"))
    }

    @Test
    fun `pending launch path keys normalize between enqueue and consume`() {
        val state = AgentWorktreeStateService()
        state.enqueuePendingLaunch(
            worktreePath = "C:\\repo\\worktree-a",
            configurationId = "cfg-a",
            configurationName = "Agent A",
            resume = true,
        )

        val pending = state.consumePendingLaunch("c:/repo/worktree-a")

        assertEquals("cfg-a", pending?.configurationId)
        assertTrue(pending?.resume ?: false)
    }

    private fun fakeProject(
        basePath: String,
        disposed: Boolean = false,
    ): Project {
        @Suppress("UNCHECKED_CAST")
        return Proxy.newProxyInstance(
            Project::class.java.classLoader,
            arrayOf(Project::class.java),
        ) { _, method, _ ->
            when (method.name) {
                "getBasePath" -> basePath
                "isDisposed" -> disposed
                "hashCode" -> basePath.hashCode()
                "equals" -> false
                "toString" -> "FakeProject($basePath)"
                else -> defaultProxyValue(method.returnType)
            }
        } as Project
    }

    private fun defaultProxyValue(returnType: Class<*>): Any? {
        val primitiveDefaults =
            mapOf(
                "boolean" to false,
                "int" to 0,
                "long" to 0L,
                "short" to 0.toShort(),
                "byte" to 0.toByte(),
                "char" to '\u0000',
                "float" to 0f,
                "double" to 0.0,
                "void" to null,
            )
        return primitiveDefaults[returnType.name] ?: if (returnType.isPrimitive) 0 else null
    }
}
