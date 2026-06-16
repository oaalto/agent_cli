package com.oaalto.agent.acp

interface AcpSessionController {
    suspend fun connect(launchPlan: AcpLaunchPlan)

    suspend fun newSession()

    suspend fun prompt(text: String)

    suspend fun cancelPrompt()

    fun dispose()
}
