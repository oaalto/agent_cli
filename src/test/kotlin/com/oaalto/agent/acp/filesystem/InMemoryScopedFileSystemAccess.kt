package com.oaalto.agent.acp.filesystem

import java.nio.file.Path

/**
 * In-memory [ScopedFileSystemAccess] for unit tests.
 *
 * Supports normal reads/writes plus read-only and version-control-ignored
 * simulation so policy tests can verify write-block behaviour without the
 * IntelliJ platform.
 */
class InMemoryScopedFileSystemAccess(
    private val files: MutableMap<Path, String> = mutableMapOf(),
    private val readOnlyPaths: Set<Path> = emptySet(),
    private val ignoredPaths: Set<Path> = emptySet(),
) : ScopedFileSystemAccess {
    override fun readText(resolved: Path): ScopedFileSystemAccess.AccessResult {
        val key = resolved.normalize().toAbsolutePath()
        val content =
            files[key]
                ?: return ScopedFileSystemAccess.AccessResult.Failure("File not found: $resolved")
        return ScopedFileSystemAccess.AccessResult.Success(content)
    }

    override fun writeText(
        resolved: Path,
        content: String,
    ): ScopedFileSystemAccess.AccessResult {
        val key = resolved.normalize().toAbsolutePath()
        if (readOnlyPaths.contains(key)) {
            return ScopedFileSystemAccess.AccessResult.Failure("File is read-only: $resolved")
        }
        files[key] = content
        return ScopedFileSystemAccess.AccessResult.Success(content)
    }

    override fun isBlockedForWrite(resolved: Path): String? {
        val key = resolved.normalize().toAbsolutePath()
        return when {
            readOnlyPaths.contains(key) -> "File is read-only: $resolved"
            ignoredPaths.contains(key) -> "File is ignored by version control: $resolved"
            else -> null
        }
    }
}
