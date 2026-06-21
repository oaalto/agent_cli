package com.oaalto.agent.acp

import com.agentclientprotocol.model.ToolCallStatus
import com.agentclientprotocol.model.ToolKind

/** Immutable view state for one row in the structured transcript. */
internal sealed class TranscriptBlock {
    abstract val blockId: String

    data class UserEcho(
        override val blockId: String,
        val text: String,
    ) : TranscriptBlock()

    data class Thought(
        override val blockId: String,
        val text: String,
    ) : TranscriptBlock()

    data class StreamingAgentText(
        override val blockId: String,
        val text: String,
    ) : TranscriptBlock()

    data class FinalAgentText(
        override val blockId: String,
        val text: String,
    ) : TranscriptBlock()

    data class PlainLine(
        override val blockId: String,
        val text: String,
        val isUserPrompt: Boolean = false,
    ) : TranscriptBlock()

    data class ErrorLine(
        override val blockId: String,
        val message: String,
    ) : TranscriptBlock()

    data class AuthFailureLine(
        override val blockId: String,
        val message: String,
    ) : TranscriptBlock()

    data class ToolCallBlock(
        override val blockId: String,
        val toolCallId: String,
        val title: String,
        val kind: ToolKind?,
        val status: ToolCallStatus?,
        val bodyParts: List<TranscriptBodyPart>,
        val expanded: Boolean = false,
    ) : TranscriptBlock() {
        val hasBodyContent: Boolean
            get() = bodyParts.isNotEmpty()
    }
}
