package com.oaalto.agent.worktree

import java.nio.file.Files
import java.nio.file.Path
import java.util.Locale
import java.util.UUID

internal object AgentWorktreeStateSupport {
    fun sanitizeState(state: AgentWorktreeStateService.StoredState) {
        state.records =
            state.records
                .mapNotNull { sanitizeRecord(it) }
                .distinctBy { normalizedPathKey(it.worktreePath) }
                .toMutableList()
        state.pendingLaunches =
            state.pendingLaunches
                .mapNotNull { sanitizePendingLaunch(it) }
                .distinctBy { normalizedPathKey(it.worktreePath) }
                .toMutableList()
    }

    fun sanitizeRecord(record: AgentWorktreeStateService.StoredRecord): AgentWorktreeStateService.StoredRecord? {
        val configurationId = record.configurationId.trim()
        val worktreePath = normalizePath(record.worktreePath)
        val repositoryRootPath = normalizePath(record.repositoryRootPath)
        if (configurationId.isBlank() || worktreePath.isBlank() || repositoryRootPath.isBlank()) {
            return null
        }
        val sanitized = AgentWorktreeStateService.StoredRecord()
        sanitized.id = record.id.trim().ifBlank { UUID.randomUUID().toString() }
        sanitized.configurationId = configurationId
        sanitized.configurationName = record.configurationName.trim().ifBlank { "Agent" }
        sanitized.repositoryRootPath = repositoryRootPath
        sanitized.worktreePath = worktreePath
        sanitized.branchName = record.branchName.trim()
        val createdAt = record.createdAtEpochMs.takeIf { it > 0 } ?: System.currentTimeMillis()
        sanitized.createdAtEpochMs = createdAt
        sanitized.lastUsedAtEpochMs = record.lastUsedAtEpochMs.takeIf { it > 0 } ?: createdAt
        sanitized.deleted = record.deleted
        sanitized.acpSessionId = record.acpSessionId.trim()
        return sanitized
    }

    fun sanitizePendingLaunch(
        pendingLaunch: AgentWorktreeStateService.StoredPendingLaunch,
    ): AgentWorktreeStateService.StoredPendingLaunch? {
        val configurationId = pendingLaunch.configurationId.trim()
        val worktreePath = normalizePath(pendingLaunch.worktreePath)
        if (configurationId.isBlank() || worktreePath.isBlank()) {
            return null
        }
        val sanitized = AgentWorktreeStateService.StoredPendingLaunch()
        sanitized.worktreePath = worktreePath
        sanitized.configurationId = configurationId
        sanitized.configurationName = pendingLaunch.configurationName.trim().ifBlank { "Agent" }
        sanitized.resume = pendingLaunch.resume
        sanitized.createdAtEpochMs = pendingLaunch.createdAtEpochMs.takeIf { it > 0 } ?: System.currentTimeMillis()
        return sanitized
    }

    fun toPublicRecord(
        record: AgentWorktreeStateService.StoredRecord,
    ): AgentWorktreeStateService.ManagedWorktreeRecord =
        AgentWorktreeStateService.ManagedWorktreeRecord(
            id = record.id,
            configurationId = record.configurationId,
            configurationName = record.configurationName,
            repositoryRootPath = record.repositoryRootPath,
            worktreePath = record.worktreePath,
            branchName = record.branchName,
            acpSessionId = record.acpSessionId.trim().ifBlank { null },
            createdAtEpochMs = record.createdAtEpochMs,
            lastUsedAtEpochMs = record.lastUsedAtEpochMs,
            deleted = record.deleted,
        )

    fun toPublicPendingLaunch(
        pending: AgentWorktreeStateService.StoredPendingLaunch,
    ): AgentWorktreeStateService.PendingLaunch =
        AgentWorktreeStateService.PendingLaunch(
            worktreePath = pending.worktreePath,
            configurationId = pending.configurationId,
            configurationName = pending.configurationName,
            resume = pending.resume,
            createdAtEpochMs = pending.createdAtEpochMs,
        )

    fun normalizePath(rawPath: String): String {
        val trimmed = rawPath.trim()
        if (trimmed.isBlank()) return ""
        return kotlin
            .runCatching {
                Path
                    .of(trimmed)
                    .toAbsolutePath()
                    .normalize()
                    .toString()
            }.getOrElse { trimmed }
    }

    fun pathExists(rawPath: String): Boolean {
        val normalized = normalizePath(rawPath)
        if (normalized.isBlank()) return false
        return kotlin
            .runCatching {
                Files.isDirectory(Path.of(normalized))
            }.getOrDefault(false)
    }

    fun normalizedPathKey(rawPath: String): String {
        var value =
            normalizePath(rawPath)
                .replace('\\', '/')
                .lowercase(Locale.ROOT)
        if (value.startsWith("//wsl$/")) {
            value = value.replaceFirst("//wsl$/", "//wsl.localhost/")
        }
        return value
    }
}
