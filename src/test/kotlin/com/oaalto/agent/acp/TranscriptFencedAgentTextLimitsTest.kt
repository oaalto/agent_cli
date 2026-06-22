package com.oaalto.agent.acp

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TranscriptFencedAgentTextLimitsTest {
    @Test
    fun `markdown code blocks beyond highlight cap remain as code blocks`() {
        val fences =
            (1..TranscriptToolCallContentRenderer.MAX_HIGHLIGHTED_CODE_BLOCKS + 2).joinToString("\n") { index ->
                "```kotlin\nfun block$index()\n```"
            }
        val blocks = TranscriptMarkdownRenderer.parseToBlocks(fences)
        val codeBlocks = blocks.filterIsInstance<RenderedBlock.CodeBlock>()

        assertEquals(
            TranscriptToolCallContentRenderer.MAX_HIGHLIGHTED_CODE_BLOCKS + 2,
            codeBlocks.size,
        )
        val truncated = TranscriptTextTruncation.truncate(codeBlocks.first().code)
        assertTrue(truncated.length <= TranscriptToolCallContentRenderer.MAX_TEXT_CHARACTERS)
    }
}
