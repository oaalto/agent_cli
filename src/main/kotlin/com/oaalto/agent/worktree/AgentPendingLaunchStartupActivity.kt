package com.oaalto.agent.worktree

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity

class AgentPendingLaunchStartupActivity : StartupActivity.DumbAware {
    override fun runActivity(project: Project) {
        WorktreePendingLaunchHandoff.completePendingLaunchIfAny(project)
    }
}
