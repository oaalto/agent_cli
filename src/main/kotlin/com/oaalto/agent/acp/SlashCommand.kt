package com.oaalto.agent.acp

/** Agent slash command advertised via ACP [com.agentclientprotocol.model.SessionUpdate.AvailableCommandsUpdate]. */
data class SlashCommand(
    val name: String,
    val description: String,
    val inputHint: String? = null,
)
