package com.oaalto.agent.worktree.resume

object AcpResumeStrategy : ResumeStrategy {
    override fun prepareLaunch(context: ResumeContext): LaunchResumePlan {
        val record = context.worktreeRecord
        if (
            !context.resume ||
            (record != null && record.configurationId != context.configuration.id.trim())
        ) {
            return LaunchResumePlan.AcpNewSession
        }
        val storedId = record?.acpSessionId?.trim().orEmpty()
        return when {
            storedId.isNotBlank() -> LaunchResumePlan.AcpLoad(storedId)
            else -> LaunchResumePlan.AcpPickSession(emptyList())
        }
    }
}
