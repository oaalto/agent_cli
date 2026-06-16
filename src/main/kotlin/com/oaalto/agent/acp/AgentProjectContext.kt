package com.oaalto.agent.acp

import com.intellij.openapi.project.Project

data class AgentProjectContext(
    val basePath: String?,
)

fun Project.toAgentProjectContext(): AgentProjectContext = AgentProjectContext(basePath = basePath)
