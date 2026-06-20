package com.oaalto.agent.acp

import com.agentclientprotocol.model.ContentBlock
import com.agentclientprotocol.model.EmbeddedResourceResource
import com.agentclientprotocol.model.ToolCallContent
import com.agentclientprotocol.model.ToolCallStatus

/**
 * Maps [ToolCallContent] entries to HTML body fragments below tool-call badge headers.
 *
 * Bodies render only when [status] is [ToolCallStatus.COMPLETED] or [ToolCallStatus.FAILED].
 */
internal object TranscriptToolCallContentRenderer {
    /** Maximum characters rendered in a single text or diff body before truncation. */
    internal const val MAX_TEXT_CHARACTERS: Int = 16 * 1024

    private const val PRE_STYLE =
        "margin-left:20px;color:#999999;border-left:2px solid #444444;" +
            "padding-left:8px;font-family:monospace;font-size:12px;white-space:pre-wrap"
    private const val MUTED_REFERENCE_STYLE =
        "margin-left:20px;color:#999999;font-family:monospace;font-size:12px"

    fun renderContentFragments(
        content: List<ToolCallContent>?,
        status: ToolCallStatus?,
    ): List<String> {
        if (!shouldRenderContent(status) || content.isNullOrEmpty()) {
            return emptyList()
        }
        return content.mapNotNull(::renderToolCallContent)
    }

    private fun shouldRenderContent(status: ToolCallStatus?): Boolean =
        status == ToolCallStatus.COMPLETED || status == ToolCallStatus.FAILED

    private fun renderToolCallContent(content: ToolCallContent): String? =
        when (content) {
            is ToolCallContent.Content -> renderContentBlock(content.content)
            is ToolCallContent.Diff -> TranscriptToolCallDiffRenderer.render(content, PRE_STYLE)
            is ToolCallContent.Terminal -> renderTerminal(content)
        }

    private fun renderContentBlock(block: ContentBlock): String? =
        when (block) {
            is ContentBlock.Text -> renderTextBody(block.text)
            is ContentBlock.Image -> renderPrePlaceholder("[image: ${block.mimeType}]")
            is ContentBlock.Audio -> renderPrePlaceholder("[audio: ${block.mimeType}]")
            is ContentBlock.ResourceLink ->
                renderPrePlaceholder("${block.name} (${block.uri})")
            is ContentBlock.Resource -> renderEmbeddedResource(block.resource)
        }

    private fun renderEmbeddedResource(resource: EmbeddedResourceResource): String? =
        when (resource) {
            is EmbeddedResourceResource.TextResourceContents -> renderTextBody(resource.text)
            is EmbeddedResourceResource.BlobResourceContents ->
                renderPrePlaceholder("[binary resource: ${resource.uri}]")
        }

    private fun renderTextBody(text: String): String? {
        if (text.isEmpty()) {
            return null
        }
        val openTag = "<pre style=\"$PRE_STYLE\">"
        val closeTag = "</pre>"
        val maxInnerChars = (MAX_TEXT_CHARACTERS - openTag.length - closeTag.length).coerceAtLeast(0)
        val displayText = truncateText(text, maxInnerChars)
        return openTag + escapeHtml(displayText) + closeTag
    }

    private fun renderPrePlaceholder(text: String): String = "<pre style=\"$PRE_STYLE\">${escapeHtml(text)}</pre>"

    private fun renderTerminal(terminal: ToolCallContent.Terminal): String {
        val escapedId = escapeHtml(terminal.terminalId)
        return (
            "<span style=\"$MUTED_REFERENCE_STYLE\">" +
                "[terminal output] id=$escapedId</span>"
        )
    }

    private fun truncateText(
        text: String,
        maxCharacters: Int = MAX_TEXT_CHARACTERS,
    ): String {
        if (text.length <= maxCharacters) {
            return text
        }
        val suffix = "\n… (truncated, ${text.length} characters total)"
        val keepLength = (maxCharacters - suffix.length).coerceAtLeast(0)
        return text.take(keepLength) + suffix
    }

    private fun escapeHtml(text: String): String = TranscriptUpdateRenderer.escapeHtml(text)
}
