package com.oaalto.agent.acp

import com.oaalto.agent.acp.AcpEditorContext

interface AcpSessionController {
    suspend fun connect(
        launchPlan: AcpLaunchPlan,
        editorContext: AcpEditorContext,
    )

    suspend fun newSession()

    suspend fun prompt(text: String)

    suspend fun cancelPrompt()

    fun dispose()
}
