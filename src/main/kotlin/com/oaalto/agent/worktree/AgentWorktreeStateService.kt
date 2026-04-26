package com.oaalto.agent.worktree

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import java.nio.file.Files
import java.nio.file.Path
import java.util.Locale
import java.util.UUID

@Service(Service.Level.APP)
@State(name = "AgentWorktreeStateService", storages = [Storage("agentWorktrees.xml")])
class AgentWorktreeStateService : PersistentStateComponent<AgentWorktreeStateService.StoredState> {
    data class ManagedWorktreeRecord(
        val id: String,
        val configurationId: String,
        val configurationName: String,
        val repositoryRootPath: String,
        val worktreePath: String,
        val branchName: String,
        val createdAtEpochMs: Long,
        val lastUsedAtEpochMs: Long,
        val deleted: Boolean,
    )

    data class PendingLaunch(
        val worktreePath: String,
        val configurationId: String,
        val configurationName: String,
        val resume: Boolean,
        val createdAtEpochMs: Long,
    )

    class StoredState {
        var records: MutableList<StoredRecord> = mutableListOf()
        var pendingLaunches: MutableList<StoredPendingLaunch> = mutableListOf()
    }

    class StoredRecord {
        var id: String = UUID.randomUUID().toString()
        var configurationId: String = ""
        var configurationName: String = ""
        var repositoryRootPath: String = ""
        var worktreePath: String = ""
        var branchName: String = ""
        var createdAtEpochMs: Long = 0
        var lastUsedAtEpochMs: Long = 0
        var deleted: Boolean = false
    }

    class StoredPendingLaunch {
        var worktreePath: String = ""
        var configurationId: String = ""
        var configurationName: String = ""
        var resume: Boolean = false
        var createdAtEpochMs: Long = 0
    }

    private var state = StoredState()

    init {
        sanitizeState()
    }

    override fun getState(): StoredState = state

    override fun loadState(state: StoredState) {
        this.state = state
        sanitizeState()
    }

    fun saveRecord(
        configurationId: String,
        configurationName: String,
        repositoryRootPath: String,
        worktreePath: String,
        branchName: String,
    ): ManagedWorktreeRecord {
        val now = System.currentTimeMillis()
        val normalizedWorktreePath = normalizePath(worktreePath)
        val normalizedRepoRootPath = normalizePath(repositoryRootPath)
        val existingIndex =
            state.records.indexOfFirst {
                normalizedPathKey(it.worktreePath) == normalizedPathKey(normalizedWorktreePath)
            }
        val storedRecord =
            if (existingIndex >= 0) {
                state.records[existingIndex]
            } else {
                StoredRecord().also { state.records.add(it) }
            }
        if (storedRecord.id.isBlank()) {
            storedRecord.id = UUID.randomUUID().toString()
        }
        storedRecord.configurationId = configurationId.trim()
        storedRecord.configurationName = configurationName.trim()
        storedRecord.repositoryRootPath = normalizedRepoRootPath
        storedRecord.worktreePath = normalizedWorktreePath
        storedRecord.branchName = branchName.trim()
        if (storedRecord.createdAtEpochMs <= 0) {
            storedRecord.createdAtEpochMs = now
        }
        storedRecord.lastUsedAtEpochMs = now
        storedRecord.deleted = false
        sanitizeState()
        return storedRecord.toPublicRecord()
    }

    fun getActiveRecords(
        configurationId: String,
        repositoryRootPath: String,
    ): List<ManagedWorktreeRecord> {
        val configId = configurationId.trim()
        if (configId.isBlank()) return emptyList()
        val repoRootKey = normalizedPathKey(repositoryRootPath)
        return state.records
            .asSequence()
            .filter { !it.deleted }
            .filter { it.configurationId == configId }
            .filter { normalizedPathKey(it.repositoryRootPath) == repoRootKey }
            .sortedByDescending { it.lastUsedAtEpochMs }
            .map { it.toPublicRecord() }
            .toList()
    }

    fun getActiveRecordsForConfiguration(configurationId: String): List<ManagedWorktreeRecord> {
        val configId = configurationId.trim()
        if (configId.isBlank()) return emptyList()
        return state.records
            .asSequence()
            .filter { !it.deleted }
            .filter { it.configurationId == configId }
            .sortedByDescending { it.lastUsedAtEpochMs }
            .map { it.toPublicRecord() }
            .toList()
    }

    fun getRecordByPath(worktreePath: String): ManagedWorktreeRecord? {
        val key = normalizedPathKey(worktreePath)
        return state.records.firstOrNull { normalizedPathKey(it.worktreePath) == key }?.toPublicRecord()
    }

    fun markDeleted(worktreePath: String) {
        val key = normalizedPathKey(worktreePath)
        state.records.forEach { record ->
            if (normalizedPathKey(record.worktreePath) == key) {
                record.deleted = true
            }
        }
    }

    fun markDeletedById(recordId: String): Boolean {
        val targetId = recordId.trim()
        if (targetId.isBlank()) return false
        var changed = false
        state.records.forEach { record ->
            if (record.id == targetId && !record.deleted) {
                record.deleted = true
                changed = true
            }
        }
        return changed
    }

    fun pruneMissingWorktreesForConfiguration(configurationId: String): Int {
        val configId = configurationId.trim()
        if (configId.isBlank()) return 0
        var prunedCount = 0
        state.records.forEach { record ->
            if (record.configurationId == configId && !record.deleted && !pathExists(record.worktreePath)) {
                record.deleted = true
                prunedCount += 1
            }
        }
        return prunedCount
    }

    fun touch(worktreePath: String) {
        val key = normalizedPathKey(worktreePath)
        val now = System.currentTimeMillis()
        state.records.forEach { record ->
            if (normalizedPathKey(record.worktreePath) == key) {
                record.lastUsedAtEpochMs = now
            }
        }
    }

    fun enqueuePendingLaunch(
        worktreePath: String,
        configurationId: String,
        configurationName: String,
        resume: Boolean,
    ) {
        val normalizedWorktreePath = normalizePath(worktreePath)
        val key = normalizedPathKey(normalizedWorktreePath)
        state.pendingLaunches =
            state.pendingLaunches
                .filterNot { pending ->
                    normalizedPathKey(pending.worktreePath) == key
                }.toMutableList()
        state.pendingLaunches.add(
            StoredPendingLaunch().also { pending ->
                pending.worktreePath = normalizedWorktreePath
                pending.configurationId = configurationId.trim()
                pending.configurationName = configurationName.trim()
                pending.resume = resume
                pending.createdAtEpochMs = System.currentTimeMillis()
            },
        )
        sanitizeState()
    }

    fun consumePendingLaunch(worktreePath: String): PendingLaunch? {
        val key = normalizedPathKey(worktreePath)
        val index =
            state.pendingLaunches.indexOfFirst { pending ->
                normalizedPathKey(pending.worktreePath) == key
            }
        if (index < 0) return null
        val pending = state.pendingLaunches.removeAt(index)
        return pending.toPublicPendingLaunch()
    }

    private fun sanitizeState() {
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

    private fun sanitizeRecord(record: StoredRecord): StoredRecord? {
        val configurationId = record.configurationId.trim()
        val worktreePath = normalizePath(record.worktreePath)
        val repositoryRootPath = normalizePath(record.repositoryRootPath)
        if (configurationId.isBlank() || worktreePath.isBlank() || repositoryRootPath.isBlank()) {
            return null
        }
        val sanitized = StoredRecord()
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
        return sanitized
    }

    private fun sanitizePendingLaunch(pendingLaunch: StoredPendingLaunch): StoredPendingLaunch? {
        val configurationId = pendingLaunch.configurationId.trim()
        val worktreePath = normalizePath(pendingLaunch.worktreePath)
        if (configurationId.isBlank() || worktreePath.isBlank()) {
            return null
        }
        val sanitized = StoredPendingLaunch()
        sanitized.worktreePath = worktreePath
        sanitized.configurationId = configurationId
        sanitized.configurationName = pendingLaunch.configurationName.trim().ifBlank { "Agent" }
        sanitized.resume = pendingLaunch.resume
        sanitized.createdAtEpochMs = pendingLaunch.createdAtEpochMs.takeIf { it > 0 } ?: System.currentTimeMillis()
        return sanitized
    }

    private fun StoredRecord.toPublicRecord(): ManagedWorktreeRecord =
        ManagedWorktreeRecord(
            id = id,
            configurationId = configurationId,
            configurationName = configurationName,
            repositoryRootPath = repositoryRootPath,
            worktreePath = worktreePath,
            branchName = branchName,
            createdAtEpochMs = createdAtEpochMs,
            lastUsedAtEpochMs = lastUsedAtEpochMs,
            deleted = deleted,
        )

    private fun StoredPendingLaunch.toPublicPendingLaunch(): PendingLaunch =
        PendingLaunch(
            worktreePath = worktreePath,
            configurationId = configurationId,
            configurationName = configurationName,
            resume = resume,
            createdAtEpochMs = createdAtEpochMs,
        )

    private fun normalizePath(rawPath: String): String {
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

    private fun pathExists(rawPath: String): Boolean {
        val normalized = normalizePath(rawPath)
        if (normalized.isBlank()) return false
        return kotlin
            .runCatching {
                Files.isDirectory(Path.of(normalized))
            }.getOrDefault(false)
    }

    private fun normalizedPathKey(rawPath: String): String {
        var value =
            normalizePath(rawPath)
                .replace('\\', '/')
                .lowercase(Locale.ROOT)
        if (value.startsWith("//wsl$/")) {
            value = value.replaceFirst("//wsl$/", "//wsl.localhost/")
        }
        return value
    }

    companion object {
        fun getInstance(): AgentWorktreeStateService = service()
    }
}
