package com.oaalto.agent.acp

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class TranscriptMarkdownRendererTest {
    // Remaining parser-level tests not covered by TranscriptContentRendererTest

    @Test
    fun `likelyContainsMarkdown returns true for common syntax`() {
        assertTrue(TranscriptMarkdownRenderer.likelyContainsMarkdown("**bold**"))
        assertTrue(TranscriptMarkdownRenderer.likelyContainsMarkdown("*italic*"))
        assertTrue(TranscriptMarkdownRenderer.likelyContainsMarkdown("`code`"))
        assertTrue(TranscriptMarkdownRenderer.likelyContainsMarkdown("[link](url)"))
        assertTrue(TranscriptMarkdownRenderer.likelyContainsMarkdown("```fence```"))
        assertTrue(TranscriptMarkdownRenderer.likelyContainsMarkdown("# heading"))
        assertTrue(TranscriptMarkdownRenderer.likelyContainsMarkdown("---"))
    }

    @Test
    fun `ordered list items have numeric markers`() {
        val input = "1. first\n2. second"
        val blocks = TranscriptMarkdownRenderer.parseToBlocks(input)

        assertTrue(blocks.size >= 2)
        val first = assertIs<RenderedBlock.InlineText>(blocks[0])
        assertTrue(first.text.contains("first"))
        val second = assertIs<RenderedBlock.InlineText>(blocks[1])
        assertTrue(second.text.contains("second"))
    }

    @Test
    fun `strikethrough extracts styled run`() {
        val blocks = TranscriptMarkdownRenderer.parseToBlocks("~~struck~~ text")

        assertEquals(1, blocks.size)
        val text = assertIs<RenderedBlock.InlineText>(blocks.single())
        assertEquals("struck text", text.text)
        val run = text.runs.firstOrNull { it.style == TextStyle.STRIKETHROUGH }
        assertTrue(run != null)
        assertEquals(0, requireNotNull(run).start)
        assertEquals(6, run.end)
    }

    @Test
    fun `blockquote with multiple paragraphs`() {
        val input = "> para one\n>\n> para two"
        val blocks = TranscriptMarkdownRenderer.parseToBlocks(input)

        assertEquals(1, blocks.size)
        val quote = assertIs<RenderedBlock.BlockQuote>(blocks.single())
        assertTrue(quote.blocks.size >= 2)
    }

    @Test
    fun `untagged fence has null language`() {
        val blocks = TranscriptMarkdownRenderer.parseToBlocks("```\nplain\n```")

        assertEquals(1, blocks.size)
        val code = assertIs<RenderedBlock.CodeBlock>(blocks.single())
        assertEquals(null, code.languageId)
        assertEquals("plain", code.code)
    }
}
