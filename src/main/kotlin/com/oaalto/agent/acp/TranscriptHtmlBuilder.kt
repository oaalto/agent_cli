package com.oaalto.agent.acp

internal object TranscriptHtmlBuilder {
    private const val PRE_STYLE =
        "margin-left:20px;color:#999999;border-left:2px solid #444444;" +
            "padding-left:8px;font-family:monospace;font-size:12px;white-space:pre-wrap"
    private const val MUTED_REFERENCE_STYLE =
        "margin-left:20px;color:#999999;font-family:monospace;font-size:12px"

    fun buildPlainPre(text: String): String {
        val openTag = "<pre style=\"$PRE_STYLE\">"
        val closeTag = "</pre>"
        val maxInnerChars =
            (
                TranscriptToolCallContentRenderer.MAX_TEXT_CHARACTERS -
                    openTag.length -
                    closeTag.length
            ).coerceAtLeast(0)
        val displayText = TranscriptTextTruncation.truncate(text, maxInnerChars)
        return openTag + TranscriptRenderHelpers.escapeHtml(displayText) + closeTag
    }

    fun buildMutedSpan(text: String): String =
        "<span style=\"$MUTED_REFERENCE_STYLE;white-space:pre-wrap\">${TranscriptRenderHelpers.escapeHtml(text)}</span>"

    fun buildStyledSpan(
        text: String,
        runs: List<StyledRun>,
        isHeading: Boolean,
    ): String {
        if (runs.isEmpty()) {
            return buildMutedSpan(text)
        }
        val segments = computeStyledSegments(text, runs)
        val htmlContent = buildStyledHtml(segments)
        val weight = if (isHeading) "font-weight:bold;" else ""
        return "<span style=\"$MUTED_REFERENCE_STYLE;${weight}white-space:pre-wrap\">$htmlContent</span>"
    }

    private fun computeStyledSegments(
        text: String,
        runs: List<StyledRun>,
    ): List<Pair<String, TextStyle?>> {
        val points = mutableSetOf(0, text.length)
        runs.forEach {
            points.add(it.start)
            points.add(it.end)
        }
        val sortedPoints = points.sorted()

        return sortedPoints
            .zipWithNext()
            .mapNotNull { (segStart, segEnd) ->
                if (segStart == segEnd) return@mapNotNull null
                val covering = runs.filter { it.start <= segStart && it.end >= segEnd }
                val segmentText = text.substring(segStart, segEnd)
                segmentText to covering.firstOrNull()?.style
            }
    }

    private fun buildStyledHtml(segments: List<Pair<String, TextStyle?>>): String =
        segments.joinToString("") { (segmentText, style) ->
            renderStyleTag(TranscriptRenderHelpers.escapeHtml(segmentText), style)
        }

    private fun renderStyleTag(
        escaped: String,
        style: TextStyle?,
    ): String =
        when (style) {
            TextStyle.BOLD -> "<strong>$escaped</strong>"
            TextStyle.ITALIC -> "<em>$escaped</em>"
            TextStyle.BOLD_ITALIC -> "<strong><em>$escaped</em></strong>"
            TextStyle.CODE ->
                "<code style='background:#2D2D2D;padding:1px 4px;border-radius:2px;color:#CE9178'>$escaped</code>"
            TextStyle.LINK -> "<span style='color:#569CD6;text-decoration:underline'>$escaped</span>"
            TextStyle.STRIKETHROUGH -> "<s>$escaped</s>"
            null -> escaped
        }
}
