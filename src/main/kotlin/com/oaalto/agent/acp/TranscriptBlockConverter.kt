package com.oaalto.agent.acp

/** Holds a mutable highlighted-code-block count across recursive rendering passes. */
internal data class HighlightedCounter(
    var value: Int = 0,
)

internal object TranscriptBlockConverter {
    fun convertBlocksToBodyParts(
        blocks: List<RenderedBlock>,
        counter: HighlightedCounter = HighlightedCounter(),
        maxHighlightedCodeBlocks: Int = TranscriptContentRenderer.MAX_HIGHLIGHTED_CODE_BLOCKS,
        maxTextCharacters: Int = TranscriptContentRenderer.MAX_TEXT_CHARACTERS,
    ): List<TranscriptBodyPart> =
        blocks.flatMap { block ->
            blockToBodyParts(block, counter, maxHighlightedCodeBlocks, maxTextCharacters)
        }

    private fun blockToBodyParts(
        block: RenderedBlock,
        counter: HighlightedCounter,
        maxHighlightedCodeBlocks: Int,
        maxTextCharacters: Int,
    ): List<TranscriptBodyPart> =
        when (block) {
            is RenderedBlock.InlineText ->
                listOfNotNull(inlineTextToBodyPart(block, maxTextCharacters))
            is RenderedBlock.CodeBlock ->
                listOfNotNull(
                    codeBlockToBodyPart(
                        block,
                        counter,
                        maxHighlightedCodeBlocks,
                        maxTextCharacters,
                    ),
                )
            is RenderedBlock.Table -> listOf(TranscriptBodyPart.Html(TranscriptTableBuilder.buildTableHtml(block)))
            is RenderedBlock.Image -> listOf(TranscriptBodyPart.Image(block.altText, block.url))
            is RenderedBlock.ThematicBreak -> listOf(TranscriptBodyPart.ThematicBreak)
            is RenderedBlock.BlockQuote ->
                blockQuoteToBodyParts(
                    block,
                    counter,
                    maxHighlightedCodeBlocks,
                    maxTextCharacters,
                )
            is RenderedBlock.CustomHtml -> listOf(TranscriptBodyPart.Html(block.html))
        }

    private fun inlineTextToBodyPart(
        block: RenderedBlock.InlineText,
        maxTextCharacters: Int,
    ): TranscriptBodyPart? {
        val displayText = TranscriptTextTruncation.truncate(block.text, maxTextCharacters)
        if (displayText.isEmpty()) return null
        return when {
            block.headingLevel > 0 -> TranscriptBodyPart.Heading(block.headingLevel, displayText, block.runs)
            block.listMarker != null -> TranscriptBodyPart.ListLine(block.listMarker, displayText, block.runs)
            else -> TranscriptBodyPart.InlineText(displayText, block.runs)
        }
    }

    private fun codeBlockToBodyPart(
        block: RenderedBlock.CodeBlock,
        counter: HighlightedCounter,
        maxHighlightedCodeBlocks: Int,
        maxTextCharacters: Int,
    ): TranscriptBodyPart? {
        val displayCode = TranscriptTextTruncation.truncate(block.code, maxTextCharacters)
        if (displayCode.isEmpty()) return null
        return if (counter.value < maxHighlightedCodeBlocks) {
            counter.value++
            TranscriptBodyPart.Code(block.languageId, displayCode)
        } else {
            TranscriptBodyPart.Html(TranscriptHtmlBuilder.buildPlainPre(displayCode, maxTextCharacters))
        }
    }

    private fun blockQuoteToBodyParts(
        block: RenderedBlock.BlockQuote,
        counter: HighlightedCounter,
        maxHighlightedCodeBlocks: Int,
        maxTextCharacters: Int,
    ): List<TranscriptBodyPart> {
        val inner =
            block.blocks.flatMap { inner ->
                blockToBodyParts(inner, counter, maxHighlightedCodeBlocks, maxTextCharacters)
            }
        return listOf(TranscriptBodyPart.BlockQuote(inner))
    }
}
