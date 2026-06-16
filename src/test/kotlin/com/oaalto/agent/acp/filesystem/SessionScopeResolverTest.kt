package com.oaalto.agent.acp.filesystem

import kotlin.test.Test
import kotlin.test.assertEquals

class SessionScopeResolverTest {
    @Test
    fun `maps linux session cwd to wsl host scope root`() {
        val scopeRoot =
            SessionScopeResolver.hostScopeRoot(
                sessionWorkingDirectory = "/home/olli/agent_cli",
                projectBasePath = """\\wsl.localhost\Ubuntu\home\olli\agent_cli""",
                workingDirectoryOverride = null,
            )

        assertEquals("""\\wsl.localhost\Ubuntu\home\olli\agent_cli""", scopeRoot.toString())
    }

    @Test
    fun `prefers working directory override`() {
        val scopeRoot =
            SessionScopeResolver.hostScopeRoot(
                sessionWorkingDirectory = "/home/olli/agent_cli",
                projectBasePath = """\\wsl.localhost\Ubuntu\home\olli\agent_cli""",
                workingDirectoryOverride = "C:\\worktrees\\run-1",
            )

        assertEquals("C:\\worktrees\\run-1", scopeRoot.toString())
    }

    @Test
    fun `normalizes linux absolute agent paths for wsl scope`() {
        val scopeRoot =
            SessionScopeResolver.hostScopeRoot(
                sessionWorkingDirectory = "/home/olli/agent_cli",
                projectBasePath = """\\wsl.localhost\Ubuntu\home\olli\agent_cli""",
                workingDirectoryOverride = null,
            )
        val normalized =
            SessionScopeResolver.normalizeAgentPath(
                requestedPath = "/home/olli/agent_cli/src/main.kt",
                scopeRoot = scopeRoot,
                projectBasePath = """\\wsl.localhost\Ubuntu\home\olli\agent_cli""",
            )

        assertEquals("""\\wsl.localhost\Ubuntu\home\olli\agent_cli\src\main.kt""", normalized)
    }
}
