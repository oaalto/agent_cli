package com.oaalto.agent.acp

/** Holds a mutable highlighted-code-block count across recursive rendering passes. */
internal data class HighlightedCounter(
    var value: Int = 0,
)

internal object TranscriptBlockConverter {
    fun convertBlocksToBodyParts(
        blocks: List<RenderedBlock>,
        counter: HighlightedCounter = HighlightedCounter(),
    ): List<TranscriptBodyPart> =
        blocks.flatMap { block ->
            blockToBodyParts(block, counter)
        }

    private fun blockToBodyParts(
        block: RenderedBlock,
        counter: HighlightedCounter,
    ): List<TranscriptBodyPart> =
        when (block) {
            is RenderedBlock.InlineText -> inlineTextToBodyParts(block)
            is RenderedBlock.CodeBlock -> codeBlockToBodyParts(block, counter)
            is RenderedBlock.Table -> listOf(TranscriptBodyPart.Html(TranscriptTableBuilder.buildTableHtml(block)))
            is RenderedBlock.Image -> imageToBodyParts(block)
            is RenderedBlock.ThematicBreak ->
                listOf(TranscriptBodyPart.Html("<hr style='border:none;border-top:1px solid #555;margin:8px 20px'/>"))
            is RenderedBlock.BlockQuote -> blockQuoteToBodyParts(block, counter)
            is RenderedBlock.CustomHtml -> listOf(TranscriptBodyPart.Html(block.html))
        }

    private fun inlineTextToBodyParts(block: RenderedBlock.InlineText): List<TranscriptBodyPart> {
        val displayText = TranscriptTextTruncation.truncate(block.text)
        if (displayText.isEmpty()) return emptyList()
        val html = TranscriptHtmlBuilder.buildStyledSpan(displayText, block.runs, block.headingLevel > 0)
        return listOf(TranscriptBodyPart.Html(html))
    }

    private fun codeBlockToBodyParts(
        block: RenderedBlock.CodeBlock,
        counter: HighlightedCounter,
    ): List<TranscriptBodyPart> {
        val displayCode = TranscriptTextTruncation.truncate(block.code)
        if (displayCode.isEmpty()) return emptyList()
        return if (counter.value < TranscriptToolCallContentRenderer.MAX_HIGHLIGHTED_CODE_BLOCKS) {
            counter.value++
            listOf(TranscriptBodyPart.Code(block.languageId, displayCode))
        } else {
            listOf(TranscriptBodyPart.Html(TranscriptHtmlBuilder.buildPlainPre(displayCode)))
        }
    }

    private fun imageToBodyParts(block: RenderedBlock.Image): List<TranscriptBodyPart> {
        val alt = block.altText
        val url = block.url
        val placeholder = if (url.isNotEmpty()) "[image: $alt] ($url)" else "[image: $alt]"
        val escaped = TranscriptRenderHelpers.escapeHtml(placeholder)
        val html =
            "<span style=\"margin-left:20px;color:#999999;font-family:monospace;font-size:12px\">$escaped</span>"
        return listOf(TranscriptBodyPart.Html(html))
    }

    private fun blockQuoteToBodyParts(
        block: RenderedBlock.BlockQuote,
        counter: HighlightedCounter,
    ): List<TranscriptBodyPart> =
        block.blocks.flatMap { inner ->
            blockToBodyParts(inner, counter)
        }
}
