package com.oaalto.agent.acp.filesystem

import java.nio.file.Path

/**
 * Testable VFS boundary for IDE filesystem I/O.
 *
 * The platform-specific access type ([IdeScopedFileSystemAccess]) becomes a
 * production adapter behind this interface; session operations depend on the
 * interface so unit tests can use an in-memory adapter without IntelliJ
 * platform fixtures.
 */
interface ScopedFileSystemAccess {
    /** Platform-neutral success/failure result. */
    sealed class AccessResult {
        data class Success(
            val content: String = "",
        ) : AccessResult()

        data class Failure(
            val message: String,
        ) : AccessResult()
    }

    /** Read a text file at the resolved path. */
    fun readText(resolved: Path): AccessResult

    /** Write text content to the resolved path. */
    fun writeText(
        resolved: Path,
        content: String,
    ): AccessResult

    /**
     * Return a block reason for write if the file is not writable (read-only or
     * VCS-ignored), or null if the write is allowed. Used by the deep module
     * before invoking [writeText].
     */
    fun isBlockedForWrite(resolved: Path): String?
}
