package com.oaalto.agent

data class AgentLaunchContext(
    val workingDirectoryOverride: String? = null,
    val additionalArguments: List<String> = emptyList(),
    val worktreeId: String? = null,
)
