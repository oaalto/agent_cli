@file:Suppress("OPT_IN_USAGE")

package com.oaalto.agent.acp

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TranscriptFencedAgentTextLimitsTest {
    @Test
    fun `segmented agent code beyond highlight cap stays plain prose parts`() {
        val fences =
            (1..TranscriptToolCallContentRenderer.MAX_HIGHLIGHTED_CODE_BLOCKS + 2).joinToString("\n") { index ->
                "```kotlin\nfun block$index()\n```"
            }
        val segments = segmentFencedCodeBlocks(fences)
        val codeSegments = segments.filterIsInstance<TextSegment.Code>()

        assertEquals(
            TranscriptToolCallContentRenderer.MAX_HIGHLIGHTED_CODE_BLOCKS + 2,
            codeSegments.size,
        )
        val truncated = TranscriptTextTruncation.truncate(codeSegments.first().text)
        assertTrue(truncated.length <= TranscriptToolCallContentRenderer.MAX_TEXT_CHARACTERS)
    }
}
