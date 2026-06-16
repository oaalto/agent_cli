package com.oaalto.agent.acp

data class AcpLaunchPlan(
    val command: List<String>,
    val processWorkingDirectory: String,
    val sessionWorkingDirectory: String,
)
