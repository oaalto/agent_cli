package com.oaalto.agent.acp

import com.agentclientprotocol.model.ToolCallStatus
import com.agentclientprotocol.model.ToolKind

/**
 * Plain-text formatting for the transcript.
 *
 * Narrow scope: error/auth formatting, text normalization, terminal &
 * permission formatting, and tool-status labels. Event mapping and text
 * extraction live in [TranscriptEventIngestion]. Content-fragment rendering
 * is delegated to [TranscriptToolCallContentRenderer].
 */
object TranscriptRenderer {
    private val BR_TAG_PATTERN = Regex("(?i)<br\\s*/?>")

    /** Cursor ACP tool call ids (e.g. `call-<uuid>_0`); not useful as a visible title. */
    private val TOOL_CALL_ID_TITLE_PATTERN = Regex("^call-[\\w-]+$", RegexOption.IGNORE_CASE)

    /**
     * Returns a human-readable tool title for transcript UI, or empty when the value is
     * missing or is the opaque ACP tool call id (badge already shows kind).
     */
    fun displayToolTitle(
        title: String?,
        toolCallId: String? = null,
    ): String {
        val trimmed = title?.trim().orEmpty()
        val visible =
            trimmed.takeUnless {
                it.isEmpty() ||
                    (toolCallId != null && it == toolCallId) ||
                    TOOL_CALL_ID_TITLE_PATTERN.matches(it)
            }
        return visible.orEmpty()
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
}
