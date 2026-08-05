package com.oaalto.agent.acp.transcript.model

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

    /**
     * A plan block showing numbered entries with status icons.
     * Plans update in-place by planId; the blockId remains stable per plan.
     */
    data class PlanBlock(
        override val blockId: String,
        val planId: String,
        val entries: List<PlanEntry>,
        val variant: PlanVariant = PlanVariant.Items,
        val dismissed: Boolean = false,
    ) : TranscriptBlock() {
        /** Number of completed entries for progress summary. */
        val completedCount: Int
            get() = if (dismissed) 0 else entries.count { it.status == PlanEntryStatus.COMPLETED }

        /** True if all entries are completed. */
        val isFullyComplete: Boolean
            get() = !dismissed && entries.isNotEmpty() && entries.all { it.status == PlanEntryStatus.COMPLETED }
    }
}
