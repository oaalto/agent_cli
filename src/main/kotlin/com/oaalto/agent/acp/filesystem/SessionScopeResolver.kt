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
        val mappedHostPath =
            if (sessionCwd.startsWith("/") && basePath.isNotBlank()) {
                val hostPath = AgentWorktreePathMapper.mapGitPathToHostPath(sessionCwd, basePath)
                hostPath.takeIf { it != sessionCwd }
            } else {
                null
            }
        val resolvedPath =
            when {
                mappedHostPath != null -> mappedHostPath
                sessionCwd.isNotBlank() -> sessionCwd
                basePath.isNotBlank() -> basePath
                else -> System.getProperty("user.dir")
            }
        return Path.of(resolvedPath)
    }

    fun normalizeAgentPath(
        requestedPath: String,
        scopeRoot: Path,
        projectBasePath: String?,
    ): String {
        val trimmed = requestedPath.trim()
        val basePath = projectBasePath?.trim().orEmpty()
        return when {
            !trimmed.startsWith("/") -> trimmed
            basePath.isBlank() -> trimmed
            else -> {
                val rootString = scopeRoot.normalize().toAbsolutePath().toString()
                val usesWslPaths =
                    AgentWorktreePathMapper.parseWslUncPath(rootString) != null ||
                        AgentWorktreePathMapper.parseWslUncPath(basePath) != null
                if (usesWslPaths) {
                    AgentWorktreePathMapper.mapGitPathToHostPath(trimmed, basePath)
                } else {
                    trimmed
                }
            }
        }
    }
}
