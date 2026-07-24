package com.oaalto.agent.acp

import com.agentclientprotocol.model.ToolCallStatus
import com.agentclientprotocol.model.ToolKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TranscriptRendererTest {
    @Test
    fun `normalizes transcript line endings and br tags`() {
        val normalized =
            TranscriptRenderer.normalizeTranscriptText(
                "Line one\r\nLine two<br/>Line three<br />Line four",
            )

        assertEquals("Line one\nLine two\nLine three\nLine four", normalized)
    }

    @Test
    fun `escape html prevents injection of angle brackets`() {
        assertEquals(
            "&lt;script&gt;alert(1)&lt;/script&gt;",
            TranscriptRenderHelpers.escapeHtml("<script>alert(1)</script>"),
        )
    }

    @Test
    fun `escape html prevents injection of ampersand`() {
        assertEquals("a &amp; b &amp; c", TranscriptRenderHelpers.escapeHtml("a & b & c"))
    }

    @Test
    fun `escape html prevents injection of double quote`() {
        assertEquals("&quot;hello&quot;", TranscriptRenderHelpers.escapeHtml("\"hello\""))
    }

    @Test
    fun `escape html handles mixed special characters`() {
        assertEquals(
            "&lt;tag attr=&quot;val&quot;&gt;text&amp;more&lt;/tag&gt;",
            TranscriptRenderHelpers.escapeHtml("<tag attr=\"val\">text&more</tag>"),
        )
    }

    @Test
    fun `escape html returns empty string for empty input`() {
        assertEquals("", TranscriptRenderHelpers.escapeHtml(""))
    }

    @Test
    fun `escape html returns plain text unchanged`() {
        assertEquals("Hello, World!", TranscriptRenderHelpers.escapeHtml("Hello, World!"))
    }

    // -- HTML helper methods ---------------------------------------------------

    @Test
    fun `formatErrorHtml returns error-colored span with escaped message`() {
        val html = TranscriptRenderHelpers.formatErrorHtml("something <broken>")

        // Check for color attribute with hex value (dynamic color from provider)
        assertTrue(html.contains(Regex("color:#[0-9a-f]{6}")), "Expected hex color in style attribute")
        assertTrue(html.contains("&lt;broken&gt;"))
        assertTrue(html.contains("Error:"))
    }

    @Test
    fun `formatAuthFailureHtml returns error-colored span`() {
        val html = TranscriptRenderHelpers.formatAuthFailureHtml("token expired")

        // Check for color attribute with hex value (dynamic color from provider)
        assertTrue(html.contains(Regex("color:#[0-9a-f]{6}")), "Expected hex color in style attribute")
        assertTrue(html.contains("token expired"))
        assertTrue(html.contains("Auth failed:"))
    }

    @Test
    fun `htmlDocumentStart returns opening html and body tags`() {
        val start = TranscriptRenderHelpers.htmlDocumentStart()

        assertTrue(start.startsWith("<html>"))
        assertTrue(start.contains("<body"))
    }

    @Test
    fun `htmlDocumentStart declares utf-8 charset for status icons`() {
        val document =
            TranscriptRenderHelpers.htmlDocumentStart() +
                TranscriptRenderHelpers.formatToolStatusHtml("ok", ToolKind.READ, ToolCallStatus.COMPLETED) +
                TranscriptRenderHelpers.HTML_LINE_BREAK +
                TranscriptRenderHelpers.formatToolStatusHtml("fail", ToolKind.EXECUTE, ToolCallStatus.FAILED) +
                TranscriptRenderHelpers.HTML_DOCUMENT_END

        assertTrue(document.contains("charset=UTF-8"))
        assertTrue(document.contains("✓"))
        assertTrue(document.contains("✗"))
    }

    @Test
    fun `htmlDocumentEnd returns closing body and html tags`() {
        assertEquals("</body></html>", TranscriptRenderHelpers.HTML_DOCUMENT_END)
    }

    @Test
    fun `formatToolStatusHtml returns span with badge for pending status`() {
        val html = TranscriptRenderHelpers.formatToolStatusHtml("ls", ToolKind.READ, null)

        assertTrue(html.contains("background-color:#666666"))
        assertTrue(html.contains("read"))
        assertTrue(html.contains("ls"))
        assertLegacyToolStatusFormatAbsent(html)
    }

    @Test
    fun `formatToolStatusHtml returns dim gray badge for explicit pending status`() {
        val html =
            TranscriptRenderHelpers.formatToolStatusHtml("ls", ToolKind.READ, ToolCallStatus.PENDING)

        assertTrue(html.contains("background-color:#666666"))
        assertTrue(html.contains("read"))
        assertTrue(html.contains("ls"))
        assertLegacyToolStatusFormatAbsent(html)
    }

    @Test
    fun `formatToolStatusHtml returns yellow badge for in progress status`() {
        val html =
            TranscriptRenderHelpers.formatToolStatusHtml(
                "read README.md",
                ToolKind.READ,
                ToolCallStatus.IN_PROGRESS,
            )

        assertTrue(html.contains("background-color:#d4a017"))
        assertTrue(html.contains("read"))
        assertTrue(html.contains("read README.md"))
        assertLegacyToolStatusFormatAbsent(html)
    }

    @Test
    fun `formatToolStatusHtml returns span with green badge for completed`() {
        val html =
            TranscriptRenderHelpers.formatToolStatusHtml("edit", ToolKind.EDIT, ToolCallStatus.COMPLETED)

        assertTrue(html.contains("background-color:#2d8a4e"))
        assertTrue(html.contains("✓"))
        assertTrue(html.contains("edit"))
        assertLegacyToolStatusFormatAbsent(html)
    }

    @Test
    fun `formatToolStatusHtml returns red badge with failure icon`() {
        val html =
            TranscriptRenderHelpers.formatToolStatusHtml("run cmd", ToolKind.EXECUTE, ToolCallStatus.FAILED)

        assertTrue(html.contains("background-color:#c43c3c"))
        assertTrue(html.contains("✗"))
        assertTrue(html.contains("execute"))
        assertTrue(html.contains("run cmd"))
        assertLegacyToolStatusFormatAbsent(html)
    }

    @Test
    fun `formatToolStatusHtml uses tool label when kind is null`() {
        val html = TranscriptRenderHelpers.formatToolStatusHtml("do thing", null, ToolCallStatus.PENDING)

        assertTrue(html.contains("tool"))
        assertTrue(html.contains("do thing"))
        assertLegacyToolStatusFormatAbsent(html)
    }

    @Test
    fun `formatToolStatusHtml escaped special characters in title`() {
        val html = TranscriptRenderHelpers.formatToolStatusHtml("<script>", ToolKind.READ, ToolCallStatus.FAILED)

        assertTrue(html.contains("&lt;script&gt;"))
        assertTrue(!html.contains("<script>"))
        assertLegacyToolStatusFormatAbsent(html)
    }

    @Test
    fun `formatToolStatusHtml allows status words in title`() {
        val html =
            TranscriptRenderHelpers.formatToolStatusHtml(
                "fix (completed) bug",
                ToolKind.EDIT,
                ToolCallStatus.COMPLETED,
            )

        assertTrue(html.contains("fix (completed) bug"))
        assertLegacyToolStatusFormatAbsent(html)
    }

    @Test
    fun `formatToolStatusHtml normalizes kind enum to lowercase label`() {
        val html =
            TranscriptRenderHelpers.formatToolStatusHtml(
                "target",
                ToolKind.OTHER,
                ToolCallStatus.IN_PROGRESS,
            )

        assertTrue(html.contains("other"))
        assertLegacyToolStatusFormatAbsent(html)
    }

    @Test
    fun `badgeLabelFor preserves escaped html in kind label`() {
        assertEquals(
            "✓ &lt;script&gt;",
            TranscriptBadgeStyle.label(ToolCallStatus.COMPLETED, "&lt;script&gt;"),
        )
        assertEquals(
            "✗ a &amp; b",
            TranscriptBadgeStyle.label(ToolCallStatus.FAILED, "a &amp; b"),
        )
    }

    @Test
    fun `formatToolStatus uses badge-first plain text without legacy syntax`() {
        assertEquals(
            "✓ edit edit file",
            TranscriptRenderer.formatToolStatus("edit file", ToolKind.EDIT, ToolCallStatus.COMPLETED),
        )
        assertEquals(
            "✗ execute run cmd",
            TranscriptRenderer.formatToolStatus("run cmd", ToolKind.EXECUTE, ToolCallStatus.FAILED),
        )
        assertEquals(
            "read ls",
            TranscriptRenderer.formatToolStatus("ls", ToolKind.READ, ToolCallStatus.IN_PROGRESS),
        )
        assertEquals(
            "tool do thing",
            TranscriptRenderer.formatToolStatus("do thing", null, ToolCallStatus.PENDING),
        )
    }

    private val legacyBracketedBadgePattern = Regex("""\[<span[^>]*>""")
    private val legacyTrailingStatusPattern =
        Regex("""\s\((?:pending|in progress|completed|failed|started)\)</span>\s*$""")

    private fun assertLegacyToolStatusFormatAbsent(html: String) {
        assertTrue(
            !legacyBracketedBadgePattern.containsMatchIn(html),
            "expected no legacy bracket-wrapped badge, got: $html",
        )
        assertTrue(
            !legacyTrailingStatusPattern.containsMatchIn(html),
            "expected no legacy trailing status label, got: $html",
        )
    }
}
