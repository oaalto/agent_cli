package com.oaalto.agent.acp

import com.agentclientprotocol.model.ContentBlock
import com.agentclientprotocol.model.SessionUpdate
import com.agentclientprotocol.model.ToolCallStatus
import com.agentclientprotocol.model.ToolKind

object TranscriptRenderer {
    private val BR_TAG_PATTERN = Regex("(?i)<br\\s*/?>")

    fun renderUpdate(update: SessionUpdate): List<String> = TranscriptUpdateRenderer.render(update)

    fun renderEventText(update: SessionUpdate): String? =
        when (update) {
            is SessionUpdate.AgentMessageChunk -> extractText(update.content)
            is SessionUpdate.AgentThoughtChunk -> extractText(update.content)?.let { "[thought] $it" }
            else -> null
        }

    fun formatError(message: String): String = "Error: $message"

    fun formatAuthFailure(message: String): String = "Auth failed: $message"

    fun normalizeTranscriptText(text: String): String =
        text
            .replace("\r\n", "\n")
            .replace('\r', '\n')
            .replace(BR_TAG_PATTERN, "\n")

    fun formatTerminalCreate(
        command: String,
        terminalId: String,
    ): String = "[terminal] $command (id=$terminalId)"

    fun formatPermissionDenied(title: String): String = "[permission denied] $title"

    // -- Plain-text helpers ----------------------------------------------------

    internal fun formatToolStatus(
        title: String,
        kind: ToolKind?,
        status: ToolCallStatus?,
    ): String {
        val kindLabel = kind?.name?.lowercase()?.replace('_', ' ') ?: "tool"
        val iconPrefix =
            when (status) {
                ToolCallStatus.COMPLETED -> "✓ "
                ToolCallStatus.FAILED -> "✗ "
                else -> ""
            }
        return "$iconPrefix$kindLabel $title"
    }

    internal fun extractText(content: ContentBlock): String? =
        when (content) {
            is ContentBlock.Text -> content.text
            else -> null
        }
}
