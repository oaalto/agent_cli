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
                "alert(1) & \"quotes\"",
                ContentRenderOptions(useMarkdownHeuristic = true),
            )

        assertEquals(1, parts.size)
        val html = assertIs<TranscriptBodyPart.Html>(parts.single())
        assertTrue(html.fragment.contains("&amp;"))
        assertTrue(html.fragment.contains("&quot;quotes&quot;"))
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

    private val agentOptions: ContentRenderOptions = ContentRenderOptions.AGENT_TEXT

    @Test
    fun `agent plain text yields InlineText body part`() {
        val parts =
            TranscriptContentRenderer.renderMarkdownText(
                "line one\nline two",
                agentOptions,
            )

        assertEquals(1, parts.size)
        val text = assertIs<TranscriptBodyPart.InlineText>(parts.single())
        assertTrue(text.text.contains("line one"))
        assertTrue(text.text.contains("line two"))
    }

    @Test
    fun `agent heading yields Heading body part`() {
        val parts =
            TranscriptContentRenderer.renderMarkdownText(
                "# Title",
                agentOptions,
            )

        assertEquals(1, parts.size)
        val heading = assertIs<TranscriptBodyPart.Heading>(parts.single())
        assertEquals(1, heading.level)
        assertEquals("Title", heading.text.trim())
    }

    @Test
    fun `agent bold text yields InlineText with styled run`() {
        val parts =
            TranscriptContentRenderer.renderMarkdownText(
                "**bold** text",
                agentOptions,
            )

        assertEquals(1, parts.size)
        val text = assertIs<TranscriptBodyPart.InlineText>(parts.single())
        assertEquals("bold text", text.text)
        assertEquals(1, text.runs.size)
        assertEquals(TextStyle.BOLD, text.runs.single().style)
    }

    @Test
    fun `agent list item yields ListLine body part`() {
        val parts =
            TranscriptContentRenderer.renderMarkdownText(
                "* item one",
                agentOptions,
            )

        assertEquals(1, parts.size)
        val line = assertIs<TranscriptBodyPart.ListLine>(parts.single())
        assertEquals("• ", line.marker)
        assertTrue(line.text.contains("item one"))
    }

    @Test
    fun `agent blockquote yields BlockQuote body part`() {
        val parts =
            TranscriptContentRenderer.renderMarkdownText(
                "> quoted text",
                agentOptions,
            )

        assertEquals(1, parts.size)
        val quote = assertIs<TranscriptBodyPart.BlockQuote>(parts.single())
        val inner = assertIs<TranscriptBodyPart.InlineText>(quote.parts.single())
        assertTrue(inner.text.contains("quoted text"))
    }

    @Test
    fun `agent thematic break yields ThematicBreak body part`() {
        val parts =
            TranscriptContentRenderer.renderMarkdownText(
                "before\n\n---\n\nafter",
                agentOptions,
            )

        assertEquals(3, parts.size)
        assertIs<TranscriptBodyPart.InlineText>(parts[0])
        assertIs<TranscriptBodyPart.ThematicBreak>(parts[1])
        assertIs<TranscriptBodyPart.InlineText>(parts[2])
    }

    @Test
    fun `agent image yields Image body part`() {
        val parts =
            TranscriptContentRenderer.renderMarkdownText(
                "![alt](http://example.com/img.png)",
                agentOptions,
            )

        assertEquals(1, parts.size)
        val image = assertIs<TranscriptBodyPart.Image>(parts.single())
        assertEquals("alt", image.altText)
        assertEquals("http://example.com/img.png", image.url)
    }

    @Test
    fun `agent malformed fences normalize to code blocks`() {
        val input =
            """```kotlinfun summarize(numbers: List<Number>) {
 val total = numbers.sumOf { it.toDouble() }
}
```Example:

```kotlinsummarize(listOf(1,2,3))```"""
        val parts =
            TranscriptContentRenderer.renderMarkdownText(
                input,
                agentOptions,
            )

        val codeParts = parts.filterIsInstance<TranscriptBodyPart.Code>()
        assertEquals(2, codeParts.size)
        assertTrue(codeParts[0].code.contains("fun summarize"))
        assertTrue(codeParts[1].code.contains("summarize(listOf"))
    }

    @Test
    fun `agent code blocks beyond highlight cap remain as code or plain pre`() {
        val fences =
            (1..TranscriptContentRenderer.MAX_HIGHLIGHTED_CODE_BLOCKS + 2).joinToString("\n") { index ->
                "```kotlin\nfun block$index()\n```"
            }

        val parts = TranscriptContentRenderer.renderMarkdownText(fences, agentOptions)
        val highlighted = parts.filterIsInstance<TranscriptBodyPart.Code>()
        val plainPre =
            parts
                .filterIsInstance<TranscriptBodyPart.Html>()
                .filter { it.fragment.contains("<pre") }

        assertEquals(TranscriptContentRenderer.MAX_HIGHLIGHTED_CODE_BLOCKS + 2, highlighted.size + plainPre.size)
        assertEquals(TranscriptContentRenderer.MAX_HIGHLIGHTED_CODE_BLOCKS, highlighted.size)
        val truncated = TranscriptTextTruncation.truncate(highlighted.first().code)
        assertTrue(truncated.length <= TranscriptContentRenderer.MAX_TEXT_CHARACTERS)
    }
}
