package com.oaalto.agent.acp

import com.agentclientprotocol.model.ContentBlock
import com.agentclientprotocol.model.SessionUpdate

internal object TranscriptUpdateRenderer {
    private const val HTML_FONT_FAMILY = "monospace"
    private const val HTML_FONT_SIZE = "12px"

    /**
     * Returns an HTML fragment (inline `<span>` tags) for each entry produced by the
     * given [update]. Callers must initialize a `<html><body>` wrapper before appending.
     */
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
        listOfNotNull(
            extractText(update.content)?.let {
                agentSegment(escapeHtml(it))
            },
        )

    private fun renderAgentThoughtChunk(update: SessionUpdate.AgentThoughtChunk): List<String> =
        listOfNotNull(
            extractText(update.content)?.let {
                thoughtSegment(escapeHtml(it))
            },
        )

    private fun renderUserMessageChunk(update: SessionUpdate.UserMessageChunk): List<String> =
        listOfNotNull(
            extractText(update.content)?.let {
                userSegment(escapeHtml(it))
            },
        )

    private fun renderToolCall(update: SessionUpdate.ToolCall): List<String> =
        listOf(
            TranscriptRenderHelpers.formatToolStatusHtml(
                title = update.title,
                kind = update.kind,
                status = update.status,
            ),
        )

    private fun renderToolCallUpdate(update: SessionUpdate.ToolCallUpdate): List<String> =
        listOf(
            TranscriptRenderHelpers.formatToolStatusHtml(
                title = update.title ?: update.toolCallId.value,
                kind = update.kind,
                status = update.status,
            ),
        )

    // -- HTML segment helpers -------------------------------------------------

    private fun agentSegment(text: String): String =
        "<span style=\"color:#d4d4d4;font-family:$HTML_FONT_FAMILY;font-size:$HTML_FONT_SIZE\">$text</span>"

    private fun thoughtSegment(text: String): String =
        "<span style=\"color:#808080;font-family:$HTML_FONT_FAMILY;font-size:$HTML_FONT_SIZE\">[thought] $text</span>"

    private fun userSegment(text: String): String =
        "<span style=\"color:#569cd6;font-family:$HTML_FONT_FAMILY;font-size:$HTML_FONT_SIZE\">&gt; $text</span>"

    internal fun escapeHtml(text: String): String =
        text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")

    private fun extractText(content: ContentBlock): String? =
        when (content) {
            is ContentBlock.Text -> content.text
            else -> null
        }
}
