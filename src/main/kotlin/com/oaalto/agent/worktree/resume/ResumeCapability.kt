package com.oaalto.agent.worktree.resume

import com.oaalto.agent.settings.AgentSettingsState
import com.oaalto.agent.settings.LaunchMode

object ResumeCapability {
    fun canResumeSessions(configuration: AgentSettingsState.AgentCliConfiguration): Boolean =
        when (LaunchMode.from(configuration.launchMode)) {
            LaunchMode.PTY_PASSTHROUGH -> PtyResumeStrategy.canResume(configuration)
            LaunchMode.ACP_CLIENT -> true
        }
}
