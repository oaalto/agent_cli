package com.oaalto.agent.acp

import com.oaalto.agent.acp.transcript.model.TranscriptBodyPart

/** Options for [TranscriptContentRenderer.renderMarkdownText]. */
internal data class ContentRenderOptions(
    val maxTextCharacters: Int = TranscriptContentRenderer.MAX_TEXT_CHARACTERS,
    val maxHighlightedCodeBlocks: Int = TranscriptContentRenderer.MAX_HIGHLIGHTED_CODE_BLOCKS,
    val useMarkdownHeuristic: Boolean = true,
    val applyFenceNormalization: Boolean = false,
) {
    companion object {
        val DEFAULT: ContentRenderOptions = ContentRenderOptions()

        /** Options for final agent markdown rows: always parse, normalize malformed fences. */
        val AGENT_TEXT: ContentRenderOptions =
            ContentRenderOptions(
                useMarkdownHeuristic = false,
                applyFenceNormalization = true,
            )

        /** Tool text bodies: heuristic skip for plain dumps; normalize fences when markdown-like. */
        fun forToolMarkdownText(text: String): ContentRenderOptions =
            if (text.contains("```") || TranscriptMarkdownRenderer.likelyContainsMarkdown(text)) {
                ContentRenderOptions(applyFenceNormalization = true)
            } else {
                DEFAULT
            }
    }
}

/** Canonical seam for markdown-ish text → [TranscriptBodyPart] conversion. */
internal object TranscriptContentRenderer {
    /** Maximum characters rendered in a single text or code body before truncation. */
    const val MAX_TEXT_CHARACTERS: Int = 16 * 1024

    /** Maximum fenced code blocks highlighted per text body; additional blocks stay plain pre. */
    const val MAX_HIGHLIGHTED_CODE_BLOCKS: Int = 8

    fun renderMarkdownText(
        text: String,
        options: ContentRenderOptions = ContentRenderOptions.DEFAULT,
    ): List<TranscriptBodyPart> {
        if (text.isEmpty()) {
            return emptyList()
        }

        val sourceText =
            if (options.applyFenceNormalization) {
                normalizeAgentFences(text)
            } else {
                text
            }

        if (options.useMarkdownHeuristic && !TranscriptMarkdownRenderer.likelyContainsMarkdown(sourceText)) {
            return listOf(
                TranscriptBodyPart.Html(
                    TranscriptHtmlBuilder.buildPlainPre(sourceText, options.maxTextCharacters),
                ),
            )
        }

        val blocks = TranscriptMarkdownRenderer.parseToBlocks(sourceText)
        if (blocks.isEmpty()) {
            return emptyList()
        }

        return TranscriptBlockConverter.convertBlocksToBodyParts(
            blocks = blocks,
            maxHighlightedCodeBlocks = options.maxHighlightedCodeBlocks,
            maxTextCharacters = options.maxTextCharacters,
        )
    }
}
