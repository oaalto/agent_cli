package com.oaalto.agent.worktree

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class AgentWorktreePathMapperTest {
    @Test
    fun `maps WSL UNC host path to Linux git path when distro matches`() {
        val repositoryPath = """\\wsl.localhost\Ubuntu\home\olli\agent_cli"""
        val hostWorktreePath = """\\wsl.localhost\Ubuntu\home\olli\agent_cli-agent-worktrees\run-1"""

        val mapped = AgentWorktreePathMapper.mapHostPathToGitPath(hostWorktreePath, repositoryPath)

        assertEquals("/home/olli/agent_cli-agent-worktrees/run-1", mapped)
    }

    @Test
    fun `keeps host path when WSL distro does not match repository`() {
        val repositoryPath = """\\wsl.localhost\Ubuntu\home\olli\agent_cli"""
        val hostWorktreePath = """\\wsl.localhost\Debian\home\olli\agent_cli-agent-worktrees\run-1"""

        val mapped = AgentWorktreePathMapper.mapHostPathToGitPath(hostWorktreePath, repositoryPath)

        assertEquals(hostWorktreePath, mapped)
    }

    @Test
    fun `maps git Linux path back to WSL UNC when repository is WSL UNC`() {
        val repositoryPath = """\\wsl.localhost\Ubuntu\home\olli\agent_cli"""
        val gitPath = "/home/olli/agent_cli-agent-worktrees/run-2"

        val mapped = AgentWorktreePathMapper.mapGitPathToHostPath(gitPath, repositoryPath)

        assertEquals("""\\wsl.localhost\Ubuntu\home\olli\agent_cli-agent-worktrees\run-2""", mapped)
    }

    @Test
    fun `normalizes wsl-dollar prefix in path keys`() {
        val key = AgentWorktreePathMapper.normalizePathKey("""\\WSL$\Ubuntu\HOME\Olli\Repo""")

        assertEquals("//wsl.localhost/ubuntu/home/olli/repo", key)
    }

    @Test
    fun `parses WSL UNC distribution and linux path`() {
        val parsed = AgentWorktreePathMapper.parseWslUncPath("""\\wsl.localhost\Ubuntu\home\olli\repo""")

        assertNotNull(parsed)
        assertEquals("Ubuntu", parsed.distribution)
        assertEquals("/home/olli/repo", parsed.linuxPath)
    }
}
