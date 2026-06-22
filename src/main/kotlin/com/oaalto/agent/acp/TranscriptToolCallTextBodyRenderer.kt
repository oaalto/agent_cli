package com.oaalto.agent.acp

/** Renders tool text bodies as HTML pre blocks, highlighted code parts, or Markdown-rendered blocks. */
internal object TranscriptToolCallTextBodyRenderer {
    fun renderTextBodyParts(text: String): List<TranscriptBodyPart> {
        if (text.isEmpty()) {
            return emptyList()
        }

        // Quick heuristic: skip Markdown parsing if the text looks like plain prose
        if (!TranscriptMarkdownRenderer.likelyContainsMarkdown(text)) {
            return listOf(TranscriptBodyPart.Html(TranscriptHtmlBuilder.buildPlainPre(text)))
        }

        val blocks = TranscriptMarkdownRenderer.parseToBlocks(text)
        if (blocks.isEmpty()) {
            return emptyList()
        }

        return TranscriptBlockConverter.convertBlocksToBodyParts(blocks)
    }
}
