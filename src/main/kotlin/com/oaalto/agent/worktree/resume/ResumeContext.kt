package com.oaalto.agent.worktree.resume

import com.oaalto.agent.settings.AgentSettingsState
import com.oaalto.agent.worktree.AgentWorktreeStateService

data class ResumeContext(
    val configuration: AgentSettingsState.AgentCliConfiguration,
    val worktreeRecord: AgentWorktreeStateService.ManagedWorktreeRecord?,
    val workingDirectory: String,
    val resume: Boolean,
    val wslWorkingDirectory: String? = null,
    val wslDistribution: String = "",
    val hostWorkingDirectory: String = "",
    val cursorProbe: CursorResumeProbe = CursorResumeProbe.Default,
)
