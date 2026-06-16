package com.oaalto.agent.worktree.resume

object AcpResumeStrategy : ResumeStrategy {
    override fun prepareLaunch(context: ResumeContext): LaunchResumePlan {
        if (!context.resume) {
            return LaunchResumePlan.AcpNewSession
        }
        val record = context.worktreeRecord
        if (record != null && record.configurationId != context.configuration.id.trim()) {
            return LaunchResumePlan.AcpNewSession
        }
        val storedId = record?.acpSessionId?.trim().orEmpty()
        return if (storedId.isNotBlank()) {
            LaunchResumePlan.AcpLoad(storedId)
        } else {
            LaunchResumePlan.AcpPickSession(emptyList())
        }
    }
}
