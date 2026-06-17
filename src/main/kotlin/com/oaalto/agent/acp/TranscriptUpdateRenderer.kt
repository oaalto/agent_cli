package com.oaalto.agent.acp

import com.agentclientprotocol.model.ContentBlock
import com.agentclientprotocol.model.SessionUpdate

internal object TranscriptUpdateRenderer {
    fun render(update: SessionUpdate): List<String> =
        when (update) {
            is SessionUpdate.AgentMessageChunk -> renderAgentMessageChunk(update)
            is SessionUpdate.AgentThoughtChunk -> renderAgentThoughtChunk(update)
            is SessionUpdate.UserMessageChunk -> renderUserMessageChunk(update)
            is SessionUpdate.ToolCall -> renderToolCall(update)
            is SessionUpdate.ToolCallUpdate -> renderToolCallUpdate(update)
            else -> emptyList()
        }

    private fun renderAgentMessageChunk(update: SessionUpdate.AgentMessageChunk): List<String> =
        listOfNotNull(extractText(update.content))

    private fun renderAgentThoughtChunk(update: SessionUpdate.AgentThoughtChunk): List<String> =
        listOfNotNull(extractText(update.content)?.let { "[thought] $it" })

    private fun renderUserMessageChunk(update: SessionUpdate.UserMessageChunk): List<String> =
        listOfNotNull(extractText(update.content)?.let { "> $it" })

    private fun renderToolCall(update: SessionUpdate.ToolCall): List<String> =
        listOf(TranscriptRenderer.formatToolStatus(update.title, update.kind, update.status))

    private fun renderToolCallUpdate(update: SessionUpdate.ToolCallUpdate): List<String> =
        listOf(
            TranscriptRenderer.formatToolStatus(
                title = update.title ?: update.toolCallId.value,
                kind = update.kind,
                status = update.status,
            ),
        )

    private fun extractText(content: ContentBlock): String? =
        when (content) {
            is ContentBlock.Text -> content.text
            else -> null
        }
}
