package com.oaalto.agent.acp

/** Renders tool text bodies as HTML pre blocks or highlighted code parts. */
internal object TranscriptToolCallTextBodyRenderer {
    private const val PRE_STYLE =
        "margin-left:20px;color:#999999;border-left:2px solid #444444;" +
            "padding-left:8px;font-family:monospace;font-size:12px;white-space:pre-wrap"
    private const val MUTED_REFERENCE_STYLE =
        "margin-left:20px;color:#999999;font-family:monospace;font-size:12px"

    fun renderTextBodyParts(text: String): List<TranscriptBodyPart> {
        if (text.isEmpty()) {
            return emptyList()
        }
        val segments = segmentFencedCodeBlocks(text)
        if (!segments.containsCode()) {
            return listOf(TranscriptBodyPart.Html(renderPlainPreBody(text)))
        }
        var highlightedBlocks = 0
        return segments.mapNotNull { segment ->
            when (segment) {
                is TextSegment.Prose ->
                    segment.text
                        .takeIf { it.isNotEmpty() }
                        ?.let { TranscriptBodyPart.Html(renderProseBody(it)) }
                is TextSegment.Code -> {
                    val displayCode = truncateText(segment.text)
                    if (highlightedBlocks < TranscriptToolCallContentRenderer.MAX_HIGHLIGHTED_CODE_BLOCKS) {
                        highlightedBlocks += 1
                        TranscriptBodyPart.Code(segment.languageId, displayCode)
                    } else {
                        TranscriptBodyPart.Html(renderPlainPreBody(displayCode))
                    }
                }
            }
        }
    }

    fun renderPlainPreBody(text: String): String {
        val openTag = "<pre style=\"$PRE_STYLE\">"
        val closeTag = "</pre>"
        val maxInnerChars =
            (
                TranscriptToolCallContentRenderer.MAX_TEXT_CHARACTERS -
                    openTag.length -
                    closeTag.length
            ).coerceAtLeast(0)
        val displayText = truncateText(text, maxInnerChars)
        return openTag + escapeHtml(displayText) + closeTag
    }

    private fun renderProseBody(text: String): String {
        val displayText = truncateText(text)
        return "<span style=\"$MUTED_REFERENCE_STYLE;white-space:pre-wrap\">${escapeHtml(displayText)}</span>"
    }

    private fun truncateText(
        text: String,
        maxCharacters: Int = TranscriptToolCallContentRenderer.MAX_TEXT_CHARACTERS,
    ): String = TranscriptTextTruncation.truncate(text, maxCharacters)

    private fun escapeHtml(text: String): String = TranscriptRenderHelpers.escapeHtml(text)
}
