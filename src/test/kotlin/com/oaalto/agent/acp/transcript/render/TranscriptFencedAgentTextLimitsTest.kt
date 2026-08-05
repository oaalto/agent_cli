package com.oaalto.agent.acp.transcript.render

import com.oaalto.agent.acp.transcript.model.TranscriptBodyPart
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TranscriptFencedAgentTextLimitsTest {
    @Test
    fun `markdown code blocks beyond highlight cap remain as code blocks`() {
        val fences =
            (1..TranscriptContentRenderer.MAX_HIGHLIGHTED_CODE_BLOCKS + 2).joinToString("\n") { index ->
                "```kotlin\nfun block$index()\n```"
            }
        val parts =
            TranscriptContentRenderer.renderMarkdownText(
                fences,
                ContentRenderOptions.AGENT_TEXT,
            )
        val codeParts = parts.filterIsInstance<TranscriptBodyPart.Code>()
        val plainPre =
            parts
                .filterIsInstance<TranscriptBodyPart.Html>()
                .filter { it.fragment.contains("<pre") }

        assertEquals(
            TranscriptContentRenderer.MAX_HIGHLIGHTED_CODE_BLOCKS + 2,
            codeParts.size + plainPre.size,
        )
        val firstCode = codeParts.first()
        val truncated = TranscriptTextTruncation.truncate(firstCode.code)
        assertTrue(truncated.length <= TranscriptContentRenderer.MAX_TEXT_CHARACTERS)
    }
}
