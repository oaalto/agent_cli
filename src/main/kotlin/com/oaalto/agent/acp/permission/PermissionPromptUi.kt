package com.oaalto.agent.acp.permission

import com.agentclientprotocol.model.PermissionOption
import com.agentclientprotocol.model.RequestPermissionOutcome

fun interface PermissionPromptUi {
    suspend fun prompt(
        title: String,
        options: List<PermissionOption>,
    ): RequestPermissionOutcome
}
