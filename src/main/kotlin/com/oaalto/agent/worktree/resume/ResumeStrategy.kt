package com.oaalto.agent.worktree.resume

fun interface ResumeStrategy {
    fun prepareLaunch(context: ResumeContext): LaunchResumePlan
}
