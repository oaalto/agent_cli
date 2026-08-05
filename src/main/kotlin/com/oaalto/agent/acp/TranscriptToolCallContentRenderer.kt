package com.oaalto.agent.acp

import com.agentclientprotocol.model.ContentBlock
import com.agentclientprotocol.model.EmbeddedResourceResource
import com.agentclientprotocol.model.ToolCallContent
import com.agentclientprotocol.model.ToolCallStatus

/**
 * Maps [ToolCallContent] entries to body parts below tool-call badge headers.
 *
 * Bodies render only when [status] is [ToolCallStatus.COMPLETED] or [ToolCallStatus.FAILED].
 */
internal object TranscriptToolCallContentRenderer {
    /** @see TranscriptContentRenderer.MAX_TEXT_CHARACTERS */
    internal const val MAX_TEXT_CHARACTERS: Int = TranscriptContentRenderer.MAX_TEXT_CHARACTERS

    /** @see TranscriptContentRenderer.MAX_HIGHLIGHTED_CODE_BLOCKS */
    internal const val MAX_HIGHLIGHTED_CODE_BLOCKS: Int = TranscriptContentRenderer.MAX_HIGHLIGHTED_CODE_BLOCKS

    private const val PRE_STYLE =
        "margin-left:20px;color:#999999;border-left:2px solid #444444;" +
            "padding-left:8px;font-family:monospace;font-size:12px;white-space:pre-wrap"
    private const val MUTED_REFERENCE_STYLE =
        "margin-left:20px;color:#999999;font-family:monospace;font-size:12px"

    fun renderBodyParts(
        content: List<ToolCallContent>?,
        status: ToolCallStatus?,
    ): List<TranscriptBodyPart> {
        if (!shouldRenderContent(status) || content.isNullOrEmpty()) {
            return emptyList()
        }
        return content.flatMap(::renderToolCallContentParts)
    }

    fun renderContentFragments(
        content: List<ToolCallContent>?,
        status: ToolCallStatus?,
    ): List<String> = renderBodyParts(content, status).map(TranscriptHtmlBuilder::bodyPartToHtmlFragment)

    private fun shouldRenderContent(status: ToolCallStatus?): Boolean =
        status == ToolCallStatus.COMPLETED || status == ToolCallStatus.FAILED

    private fun renderToolCallContentParts(content: ToolCallContent): List<TranscriptBodyPart> =
        when (content) {
            is ToolCallContent.Content -> renderContentBlockParts(content.content)
            is ToolCallContent.Diff ->
                listOf(
                    TranscriptBodyPart.Html(
                        TranscriptToolCallDiffRenderer.render(content, PRE_STYLE),
                    ),
                )
            is ToolCallContent.Terminal ->
                listOf(TranscriptBodyPart.Html(renderTerminal(content)))
        }

    private fun renderContentBlockParts(block: ContentBlock): List<TranscriptBodyPart> =
        when (block) {
            is ContentBlock.Text ->
                TranscriptContentRenderer.renderMarkdownText(
                    block.text,
                    ContentRenderOptions.forToolMarkdownText(block.text),
                )
            is ContentBlock.Image ->
                listOf(TranscriptBodyPart.Html(renderPrePlaceholder("[image: ${block.mimeType}]")))
            is ContentBlock.Audio ->
                listOf(TranscriptBodyPart.Html(renderPrePlaceholder("[audio: ${block.mimeType}]")))
            is ContentBlock.ResourceLink ->
                listOf(TranscriptBodyPart.Html(renderPrePlaceholder("${block.name} (${block.uri})")))
            is ContentBlock.Resource -> renderEmbeddedResourceParts(block.resource)
        }

    private fun renderEmbeddedResourceParts(resource: EmbeddedResourceResource): List<TranscriptBodyPart> =
        when (resource) {
            is EmbeddedResourceResource.TextResourceContents ->
                TranscriptContentRenderer.renderMarkdownText(
                    resource.text,
                    ContentRenderOptions.forToolMarkdownText(resource.text),
                )
            is EmbeddedResourceResource.BlobResourceContents ->
                listOf(TranscriptBodyPart.Html(renderPrePlaceholder("[binary resource: ${resource.uri}]")))
        }

    private fun renderPrePlaceholder(text: String): String =
        TranscriptHtmlBuilder.buildPlainPre(text.replace("\n", " "))

    private fun renderTerminal(terminal: ToolCallContent.Terminal): String {
        val escapedId = escapeHtml(terminal.terminalId)
        return (
            "<span style=\"$MUTED_REFERENCE_STYLE\">" +
                "[terminal output] id=$escapedId</span>"
        )
    }

    private fun escapeHtml(text: String): String = TranscriptRenderHelpers.escapeHtml(text)
}
