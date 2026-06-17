package com.oaalto.agent.worktree

fun AgentWorktreeStateService.getActiveRecordsForConfiguration(
    configurationId: String,
): List<AgentWorktreeStateService.ManagedWorktreeRecord> {
    val configId = configurationId.trim()
    if (configId.isBlank()) return emptyList()
    return getState()
        .records
        .asSequence()
        .filter { !it.deleted }
        .filter { it.configurationId == configId }
        .sortedByDescending { it.lastUsedAtEpochMs }
        .map(AgentWorktreeStateSupport::toPublicRecord)
        .toList()
}

fun AgentWorktreeStateService.touch(worktreePath: String) {
    val key = AgentWorktreeStateSupport.normalizedPathKey(worktreePath)
    val now = System.currentTimeMillis()
    getState().records.forEach { record ->
        if (AgentWorktreeStateSupport.normalizedPathKey(record.worktreePath) == key) {
            record.lastUsedAtEpochMs = now
        }
    }
}
