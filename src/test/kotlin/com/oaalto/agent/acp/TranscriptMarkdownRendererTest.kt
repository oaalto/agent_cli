package com.oaalto.agent.acp

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class TranscriptMarkdownRendererTest {
    // -----------------------------------------------------------------------
    // Basic inline formatting
    // -----------------------------------------------------------------------

    @Test
    fun `plain text yields one InlineText with no styled runs`() {
        val blocks = TranscriptMarkdownRenderer.parseToBlocks("Hello, world!")

        assertEquals(1, blocks.size)
        val text = assertIs<RenderedBlock.InlineText>(blocks.single())
        assertEquals("Hello, world!", text.text)
        assertTrue(text.runs.isEmpty())
    }

    @Test
    fun `bold text extracts styled run`() {
        val blocks = TranscriptMarkdownRenderer.parseToBlocks("**bold** text")

        assertEquals(1, blocks.size)
        val text = assertIs<RenderedBlock.InlineText>(blocks.single())
        assertEquals("bold text", text.text)
        assertEquals(1, text.runs.size)
        val run = text.runs.single()
        assertEquals(TextStyle.BOLD, run.style)
        assertEquals(0, run.start)
        assertEquals(4, run.end)
    }

    @Test
    fun `italic text extracts styled run`() {
        val blocks = TranscriptMarkdownRenderer.parseToBlocks("*italic* text")

        assertEquals(1, blocks.size)
        val text = assertIs<RenderedBlock.InlineText>(blocks.single())
        assertEquals("italic text", text.text)
        val run = text.runs.single()
        assertEquals(TextStyle.ITALIC, run.style)
        assertEquals(0, run.start)
        assertEquals(6, run.end)
    }

    @Test
    fun `inline code extracts styled run`() {
        val blocks = TranscriptMarkdownRenderer.parseToBlocks("Use `code` here")

        assertEquals(1, blocks.size)
        val text = assertIs<RenderedBlock.InlineText>(blocks.single())
        assertEquals("Use code here", text.text)
        val run = text.runs.single()
        assertEquals(TextStyle.CODE, run.style)
        assertEquals(4, run.start)
        assertEquals(8, run.end)
    }

    @Test
    fun `bold italic combined`() {
        val blocks = TranscriptMarkdownRenderer.parseToBlocks("***bold italic***")

        assertEquals(1, blocks.size)
        val text = assertIs<RenderedBlock.InlineText>(blocks.single())
        assertEquals("bold italic", text.text)
        assertEquals(1, text.runs.size)
        assertEquals(TextStyle.BOLD_ITALIC, text.runs.single().style)
    }

    @Test
    fun `link extracts url`() {
        val blocks = TranscriptMarkdownRenderer.parseToBlocks("[text](http://example.com)")

        assertEquals(1, blocks.size)
        val text = assertIs<RenderedBlock.InlineText>(blocks.single())
        assertEquals("text", text.text)
        assertEquals(1, text.runs.size)
        val run = text.runs.single()
        assertEquals(TextStyle.LINK, run.style)
        assertEquals("http://example.com", run.url)
    }

    @Test
    fun `multiple inline styles in one paragraph`() {
        val blocks =
            TranscriptMarkdownRenderer.parseToBlocks("**bold** and *italic* and `code`")

        assertEquals(1, blocks.size)
        val text = assertIs<RenderedBlock.InlineText>(blocks.single())
        assertEquals("bold and italic and code", text.text)
        assertTrue(text.runs.size >= 2)
        val boldRun = text.runs.first()
        assertEquals(TextStyle.BOLD, boldRun.style)
    }

    // -----------------------------------------------------------------------
    // Block-level: headings
    // -----------------------------------------------------------------------

    @Test
    fun `heading levels`() {
        val input = "# H1\n\n## H2\n\n### H3"
        val blocks = TranscriptMarkdownRenderer.parseToBlocks(input)

        assertEquals(3, blocks.size)
        val h1 = assertIs<RenderedBlock.InlineText>(blocks[0])
        assertEquals("H1", h1.text.trim())
        assertTrue(h1.headingLevel >= 1)
        val h2 = assertIs<RenderedBlock.InlineText>(blocks[1])
        assertEquals("H2", h2.text.trim())
        val h3 = assertIs<RenderedBlock.InlineText>(blocks[2])
        assertEquals("H3", h3.text.trim())
    }

    // -----------------------------------------------------------------------
    // Fenced code blocks
    // -----------------------------------------------------------------------

    @Test
    fun `single kotlin fence yields one code block`() {
        val blocks = TranscriptMarkdownRenderer.parseToBlocks("```kotlin\nfun main()\n```")

        assertEquals(1, blocks.size)
        val code = assertIs<RenderedBlock.CodeBlock>(blocks.single())
        assertEquals("fun main()", code.code)
        assertEquals("kotlin", code.languageId)
    }

    @Test
    fun `untagged fence has null language`() {
        val blocks = TranscriptMarkdownRenderer.parseToBlocks("```\nplain\n```")

        assertEquals(1, blocks.size)
        val code = assertIs<RenderedBlock.CodeBlock>(blocks.single())
        assertEquals(null, code.languageId)
        assertEquals("plain", code.code)
    }

    @Test
    fun `prose fence prose yields three blocks`() {
        val input = "intro\n\n```json\n{\"a\":1}\n```\n\noutro"
        val blocks = TranscriptMarkdownRenderer.parseToBlocks(input)

        assertTrue(blocks.size >= 3)
        val proseBefore = assertIs<RenderedBlock.InlineText>(blocks[0])
        assertTrue(proseBefore.text.contains("intro"))
        val code = assertIs<RenderedBlock.CodeBlock>(blocks[1])
        assertEquals("json", code.languageId)
        assertEquals("{\"a\":1}", code.code)
        val proseAfter = assertIs<RenderedBlock.InlineText>(blocks[2])
        assertTrue(proseAfter.text.contains("outro"))
    }

    @Test
    fun `multiple fences are parsed independently`() {
        val input = "```sh\necho one\n```\n\n```py\nprint(2)\n```"
        val blocks = TranscriptMarkdownRenderer.parseToBlocks(input)

        assertEquals(2, blocks.size)
        val first = assertIs<RenderedBlock.CodeBlock>(blocks[0])
        assertEquals("sh", first.languageId)
        val second = assertIs<RenderedBlock.CodeBlock>(blocks[1])
        assertEquals("py", second.languageId)
    }

    @Test
    fun `concat transcript with inline close and unclosed second fence renders code blocks`() {
        val dollar = "$"
        val input =
            """```kotlinfun concat(a: String, b: String): String = a + b```Or with string templates:

```kotlinfun concat(a: String, b: String): String = ${dollar}a${dollar}b
"""
        val blocks = TranscriptMarkdownRenderer.parseToBlocks(input)

        assertTrue(blocks.any { it is RenderedBlock.CodeBlock }, "expected at least one code block: $blocks")
        val codeBlocks = blocks.filterIsInstance<RenderedBlock.CodeBlock>()
        assertEquals(2, codeBlocks.size, blocks.toString())
        assertTrue(codeBlocks[0].code.isNotBlank(), "first code block empty: '${codeBlocks[0].code}'")
        assertTrue(codeBlocks[1].code.isNotBlank(), "second code block empty: '${codeBlocks[1].code}'")
        assertTrue(codeBlocks[0].code.contains("fun concat"))
        assertTrue(codeBlocks[1].code.contains("fun concat"))
    }

    @Test
    fun `kotlin sample transcript renders code blocks`() {
        val dollar = "$"
        val input =
            """Here's a small Kotlin sample:

```kotlinfun main() {
 val name = "Kotlin"
 val numbers = listOf(1,2,3,4,5)

 val doubled = numbers.map { it *2 }
 println("Hello, ${dollar}name!") println("Doubled: ${dollar}doubled") greet("Ada")}

fun greet(person: String) {
 println("Nice to meet you, ${dollar}person.")}
```Want something more specific (coroutines, Android, data classes, etc.)?"""
        val blocks = TranscriptMarkdownRenderer.parseToBlocks(input)

        val codeBlocks = blocks.filterIsInstance<RenderedBlock.CodeBlock>()
        assertEquals(1, codeBlocks.size, blocks.toString())
        assertTrue(codeBlocks[0].code.contains("fun main"))
        assertTrue(codeBlocks[0].code.contains("fun greet"))
        assertTrue(codeBlocks[0].code.contains('\n'), "code should preserve line breaks: ${codeBlocks[0].code}")
        val trailing = blocks.filterIsInstance<RenderedBlock.InlineText>().last()
        assertTrue(trailing.text.contains("Want something more specific"))
    }

    @Test
    fun `main kt inline fence transcript renders two code blocks`() {
        val dollar = "$"
        val input =
            """Here's a concise Kotlin example in the same style as your `Main.kt`:```kotlinfun sum(numbers: List<Int>): Int = numbers.sum()fun main() {
 val numbers = listOf(1,2,3,4,5)
 println(sum(numbers)) //15}
```A slightly richer version with data classes and null safety:

```kotlindata class User(val name: String, val age: Int?)fun greet(user: User): String {
 val ageText = user.age?.let { "${dollar}it years old" } ?: "age unknown"
 return "Hello, $dollar{user.name} (${dollar}ageText)"}

fun main() {
 val users = listOf(
 User("Ada",36), User("Grace", null)
 )

 users .map(::greet)
 .forEach(::println)
}
```Your project already has the first style in `src/main/kotlin/Main.kt`."""
        val blocks = TranscriptMarkdownRenderer.parseToBlocks(input)

        val codeBlocks = blocks.filterIsInstance<RenderedBlock.CodeBlock>()
        assertEquals(2, codeBlocks.size, blocks.toString())
        assertEquals("kotlin", codeBlocks[0].languageId)
        assertTrue(codeBlocks[0].code.contains("fun sum"))
        assertTrue(codeBlocks[0].code.contains('\n'), "first code block should preserve line breaks")
        assertEquals("kotlin", codeBlocks[1].languageId)
        assertTrue(codeBlocks[1].code.contains("data class User"))
        assertTrue(codeBlocks[1].code.contains('\n'), "second code block should preserve line breaks")
        val intro = assertIs<RenderedBlock.InlineText>(blocks.first())
        assertTrue(intro.text.contains("Main.kt"))
        assertTrue(!intro.text.contains("kotlinfun"), "prose should not contain merged fence text")
        val middle = blocks.filterIsInstance<RenderedBlock.InlineText>()[1]
        assertTrue(middle.text.contains("slightly richer"))
    }

    @Test
    fun `malformed agent fences with merged language tags render as code blocks`() {
        val input =
            """```kotlinfun summarize(numbers: List<Number>) {
 val total = numbers.sumOf { it.toDouble() }
 println(total)
}
```Example:

```kotlinsummarize(listOf(1,2,3,4.5)) // prints10.5```"""
        val blocks = TranscriptMarkdownRenderer.parseToBlocks(input)

        assertEquals(3, blocks.size)
        val first = assertIs<RenderedBlock.CodeBlock>(blocks[0])
        assertEquals("kotlin", first.languageId)
        assertTrue(first.code.contains("fun summarize"))
        val example = assertIs<RenderedBlock.InlineText>(blocks[1])
        assertEquals("Example:", example.text.trim())
        val second = assertIs<RenderedBlock.CodeBlock>(blocks[2])
        assertEquals("kotlin", second.languageId)
        assertTrue(second.code.contains("summarize(listOf"))
    }

    @Test
    fun `person transcript renders two kotlin code blocks`() {
        val dollar = "$"
        val input =
            """Added `Person` in `src/main/kotlin/Person.kt`:```kotlinclass Person(
 val name: String,
 val age: Int,
) {
 fun greet(): String = "Hello, my name is ${dollar}name and I am ${dollar}age years old." fun isAdult(): Boolean = age >=18}
```Usage:

```kotlinval person = Person("Ada",36)
println(person.greet())println(person.isAdult())```"""
        val blocks = TranscriptMarkdownRenderer.parseToBlocks(input)
        val codeBlocks = blocks.filterIsInstance<RenderedBlock.CodeBlock>()

        assertEquals(2, codeBlocks.size, blocks.toString())
        assertTrue(codeBlocks[0].code.contains("class Person"))
        assertTrue(codeBlocks[1].code.contains("val person"))
        val usage = blocks.filterIsInstance<RenderedBlock.InlineText>().find { it.text == "Usage:" }
        assertTrue(usage != null, "expected Usage inline text in $blocks")
    }

    @Test
    fun `citation fence with merged path renders kotlin code block`() {
        val dollar = "$"
        val input =
            """Here it is:

```3:10:src/main/kotlin/Person.ktclass Person(
 val name: String,
 val age: Int,
) {
 fun greet(): String = "Hello, my name is ${dollar}name and I am ${dollar}age years old." fun isAdult(): Boolean = age >=18}
```"""
        val blocks = TranscriptMarkdownRenderer.parseToBlocks(input)
        val codeBlocks = blocks.filterIsInstance<RenderedBlock.CodeBlock>()

        assertEquals(1, codeBlocks.size, blocks.toString())
        assertEquals("kotlin", codeBlocks[0].languageId)
        assertTrue(codeBlocks[0].code.contains("class Person"))
        assertTrue(codeBlocks[0].code.contains("fun greet()"))
    }

    // -----------------------------------------------------------------------
    // Lists
    // -----------------------------------------------------------------------

    @Test
    fun `unordered list items have markers`() {
        val input = "* item 1\n* item 2"
        val blocks = TranscriptMarkdownRenderer.parseToBlocks(input)

        assertTrue(blocks.size >= 2)
        val first = assertIs<RenderedBlock.InlineText>(blocks[0])
        assertEquals("• ", first.listMarker)
        assertTrue(first.text.contains("item 1"))
        val second = assertIs<RenderedBlock.InlineText>(blocks[1])
        assertEquals("• ", second.listMarker)
        assertTrue(second.text.contains("item 2"))
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

    // -----------------------------------------------------------------------
    // Tables
    // -----------------------------------------------------------------------

    @Test
    fun `gfm table extracts headers and rows`() {
        val input = "| H1 | H2 |\n| --- | --- |\n| A1 | A2 |\n| B1 | B2 |"
        val blocks = TranscriptMarkdownRenderer.parseToBlocks(input)

        assertEquals(1, blocks.size)
        val table = assertIs<RenderedBlock.Table>(blocks.single())
        assertEquals(listOf("H1", "H2"), table.headers)
        assertEquals(2, table.rows.size)
        assertEquals(listOf("A1", "A2"), table.rows[0])
        assertEquals(listOf("B1", "B2"), table.rows[1])
    }

    // -----------------------------------------------------------------------
    // Blockquotes
    // -----------------------------------------------------------------------

    @Test
    fun `blockquote wraps inner blocks`() {
        val input = "> quoted text"
        val blocks = TranscriptMarkdownRenderer.parseToBlocks(input)

        assertEquals(1, blocks.size)
        val quote = assertIs<RenderedBlock.BlockQuote>(blocks.single())
        assertEquals(1, quote.blocks.size)
        val inner = assertIs<RenderedBlock.InlineText>(quote.blocks.single())
        assertTrue(inner.text.contains("quoted text"))
    }

    @Test
    fun `blockquote with multiple paragraphs`() {
        val input = "> para one\n>\n> para two"
        val blocks = TranscriptMarkdownRenderer.parseToBlocks(input)

        assertEquals(1, blocks.size)
        val quote = assertIs<RenderedBlock.BlockQuote>(blocks.single())
        assertTrue(quote.blocks.size >= 2)
    }

    // -----------------------------------------------------------------------
    // Thematic break
    // -----------------------------------------------------------------------

    @Test
    fun `thematic break produces ThematicBreak block`() {
        val input = "before\n\n---\n\nafter"
        val blocks = TranscriptMarkdownRenderer.parseToBlocks(input)

        assertEquals(3, blocks.size)
        assertIs<RenderedBlock.InlineText>(blocks[0])
        assertIs<RenderedBlock.ThematicBreak>(blocks[1])
        assertIs<RenderedBlock.InlineText>(blocks[2])
    }

    // -----------------------------------------------------------------------
    // Images
    // -----------------------------------------------------------------------

    @Test
    fun `image produces Image block`() {
        val input = "![alt text](http://example.com/img.png)"
        val blocks = TranscriptMarkdownRenderer.parseToBlocks(input)

        assertEquals(1, blocks.size)
        val img = assertIs<RenderedBlock.Image>(blocks.single())
        assertEquals("alt text", img.altText)
        assertEquals("http://example.com/img.png", img.url)
    }

    // -----------------------------------------------------------------------
    // Malformed / edge cases
    // -----------------------------------------------------------------------

    @Test
    fun `empty input returns empty list`() {
        val blocks = TranscriptMarkdownRenderer.parseToBlocks("")
        assertTrue(blocks.isEmpty())
    }

    @Test
    fun `blank input returns empty list`() {
        val blocks = TranscriptMarkdownRenderer.parseToBlocks("   ")
        assertTrue(blocks.isEmpty())
    }

    @Test
    fun `malformed markdown degrades gracefully`() {
        // Unmatched opening markers should not crash
        val blocks = TranscriptMarkdownRenderer.parseToBlocks("**unclosed")
        assertTrue(blocks.isNotEmpty())
    }

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
    fun `likelyContainsMarkdown returns false for plain text`() {
        assertTrue(!TranscriptMarkdownRenderer.likelyContainsMarkdown("hello world"))
        assertTrue(!TranscriptMarkdownRenderer.likelyContainsMarkdown("just some plain text"))
    }

    // -----------------------------------------------------------------------
    // Strikethrough (GFM)
    // -----------------------------------------------------------------------

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

    // -----------------------------------------------------------------------
    // Inline code inside list items
    // -----------------------------------------------------------------------

    @Test
    fun `inline code inside list item`() {
        val input = "- item with `code`"
        val blocks = TranscriptMarkdownRenderer.parseToBlocks(input)

        assertTrue(blocks.isNotEmpty())
        val item = assertIs<RenderedBlock.InlineText>(blocks.first())
        assertTrue(item.text.contains("code"))
        val codeRun = item.runs.firstOrNull { it.style == TextStyle.CODE }
        assertTrue(codeRun != null)
    }

    // -----------------------------------------------------------------------
    // Mixed block types
    // -----------------------------------------------------------------------

    @Test
    fun `heading then paragraph then code fence`() {
        val input = "# Title\n\nSome text\n\n```sh\necho hi\n```"
        val blocks = TranscriptMarkdownRenderer.parseToBlocks(input)

        assertEquals(3, blocks.size)
        assertIs<RenderedBlock.InlineText>(blocks[0]) // heading
        assertIs<RenderedBlock.InlineText>(blocks[1]) // paragraph
        assertIs<RenderedBlock.CodeBlock>(blocks[2]) // code
    }

    @Test
    fun `closing fence on same line as code does not include trailing prose`() {
        val input =
            """```python
if n <= 1: return n
return fibonacci(n-1) + fibonacci(n-2)```Example: `fibonacci(10)` → `55`."""
        val blocks = TranscriptMarkdownRenderer.parseToBlocks(input)

        val code = blocks.filterIsInstance<RenderedBlock.CodeBlock>().singleOrNull()
        assertTrue(code != null, "blocks=$blocks")
        assertTrue(!code.code.contains("Example"), "code leaked prose: ${code.code}")
        assertTrue(code.code.contains("fibonacci"))
        val prose = blocks.filterIsInstance<RenderedBlock.InlineText>().singleOrNull()
        assertTrue(prose != null, "blocks=$blocks")
        assertTrue(prose.text.contains("Example"))
    }

    @Test
    fun `closing fence without blank line before trailing prose`() {
        val input =
            """```python
if n <= 1: return n
return fibonacci(n-1) + fibonacci(n-2)
```Example: `fibonacci(10)` → `55`."""
        val blocks = TranscriptMarkdownRenderer.parseToBlocks(input)

        val code = blocks.filterIsInstance<RenderedBlock.CodeBlock>().single()
        assertTrue(!code.code.contains("Example"), "code leaked prose: ${code.code}")
        val prose = blocks.filterIsInstance<RenderedBlock.InlineText>().single()
        assertTrue(prose.text.contains("Example"))
    }
}
