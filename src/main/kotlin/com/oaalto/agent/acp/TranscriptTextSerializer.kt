package com.oaalto.agent.acp

import com.agentclientprotocol.model.ToolCallStatus
import com.oaalto.agent.acp.transcript.model.TranscriptBlock

/**
 * Maps [TranscriptBlock] snapshots to plain text for session transcript files.
 * Thought blocks are included with `[thought]` prefix (matches live Transcript UI).
 * Plan blocks, tool payloads, and usage footer are omitted.
 */
internal object TranscriptTextSerializer {
    fun serialize(blocks: List<TranscriptBlock>): String =
        blocks
            .flatMap(::serializeBlock)
            .joinToString("\n")

    private fun serializeBlock(block: TranscriptBlock): List<String> =
        when (block) {
            is TranscriptBlock.PlanBlock -> emptyList()
            is TranscriptBlock.ToolCallBlock -> listOf(formatToolHeader(block))
            else -> serializeTextBlock(block)
        }

    private fun serializeTextBlock(block: TranscriptBlock): List<String> =
        when (block) {
            is TranscriptBlock.UserEcho -> listOf("> ${block.text}")
            is TranscriptBlock.Thought -> listOf("[thought] ${block.text}")
            is TranscriptBlock.StreamingAgentText -> textLines(block.text)
            is TranscriptBlock.FinalAgentText -> textLines(block.text)
            is TranscriptBlock.PlainLine -> listOf(block.text)
            is TranscriptBlock.ErrorLine -> listOf(TranscriptRenderer.formatError(block.message))
            is TranscriptBlock.AuthFailureLine ->
                listOf(TranscriptRenderer.formatAuthFailure(block.message))
            else -> emptyList()
        }

    private fun textLines(text: String): List<String> {
        val normalized = TranscriptRenderer.normalizeTranscriptText(text)
        if (normalized.isEmpty()) return emptyList()
        return normalized.split('\n')
    }

    internal fun formatToolHeader(block: TranscriptBlock.ToolCallBlock): String {
        val statusSuffix =
            when (block.status) {
                ToolCallStatus.COMPLETED -> " completed"
                ToolCallStatus.FAILED -> " failed"
                ToolCallStatus.IN_PROGRESS -> " in progress"
                ToolCallStatus.PENDING -> " pending"
                null -> ""
            }
        return "[tool: ${block.title}$statusSuffix]"
    }
}
