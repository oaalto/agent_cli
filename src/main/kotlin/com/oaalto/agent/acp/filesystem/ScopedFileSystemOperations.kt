package com.oaalto.agent.acp.filesystem

import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.relativeToOrNull

/**
 * Resolves agent-requested paths against a session scope root and enforces
 * read/write policy without touching the IDE VFS.
 */
class ScopedFileSystemOperations private constructor(
    private val scopeRoot: Path,
    private val projectBasePath: String?,
) {
    constructor(scopeRoot: Path) : this(scopeRoot, null)

    sealed class ScopeResult {
        data class InScope(
            val resolved: Path,
        ) : ScopeResult()

        data class OutOfScope(
            val message: String,
        ) : ScopeResult()
    }

    fun resolveForRead(requestedPath: String): ScopeResult {
        val resolved = resolveInScope(requestedPath) ?: return outOfScope(requestedPath)
        return when {
            !resolved.exists() -> ScopeResult.OutOfScope("File not found: $requestedPath")
            !resolved.isRegularFile() -> ScopeResult.OutOfScope("Not a regular file: $requestedPath")
            else -> ScopeResult.InScope(resolved)
        }
    }

    fun resolveForWrite(requestedPath: String): ScopeResult {
        val resolved = resolveInScope(requestedPath) ?: return outOfScope(requestedPath)
        val parent = resolved.parent
        return when {
            parent != null && !parent.exists() ->
                ScopeResult.OutOfScope(
                    "Parent directory does not exist: $requestedPath",
                )
            else -> ScopeResult.InScope(resolved)
        }
    }

    fun scopeRoot(): Path = scopeRoot.normalize()

    private fun resolveInScope(requestedPath: String): Path? {
        val trimmed =
            SessionScopeResolver.normalizeAgentPath(
                requestedPath = requestedPath,
                scopeRoot = scopeRoot,
                projectBasePath = projectBasePath,
            )
        if (trimmed.isEmpty()) return null
        val root = scopeRoot.normalize().toAbsolutePath()
        val candidate =
            if (Path.of(trimmed).isAbsolute) {
                Path.of(trimmed).normalize().toAbsolutePath()
            } else {
                root.resolve(trimmed).normalize().toAbsolutePath()
            }
        return when {
            !candidate.startsWith(root) -> null
            candidate == root && !candidate.isDirectory() -> null
            else -> candidate
        }
    }

    private fun outOfScope(requestedPath: String): ScopeResult.OutOfScope =
        ScopeResult.OutOfScope("Path is outside the project or worktree scope: $requestedPath")

    companion object {
        fun create(
            scopeRoot: Path,
            projectBasePath: String? = null,
        ): ScopedFileSystemOperations = ScopedFileSystemOperations(scopeRoot, projectBasePath)

        fun relativeKey(
            scopeRoot: Path,
            resolved: Path,
        ): String? {
            val root = scopeRoot.normalize().toAbsolutePath()
            val absolute = resolved.normalize().toAbsolutePath()
            return absolute.relativeToOrNull(root)?.toString()?.replace('\\', '/')
        }
    }
}
