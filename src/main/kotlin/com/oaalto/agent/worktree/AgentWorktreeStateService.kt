package com.oaalto.agent.worktree

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
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
        val acpSessionId: String?,
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
        var acpSessionId: String = ""
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
        AgentWorktreeStateSupport.sanitizeState(state)
    }

    override fun getState(): StoredState = state

    override fun loadState(state: StoredState) {
        this.state = state
        AgentWorktreeStateSupport.sanitizeState(this.state)
    }

    fun saveRecord(
        configurationId: String,
        configurationName: String,
        repositoryRootPath: String,
        worktreePath: String,
        branchName: String,
    ): ManagedWorktreeRecord {
        val now = System.currentTimeMillis()
        val normalizedWorktreePath = AgentWorktreeStateSupport.normalizePath(worktreePath)
        val normalizedRepoRootPath = AgentWorktreeStateSupport.normalizePath(repositoryRootPath)
        val existingIndex =
            state.records.indexOfFirst {
                AgentWorktreeStateSupport.normalizedPathKey(it.worktreePath) ==
                    AgentWorktreeStateSupport.normalizedPathKey(normalizedWorktreePath)
            }
        val storedRecord =
            if (existingIndex >= 0) {
                val existing = state.records[existingIndex]
                if (existing.configurationId != configurationId.trim() && existing.acpSessionId.isNotBlank()) {
                    existing.acpSessionId = ""
                }
                existing
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
        AgentWorktreeStateSupport.sanitizeState(state)
        return AgentWorktreeStateSupport.toPublicRecord(storedRecord)
    }

    fun getActiveRecords(
        configurationId: String,
        repositoryRootPath: String,
    ): List<ManagedWorktreeRecord> {
        val configId = configurationId.trim()
        if (configId.isBlank()) return emptyList()
        val repoRootKey = AgentWorktreeStateSupport.normalizedPathKey(repositoryRootPath)
        return state.records
            .asSequence()
            .filter { !it.deleted }
            .filter { it.configurationId == configId }
            .filter { AgentWorktreeStateSupport.normalizedPathKey(it.repositoryRootPath) == repoRootKey }
            .sortedByDescending { it.lastUsedAtEpochMs }
            .map(AgentWorktreeStateSupport::toPublicRecord)
            .toList()
    }

    fun getRecordByPath(worktreePath: String): ManagedWorktreeRecord? {
        val key = AgentWorktreeStateSupport.normalizedPathKey(worktreePath)
        return state.records
            .firstOrNull { AgentWorktreeStateSupport.normalizedPathKey(it.worktreePath) == key }
            ?.let(AgentWorktreeStateSupport::toPublicRecord)
    }

    fun markDeleted(worktreePath: String) {
        val key = AgentWorktreeStateSupport.normalizedPathKey(worktreePath)
        state.records.forEach { record ->
            if (AgentWorktreeStateSupport.normalizedPathKey(record.worktreePath) == key) {
                record.deleted = true
                record.acpSessionId = ""
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
                record.acpSessionId = ""
                changed = true
            }
        }
        return changed
    }

    fun setAcpSessionId(
        recordId: String,
        sessionId: String,
    ): Boolean {
        val targetId = recordId.trim()
        val normalizedSessionId = sessionId.trim()
        if (targetId.isBlank() || normalizedSessionId.isBlank()) return false
        var updated = false
        state.records.forEach { record ->
            if (record.id == targetId && !record.deleted) {
                record.acpSessionId = normalizedSessionId
                record.lastUsedAtEpochMs = System.currentTimeMillis()
                updated = true
            }
        }
        return updated
    }

    fun clearAcpSessionId(recordId: String): Boolean {
        val targetId = recordId.trim()
        if (targetId.isBlank()) return false
        var updated = false
        state.records.forEach { record ->
            if (record.id == targetId && record.acpSessionId.isNotBlank()) {
                record.acpSessionId = ""
                updated = true
            }
        }
        return updated
    }

    fun pruneMissingWorktreesForConfiguration(configurationId: String): Int {
        val configId = configurationId.trim()
        if (configId.isBlank()) return 0
        var prunedCount = 0
        state.records.forEach { record ->
            if (
                record.configurationId == configId &&
                !record.deleted &&
                !AgentWorktreeStateSupport.pathExists(record.worktreePath)
            ) {
                record.deleted = true
                prunedCount += 1
            }
        }
        return prunedCount
    }

    fun enqueuePendingLaunch(
        worktreePath: String,
        configurationId: String,
        configurationName: String,
        resume: Boolean,
    ) {
        val normalizedWorktreePath = AgentWorktreeStateSupport.normalizePath(worktreePath)
        val key = AgentWorktreeStateSupport.normalizedPathKey(normalizedWorktreePath)
        state.pendingLaunches =
            state.pendingLaunches
                .filterNot { pending ->
                    AgentWorktreeStateSupport.normalizedPathKey(pending.worktreePath) == key
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
        AgentWorktreeStateSupport.sanitizeState(state)
    }

    fun consumePendingLaunch(worktreePath: String): PendingLaunch? {
        val key = AgentWorktreeStateSupport.normalizedPathKey(worktreePath)
        val index =
            state.pendingLaunches.indexOfFirst { pending ->
                AgentWorktreeStateSupport.normalizedPathKey(pending.worktreePath) == key
            }
        if (index < 0) return null
        val pending = state.pendingLaunches.removeAt(index)
        return AgentWorktreeStateSupport.toPublicPendingLaunch(pending)
    }

    companion object {
        fun getInstance(): AgentWorktreeStateService = service()
    }
}
