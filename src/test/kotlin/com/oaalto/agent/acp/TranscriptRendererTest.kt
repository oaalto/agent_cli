@file:Suppress("OPT_IN_USAGE")

package com.oaalto.agent.acp

import com.agentclientprotocol.model.ContentBlock
import com.agentclientprotocol.model.SessionUpdate
import com.agentclientprotocol.model.ToolCallContent
import com.agentclientprotocol.model.ToolCallId
import com.agentclientprotocol.model.ToolCallStatus
import com.agentclientprotocol.model.ToolKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TranscriptRendererTest {
    @Test
    fun `renders agent text chunks as incremental transcript text`() {
        val text =
            TranscriptRenderer.renderEventText(
                SessionUpdate.AgentMessageChunk(content = ContentBlock.Text("Hello")),
            )

        assertEquals("Hello", text)
    }

    @Test
    fun `normalizes transcript line endings and br tags`() {
        val normalized =
            TranscriptRenderer.normalizeTranscriptText(
                "Line one\r\nLine two<br/>Line three<br />Line four",
            )

        assertEquals("Line one\nLine two\nLine three\nLine four", normalized)
    }

    @Test
    fun `renders tool call status lines in chronological update order`() {
        val updates =
            listOf(
                SessionUpdate.AgentMessageChunk(content = ContentBlock.Text("Thinking")),
                SessionUpdate.ToolCall(
                    toolCallId = ToolCallId("1"),
                    title = "read README.md",
                    kind = ToolKind.READ,
                    status = ToolCallStatus.IN_PROGRESS,
                ),
                SessionUpdate.ToolCallUpdate(
                    toolCallId = ToolCallId("1"),
                    title = "read README.md",
                    kind = ToolKind.READ,
                    status = ToolCallStatus.COMPLETED,
                    content = listOf(ToolCallContent.Content(ContentBlock.Text("# README\ncontent"))),
                ),
                SessionUpdate.AgentMessageChunk(content = ContentBlock.Text("Done")),
            )

        val fragments = updates.flatMap(TranscriptRenderer::renderUpdate)

        assertEquals(5, fragments.size)
        assertTrue(fragments[0].contains("<span"))
        assertTrue(fragments[0].contains("Thinking"))
        assertTrue(fragments[1].contains("<span"))
        assertTrue(fragments[1].contains("background-color:#d4a017"))
        assertLegacyToolStatusFormatAbsent(fragments[1])
        assertTrue(fragments[2].contains("<span"))
        assertTrue(fragments[2].contains("background-color:#2d8a4e"))
        assertLegacyToolStatusFormatAbsent(fragments[2])
        assertTrue(fragments[3].contains("<pre"))
        assertTrue(fragments[3].contains("# README"))
        assertTrue(fragments[4].contains("<span"))
        assertTrue(fragments[4].contains("Done"))
    }

    // -- HTML output tests -----------------------------------------------------

    @Test
    fun `renderUpdate returns html fragments for agent message chunk`() {
        val fragments =
            TranscriptRenderer.renderUpdate(
                SessionUpdate.AgentMessageChunk(content = ContentBlock.Text("Hello world")),
            )

        assertEquals(1, fragments.size)
        assertTrue(fragments[0].contains("<span"))
        assertTrue(fragments[0].contains("Hello world"))
        assertTrue(fragments[0].contains("color:#d4d4d4"))
    }

    @Test
    fun `renderUpdate returns html fragments for agent thought chunk`() {
        val fragments =
            TranscriptRenderer.renderUpdate(
                SessionUpdate.AgentThoughtChunk(content = ContentBlock.Text("thinking...")),
            )

        assertEquals(1, fragments.size)
        assertTrue(fragments[0].contains("<span"))
        assertTrue(fragments[0].contains("[thought]"))
        assertTrue(fragments[0].contains("thinking..."))
        assertTrue(fragments[0].contains("color:#808080"))
    }

    @Test
    fun `renderUpdate returns html fragments for user message chunk`() {
        val fragments =
            TranscriptRenderer.renderUpdate(
                SessionUpdate.UserMessageChunk(content = ContentBlock.Text("my prompt")),
            )

        assertEquals(1, fragments.size)
        assertTrue(fragments[0].contains("<span"))
        assertTrue(fragments[0].contains("&gt;"))
        assertTrue(fragments[0].contains("my prompt"))
        assertTrue(fragments[0].contains("color:#569cd6"))
    }

    @Test
    fun `renderUpdate returns html tool call badge with status color`() {
        val fragments =
            TranscriptRenderer.renderUpdate(
                SessionUpdate.ToolCall(
                    toolCallId = ToolCallId("1"),
                    title = "edit file",
                    kind = ToolKind.EDIT,
                    status = ToolCallStatus.IN_PROGRESS,
                ),
            )

        assertEquals(1, fragments.size)
        assertTrue(fragments[0].contains("color:#cccccc"))
        assertTrue(fragments[0].contains("background-color:#d4a017"))
        assertTrue(fragments[0].contains("edit"))
        assertLegacyToolStatusFormatAbsent(fragments[0])
    }

    @Test
    fun `renderUpdate returns html tool call update with completed badge`() {
        val fragments =
            TranscriptRenderer.renderUpdate(
                SessionUpdate.ToolCallUpdate(
                    toolCallId = ToolCallId("1"),
                    title = "edit file",
                    kind = ToolKind.EDIT,
                    status = ToolCallStatus.COMPLETED,
                ),
            )

        assertEquals(1, fragments.size)
        assertTrue(fragments[0].contains("background-color:#2d8a4e"))
        assertTrue(fragments[0].contains("✓"))
        assertTrue(fragments[0].contains("edit"))
        assertTrue(fragments[0].contains("edit file"))
        assertLegacyToolStatusFormatAbsent(fragments[0])
    }

    @Test
    fun `renderUpdate uses tool call id when tool call update title is null`() {
        val fragments =
            TranscriptRenderer.renderUpdate(
                SessionUpdate.ToolCallUpdate(
                    toolCallId = ToolCallId("tool-42"),
                    title = null,
                    kind = ToolKind.READ,
                    status = ToolCallStatus.COMPLETED,
                ),
            )

        assertEquals(1, fragments.size)
        assertTrue(fragments[0].contains("background-color:#2d8a4e"))
        assertTrue(fragments[0].contains("tool-42"))
        assertTrue(fragments[0].contains("✓"))
        assertLegacyToolStatusFormatAbsent(fragments[0])
    }

    // -- HTML escaping tests ---------------------------------------------------

    @Test
    fun `escape html prevents injection of angle brackets`() {
        assertEquals(
            "&lt;script&gt;alert(1)&lt;/script&gt;",
            TranscriptUpdateRenderer.escapeHtml("<script>alert(1)</script>"),
        )
    }

    @Test
    fun `escape html prevents injection of ampersand`() {
        assertEquals("a &amp; b &amp; c", TranscriptUpdateRenderer.escapeHtml("a & b & c"))
    }

    @Test
    fun `escape html prevents injection of double quote`() {
        assertEquals("&quot;hello&quot;", TranscriptUpdateRenderer.escapeHtml("\"hello\""))
    }

    @Test
    fun `escape html handles mixed special characters`() {
        assertEquals(
            "&lt;tag attr=&quot;val&quot;&gt;text&amp;more&lt;/tag&gt;",
            TranscriptUpdateRenderer.escapeHtml("<tag attr=\"val\">text&more</tag>"),
        )
    }

    @Test
    fun `escape html returns empty string for empty input`() {
        assertEquals("", TranscriptUpdateRenderer.escapeHtml(""))
    }

    @Test
    fun `escape html returns plain text unchanged`() {
        assertEquals("Hello, World!", TranscriptUpdateRenderer.escapeHtml("Hello, World!"))
    }

    // -- HTML helper methods ---------------------------------------------------

    @Test
    fun `formatErrorHtml returns red span with escaped message`() {
        val html = TranscriptRenderHelpers.formatErrorHtml("something <broken>")

        assertTrue(html.contains("color:#f44747"))
        assertTrue(html.contains("&lt;broken&gt;"))
    }

    @Test
    fun `formatAuthFailureHtml returns red span`() {
        val html = TranscriptRenderHelpers.formatAuthFailureHtml("token expired")

        assertTrue(html.contains("color:#f44747"))
        assertTrue(html.contains("token expired"))
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
