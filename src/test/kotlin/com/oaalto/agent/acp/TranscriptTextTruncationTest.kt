package com.oaalto.agent.acp

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TranscriptTextTruncationTest {
    @Test
    fun `truncate leaves short text unchanged`() {
        assertEquals("hello", TranscriptTextTruncation.truncate("hello"))
    }

    @Test
    fun `truncate appends total character suffix`() {
        val longText = "x".repeat(TranscriptToolCallContentRenderer.MAX_TEXT_CHARACTERS + 100)
        val truncated = TranscriptTextTruncation.truncate(longText)

        assertTrue(truncated.contains("… (truncated, ${longText.length} characters total)"))
        assertTrue(truncated.length < longText.length)
    }
}
