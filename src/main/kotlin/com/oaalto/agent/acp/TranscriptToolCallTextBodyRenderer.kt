package com.oaalto.agent.acp

/** Holds a mutable highlighted-code-block count across recursive rendering passes. */
private data class HighlightedCounter(
    var value: Int = 0,
)

/** Renders tool text bodies as HTML pre blocks, highlighted code parts, or Markdown-rendered blocks. */
@Suppress("TooManyFunctions")
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

        // Quick heuristic: skip Markdown parsing if the text looks like plain prose
        if (!TranscriptMarkdownRenderer.likelyContainsMarkdown(text)) {
            return listOf(TranscriptBodyPart.Html(renderPlainPreBody(text)))
        }

        val blocks = TranscriptMarkdownRenderer.parseToBlocks(text)
        if (blocks.isEmpty()) {
            return emptyList()
        }

        val counter = HighlightedCounter()
        return blocks.flatMap { block ->
            blockToBodyParts(block, counter)
        }
    }

    private fun blockToBodyParts(
        block: RenderedBlock,
        counter: HighlightedCounter,
    ): List<TranscriptBodyPart> =
        when (block) {
            is RenderedBlock.InlineText -> inlineTextToBodyParts(block)
            is RenderedBlock.CodeBlock -> codeBlockToBodyParts(block, counter)
            is RenderedBlock.Table -> listOf(TranscriptBodyPart.Html(renderTableHtml(block)))
            is RenderedBlock.Image -> imageToBodyParts(block)
            is RenderedBlock.ThematicBreak ->
                listOf(TranscriptBodyPart.Html("<hr style='border:none;border-top:1px solid #555;margin:8px 20px'/>"))
            is RenderedBlock.BlockQuote -> blockQuoteToBodyParts(block, counter)
            is RenderedBlock.CustomHtml -> listOf(TranscriptBodyPart.Html(block.html))
        }

    private fun inlineTextToBodyParts(block: RenderedBlock.InlineText): List<TranscriptBodyPart> {
        val displayText = truncateText(block.text)
        if (displayText.isEmpty()) return emptyList()
        val html = renderStyledInlineHtml(displayText, block.runs, block.headingLevel > 0)
        return listOf(TranscriptBodyPart.Html(html))
    }

    private fun codeBlockToBodyParts(
        block: RenderedBlock.CodeBlock,
        counter: HighlightedCounter,
    ): List<TranscriptBodyPart> {
        val displayCode = truncateText(block.code)
        if (displayCode.isEmpty()) return emptyList()
        return if (counter.value < TranscriptToolCallContentRenderer.MAX_HIGHLIGHTED_CODE_BLOCKS) {
            counter.value++
            listOf(TranscriptBodyPart.Code(block.languageId, displayCode))
        } else {
            listOf(TranscriptBodyPart.Html(renderPlainPreBody(displayCode)))
        }
    }

    private fun imageToBodyParts(block: RenderedBlock.Image): List<TranscriptBodyPart> {
        val alt = block.altText
        val url = block.url
        val placeholder = if (url.isNotEmpty()) "[image: $alt] ($url)" else "[image: $alt]"
        return listOf(TranscriptBodyPart.Html(renderSpanBody(placeholder)))
    }

    private fun blockQuoteToBodyParts(
        block: RenderedBlock.BlockQuote,
        counter: HighlightedCounter,
    ): List<TranscriptBodyPart> =
        block.blocks.flatMap { inner ->
            blockToBodyParts(inner, counter)
        }

    private fun renderTableHtml(table: RenderedBlock.Table): String {
        val escapedHeaders = table.headers.map { escapeHtml(it) }
        val escapedRows = table.rows.map { row -> row.map { escapeHtml(it) } }
        val alignStyles =
            table.alignments.map { align ->
                when (align) {
                    TableAlignment.LEFT -> "text-align:left"
                    TableAlignment.CENTER -> "text-align:center"
                    TableAlignment.RIGHT -> "text-align:right"
                }
            }
        val cellStyle = "border:1px solid #555;padding:4px"
        val headerCells =
            escapedHeaders
                .mapIndexed { i, h ->
                    val align = alignStyles.getOrElse(i) { "text-align:left" }
                    "<th style='$cellStyle;$align'>$h</th>"
                }.joinToString("")
        val bodyCells =
            escapedRows.joinToString("\n") { row ->
                "<tr>${
                    row.mapIndexed { i, cell ->
                        val align = alignStyles.getOrElse(i) { "text-align:left" }
                        "<td style='$cellStyle;$align'>$cell</td>"
                    }.joinToString("")
                }</tr>"
            }
        return (
            "<div style='margin-left:20px;overflow-x:auto'>" +
                "<table style='border-collapse:collapse;width:auto'>" +
                "<thead><tr>$headerCells</tr></thead>" +
                "<tbody>$bodyCells</tbody>" +
                "</table></div>"
        )
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

    private fun renderSpanBody(text: String): String {
        val displayText = truncateText(text)
        return "<span style=\"$MUTED_REFERENCE_STYLE;white-space:pre-wrap\">${escapeHtml(displayText)}</span>"
    }

    private fun truncateText(
        text: String,
        maxCharacters: Int = TranscriptToolCallContentRenderer.MAX_TEXT_CHARACTERS,
    ): String = TranscriptTextTruncation.truncate(text, maxCharacters)

    private fun escapeHtml(text: String): String = TranscriptRenderHelpers.escapeHtml(text)

    private fun renderStyledInlineHtml(
        text: String,
        runs: List<StyledRun>,
        isHeading: Boolean,
    ): String {
        if (runs.isEmpty()) {
            return renderUnstyledSpan(text)
        }
        val segments = computeStyledSegments(text, runs)
        val htmlContent = buildStyledHtml(segments)
        val weight = if (isHeading) "font-weight:bold;" else ""
        return "<span style=\"$MUTED_REFERENCE_STYLE;${weight}white-space:pre-wrap\">$htmlContent</span>"
    }

    private fun renderUnstyledSpan(text: String): String =
        "<span style=\"$MUTED_REFERENCE_STYLE;white-space:pre-wrap\">${escapeHtml(text)}</span>"

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
            renderStyleTag(escapeHtml(segmentText), style)
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
