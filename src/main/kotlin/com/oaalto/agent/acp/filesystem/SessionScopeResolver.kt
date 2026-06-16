package com.oaalto.agent.acp.filesystem

import com.oaalto.agent.worktree.AgentWorktreePathMapper
import java.nio.file.Path

object SessionScopeResolver {
    fun hostScopeRoot(
        sessionWorkingDirectory: String,
        projectBasePath: String?,
        workingDirectoryOverride: String?,
    ): Path {
        val override = workingDirectoryOverride?.trim().orEmpty()
        if (override.isNotBlank()) {
            return Path.of(override)
        }
        val sessionCwd = sessionWorkingDirectory.trim()
        val basePath = projectBasePath?.trim().orEmpty()
        if (sessionCwd.startsWith("/") && basePath.isNotBlank()) {
            val hostPath = AgentWorktreePathMapper.mapGitPathToHostPath(sessionCwd, basePath)
            if (hostPath != sessionCwd) {
                return Path.of(hostPath)
            }
        }
        if (sessionCwd.isNotBlank()) {
            return Path.of(sessionCwd)
        }
        if (basePath.isNotBlank()) {
            return Path.of(basePath)
        }
        return Path.of(System.getProperty("user.dir"))
    }

    fun normalizeAgentPath(
        requestedPath: String,
        scopeRoot: Path,
        projectBasePath: String?,
    ): String {
        val trimmed = requestedPath.trim()
        if (!trimmed.startsWith("/")) return trimmed
        val basePath = projectBasePath?.trim().orEmpty()
        if (basePath.isBlank()) return trimmed
        val rootString = scopeRoot.normalize().toAbsolutePath().toString()
        if (AgentWorktreePathMapper.parseWslUncPath(rootString) != null ||
            AgentWorktreePathMapper.parseWslUncPath(basePath) != null
        ) {
            return AgentWorktreePathMapper.mapGitPathToHostPath(trimmed, basePath)
        }
        return trimmed
    }
}
