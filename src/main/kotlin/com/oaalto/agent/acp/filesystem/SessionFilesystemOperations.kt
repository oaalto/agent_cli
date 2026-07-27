package com.oaalto.agent.acp.filesystem

/**
 * Deep module owning the full filesystem client-op policy chain:
 * scope resolution → write permission → VFS access → line/limit slicing.
 *
 * All scope/permission/VFS logic lives here, not split across the SDK adapter
 * and individual scope/permission modules.
 */
sealed class SessionFilesystemResult {
    data class Success(
        val content: String,
    ) : SessionFilesystemResult()

    data class Failure(
        val reason: FailureReason,
        val message: String,
    ) : SessionFilesystemResult()

    enum class FailureReason { OUT_OF_SCOPE, PERMISSION_DENIED, VFS_ERROR }
}

interface SessionFilesystemOperations {
    suspend fun readText(
        path: String,
        line: UInt?,
        limit: UInt?,
    ): SessionFilesystemResult

    suspend fun writeText(
        path: String,
        content: String,
    ): SessionFilesystemResult
}

class SessionFilesystemOperationsImpl(
    private val scope: ScopedFileSystemOperations,
    private val vfs: ScopedFileSystemAccess,
    private val permissionCoordinator: com.oaalto.agent.acp.permission.PermissionCoordinator,
) : SessionFilesystemOperations {
    override suspend fun readText(
        path: String,
        line: UInt?,
        limit: UInt?,
    ): SessionFilesystemResult =
        when (val resolution = scope.resolveForRead(path)) {
            is ScopedFileSystemOperations.ScopeResult.OutOfScope ->
                SessionFilesystemResult.Failure(
                    SessionFilesystemResult.FailureReason.OUT_OF_SCOPE,
                    resolution.message,
                )
            is ScopedFileSystemOperations.ScopeResult.InScope -> {
                when (val readResult = vfs.readText(resolution.resolved)) {
                    is ScopedFileSystemAccess.AccessResult.Success ->
                        SessionFilesystemResult.Success(sliceLines(readResult.content, line, limit))
                    is ScopedFileSystemAccess.AccessResult.Failure ->
                        SessionFilesystemResult.Failure(
                            SessionFilesystemResult.FailureReason.VFS_ERROR,
                            readResult.message,
                        )
                }
            }
        }

    override suspend fun writeText(
        path: String,
        content: String,
    ): SessionFilesystemResult {
        val resolution = scope.resolveForWrite(path)
        if (resolution !is ScopedFileSystemOperations.ScopeResult.InScope) {
            val outOfScope = resolution as ScopedFileSystemOperations.ScopeResult.OutOfScope
            return failure(SessionFilesystemResult.FailureReason.OUT_OF_SCOPE, outOfScope.message)
        }
        val writeError = checkWritePreconditions(path, resolution.resolved)
        if (writeError != null) return writeError
        return when (val writeResult = vfs.writeText(resolution.resolved, content)) {
            is ScopedFileSystemAccess.AccessResult.Success ->
                SessionFilesystemResult.Success(writeResult.content)
            is ScopedFileSystemAccess.AccessResult.Failure ->
                failure(SessionFilesystemResult.FailureReason.VFS_ERROR, writeResult.message)
        }
    }

    private fun failure(
        reason: SessionFilesystemResult.FailureReason,
        message: String,
    ): SessionFilesystemResult = SessionFilesystemResult.Failure(reason, message)

    private suspend fun checkWritePreconditions(
        path: String,
        resolved: java.nio.file.Path,
    ): SessionFilesystemResult? {
        vfs.isBlockedForWrite(resolved)?.let { message ->
            return failure(SessionFilesystemResult.FailureReason.VFS_ERROR, message)
        }
        if (!permissionCoordinator.requestWritePermission(path)) {
            return failure(
                SessionFilesystemResult.FailureReason.PERMISSION_DENIED,
                "Write permission denied for $path",
            )
        }
        return null
    }

    private fun sliceLines(
        content: String,
        line: UInt?,
        limit: UInt?,
    ): String {
        if (line == null && limit == null) return content
        val lines = content.lines()
        val start = line?.toInt() ?: 0
        val end =
            if (limit != null) {
                (start + limit.toInt()).coerceAtMost(lines.size)
            } else {
                lines.size
            }
        return lines.drop(start).take(end - start).joinToString("\n")
    }
}
