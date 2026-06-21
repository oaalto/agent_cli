package com.oaalto.agent.acp

import com.agentclientprotocol.model.ToolCallStatus
import com.agentclientprotocol.model.ToolKind

/** Normalized transcript delta produced from ACP session events. */
sealed class StructuredUpdate {
    data class AppendAgentText(
        val text: String,
    ) : StructuredUpdate()

    data object FinalizeAgentStream : StructuredUpdate()

    data class AppendUserEcho(
        val text: String,
    ) : StructuredUpdate()

    data class AppendThought(
        val text: String,
    ) : StructuredUpdate()

    data class AppendPlainLine(
        val line: String,
        val isUserPrompt: Boolean = false,
    ) : StructuredUpdate()

    data class AppendError(
        val message: String,
    ) : StructuredUpdate()

    data class AppendAuthFailure(
        val message: String,
    ) : StructuredUpdate()

    data class StartOrUpdateToolCall(
        val toolCallId: String,
        val title: String,
        val kind: ToolKind?,
        val status: ToolCallStatus?,
        val bodyParts: List<TranscriptBodyPart> = emptyList(),
    ) : StructuredUpdate()
}
