package com.oaalto.agent.acp

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TranscriptStreamingCursorTest {
    @Test
    fun `stream block includes cursor entity and agent styling`() {
        val block = TranscriptStreamingCursor.streamBlockHtml("Hello", "")

        assertTrue(block.contains("Hello"))
        assertTrue(block.contains(TranscriptStreamingCursor.CURSOR_HTML))
        assertTrue(TranscriptStreamingCursor.hasCursor(block))
        assertTrue(block.contains("color:#d4d4d4"))
        assertTrue(block.contains("monospace"))
    }

    @Test
    fun `finalized block removes cursor`() {
        val block = TranscriptStreamingCursor.finalizedBlockHtml("Done", "")

        assertTrue(block.contains("Done"))
        assertFalse(TranscriptStreamingCursor.hasCursor(block))
    }

    @Test
    fun `stripCursor removes entity and preserves text`() {
        val block = TranscriptStreamingCursor.streamBlockHtml("Hello", "")
        val stripped = TranscriptStreamingCursor.stripCursor(block)

        assertTrue(stripped.contains("Hello"))
        assertFalse(TranscriptStreamingCursor.hasCursor(stripped))
    }

    @Test
    fun `escaped special characters remain safe in stream block`() {
        val escaped = TranscriptRenderHelpers.escapeHtml("<a & b>")
        val block = TranscriptStreamingCursor.streamBlockHtml(escaped, "")

        assertTrue(block.contains("&lt;a &amp; b&gt;"))
        assertFalse(block.contains("<a & b>"))
    }

    @Test
    fun `hasCursor recognizes hex entity form`() {
        assertTrue(TranscriptStreamingCursor.hasCursor("Hello&#x258a;"))
    }

    @Test
    fun `hasCursor recognizes literal unicode character`() {
        assertTrue(TranscriptStreamingCursor.hasCursor("Hello\u258a"))
    }

    @Test
    fun `double stripCursor is idempotent`() {
        val block = TranscriptStreamingCursor.streamBlockHtml("x", "")
        val once = TranscriptStreamingCursor.stripCursor(block)
        val twice = TranscriptStreamingCursor.stripCursor(once)
        assertEquals(once, twice)
    }
}
