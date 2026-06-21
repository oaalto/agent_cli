@file:Suppress("OPT_IN_USAGE")

package com.oaalto.agent.acp

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class TextSegmentTest {
    @Test
    fun `single kotlin fence yields one code segment`() {
        val segments = segmentFencedCodeBlocks("```kotlin\nfun main()\n```")

        assertEquals(1, segments.size)
        val code = assertIs<TextSegment.Code>(segments.single())
        assertEquals("fun main()", code.text)
        assertEquals("kotlin", code.languageId)
    }

    @Test
    fun `prose fence prose yields three segments`() {
        val input = "intro\n```json\n{\"a\":1}\n```\noutro"
        val segments = segmentFencedCodeBlocks(input)

        assertEquals(3, segments.size)
        val prose = assertIs<TextSegment.Prose>(segments[0])
        assertEquals("intro\n", prose.text)
        val code = assertIs<TextSegment.Code>(segments[1])
        assertEquals("json", code.languageId)
        assertEquals("{\"a\":1}", code.text)
        assertEquals("outro", (segments[2] as TextSegment.Prose).text)
    }

    @Test
    fun `unclosed fence treats tail as prose`() {
        val segments = segmentFencedCodeBlocks("before\n```kotlin\nfun main()")

        assertEquals(2, segments.size)
        val proseBefore = assertIs<TextSegment.Prose>(segments[0])
        assertEquals("before\n", proseBefore.text)
        val proseTail = assertIs<TextSegment.Prose>(segments[1])
        assertEquals("```kotlin\nfun main()", proseTail.text)
    }

    @Test
    fun `closing fence appended yields code segment`() {
        val streaming = "before\n```kotlin\nfun main()"
        val finalized = "$streaming\n```"
        val segments = segmentFencedCodeBlocks(finalized)

        assertEquals(2, segments.size)
        val proseBefore = assertIs<TextSegment.Prose>(segments[0])
        assertEquals("before\n", proseBefore.text)
        val code = assertIs<TextSegment.Code>(segments[1])
        assertEquals("kotlin", code.languageId)
        assertEquals("fun main()", code.text)
    }

    @Test
    fun `untagged fence has null language`() {
        val segments = segmentFencedCodeBlocks("```\nplain\n```")
        val code = assertIs<TextSegment.Code>(segments.single())
        assertEquals(null, code.languageId)
        assertEquals("plain", code.text)
    }

    @Test
    fun `multiple fences are segmented independently`() {
        val input = "```sh\necho one\n```\n```py\nprint(2)\n```"
        val segments = segmentFencedCodeBlocks(input)

        assertEquals(2, segments.size)
        assertEquals("sh", (segments[0] as TextSegment.Code).languageId)
        assertEquals("py", (segments[1] as TextSegment.Code).languageId)
    }

    @Test
    fun `containsCode is false for plain text`() {
        assertTrue(!segmentFencedCodeBlocks("no fences here").containsCode())
    }
}
