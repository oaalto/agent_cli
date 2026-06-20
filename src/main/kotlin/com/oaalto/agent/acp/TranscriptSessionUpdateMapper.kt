package com.oaalto.agent.acp

import com.agentclientprotocol.model.SessionUpdate

/** Maps ACP [SessionUpdate] events to normalized [StructuredUpdate] values. */
internal object TranscriptSessionUpdateMapper {
    fun mapAgentChunk(update: SessionUpdate.AgentMessageChunk): StructuredUpdate? =
        TranscriptRenderer
            .renderEventText(update)
            ?.takeIf { it.isNotBlank() }
            ?.let { StructuredUpdate.AppendAgentText(it) }

    fun mapUpdate(update: SessionUpdate): List<StructuredUpdate> =
        when (update) {
            is SessionUpdate.AgentMessageChunk -> listOfNotNull(mapAgentChunk(update))
            is SessionUpdate.AgentThoughtChunk -> mapThoughtChunk(update)
            is SessionUpdate.UserMessageChunk -> mapUserChunk(update)
            is SessionUpdate.ToolCall -> listOf(mapToolCall(update))
            is SessionUpdate.ToolCallUpdate -> listOf(mapToolCallUpdate(update))
            else -> emptyList()
        }

    private fun mapThoughtChunk(update: SessionUpdate.AgentThoughtChunk): List<StructuredUpdate> =
        listOfNotNull(
            TranscriptRenderer.extractText(update.content)?.takeIf { it.isNotBlank() }?.let {
                StructuredUpdate.AppendThought(it)
            },
        )

    private fun mapUserChunk(update: SessionUpdate.UserMessageChunk): List<StructuredUpdate> =
        listOfNotNull(
            TranscriptRenderer.extractText(update.content)?.takeIf { it.isNotBlank() }?.let {
                StructuredUpdate.AppendUserEcho(it)
            },
        )

    private fun mapToolCall(update: SessionUpdate.ToolCall): StructuredUpdate.StartOrUpdateToolCall =
        StructuredUpdate.StartOrUpdateToolCall(
            toolCallId = update.toolCallId.value,
            title = update.title,
            kind = update.kind,
            status = update.status,
            contentFragments =
                TranscriptRenderer.renderToolCallContentFragments(
                    content = update.content,
                    status = update.status,
                ),
        )

    private fun mapToolCallUpdate(update: SessionUpdate.ToolCallUpdate): StructuredUpdate.StartOrUpdateToolCall =
        StructuredUpdate.StartOrUpdateToolCall(
            toolCallId = update.toolCallId.value,
            title = update.title ?: update.toolCallId.value,
            kind = update.kind,
            status = update.status,
            contentFragments =
                TranscriptRenderer.renderToolCallContentFragments(
                    content = update.content,
                    status = update.status,
                ),
        )
}
