package com.oaalto.agent.acp

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class TranscriptContentRendererTest {
    @Test
    fun `empty text produces no parts`() {
        val parts = TranscriptContentRenderer.renderMarkdownText("")

        assertTrue(parts.isEmpty())
    }

    @Test
    fun `plain text renders escaped pre block`() {
        val parts =
            TranscriptContentRenderer.renderMarkdownText(
                "line one\nline two",
            )

        assertEquals(1, parts.size)
        val html = assertIs<TranscriptBodyPart.Html>(parts.single())
        assertTrue(html.fragment.contains("<pre"))
        assertTrue(html.fragment.contains("border-left:2px solid #444444"))
        assertTrue(html.fragment.contains("color:#999999"))
        assertTrue(html.fragment.contains("line one"))
        assertTrue(html.fragment.contains("line two"))
    }

    @Test
    fun `plain prose skips markdown parse when heuristic enabled`() {
        val parts =
            TranscriptContentRenderer.renderMarkdownText(
                "just some plain log output",
                ContentRenderOptions(useMarkdownHeuristic = true),
            )

        assertEquals(1, parts.size)
        val html = assertIs<TranscriptBodyPart.Html>(parts.single())
        assertTrue(html.fragment.contains("<pre"))
        assertTrue(html.fragment.contains("just some plain log output"))
    }

    @Test
    fun `text escapes html special characters`() {
        val parts =
            TranscriptContentRenderer.renderMarkdownText(
                "<script>alert(1)</script> & \"quotes\"",
            )

        assertEquals(1, parts.size)
        val html = assertIs<TranscriptBodyPart.Html>(parts.single())
        assertTrue(html.fragment.contains("&lt;script&gt;"))
        assertTrue(html.fragment.contains("&amp;"))
        assertTrue(html.fragment.contains("&quot;quotes&quot;"))
        assertTrue(!html.fragment.contains("<script>"))
    }

    @Test
    fun `very long text truncates with character total suffix`() {
        val longText = "x".repeat(TranscriptContentRenderer.MAX_TEXT_CHARACTERS + 100)
        val parts = TranscriptContentRenderer.renderMarkdownText(longText)

        assertEquals(1, parts.size)
        val html = assertIs<TranscriptBodyPart.Html>(parts.single())
        assertTrue(html.fragment.contains("… (truncated, ${longText.length} characters total)"))
        assertTrue(html.fragment.length < longText.length)
    }

    @Test
    fun `fenced kotlin emits highlighted code body part`() {
        val parts =
            TranscriptContentRenderer.renderMarkdownText(
                "```kotlin\nfun main()\n```",
            )

        assertEquals(1, parts.size)
        val code = assertIs<TranscriptBodyPart.Code>(parts.single())
        assertEquals("kotlin", code.languageId)
        assertEquals("fun main()", code.code)
    }

    @Test
    fun `gfm table renders html table body part`() {
        val input =
            """
            | H1 | H2 |
            | --- | --- |
            | A1 | A2 |
            """.trimMargin()

        val parts = TranscriptContentRenderer.renderMarkdownText(input)

        assertEquals(1, parts.size)
        val html = assertIs<TranscriptBodyPart.Html>(parts.single())
        assertTrue(html.fragment.contains("<table"))
        assertTrue(html.fragment.contains("H1"))
        assertTrue(html.fragment.contains("A2"))
    }

    @Test
    fun `code blocks beyond highlight cap render as plain pre`() {
        val fences =
            (1..TranscriptContentRenderer.MAX_HIGHLIGHTED_CODE_BLOCKS + 2).joinToString("\n") { index ->
                "```kotlin\nfun block$index()\n```"
            }

        val parts = TranscriptContentRenderer.renderMarkdownText(fences)
        val highlighted = parts.filterIsInstance<TranscriptBodyPart.Code>()
        val plainPre =
            parts
                .filterIsInstance<TranscriptBodyPart.Html>()
                .filter { it.fragment.contains("<pre") }

        assertEquals(TranscriptContentRenderer.MAX_HIGHLIGHTED_CODE_BLOCKS, highlighted.size)
        assertEquals(2, plainPre.size)
    }
}
