package com.oaalto.agent

import com.oaalto.agent.worktree.resume.LaunchResumePlan

data class AgentLaunchContext(
    val workingDirectoryOverride: String? = null,
    val additionalArguments: List<String> = emptyList(),
    val worktreeId: String? = null,
    val resume: Boolean = false,
    val resumePlan: LaunchResumePlan? = null,
)
