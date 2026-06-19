@file:Suppress("OPT_IN_USAGE")

package com.oaalto.agent.acp

import com.agentclientprotocol.model.ContentBlock
import com.agentclientprotocol.model.SessionUpdate
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
                ),
                SessionUpdate.AgentMessageChunk(content = ContentBlock.Text("Done")),
            )

        val fragments = updates.flatMap(TranscriptRenderer::renderUpdate)

        assertEquals(4, fragments.size)
        assertTrue(fragments[0].contains("<span"))
        assertTrue(fragments[0].contains("Thinking"))
        assertTrue(fragments[1].contains("<span"))
        assertTrue(fragments[2].contains("<span"))
        assertTrue(fragments[3].contains("<span"))
        assertTrue(fragments[3].contains("Done"))
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
        assertTrue(fragments[0].contains("completed"))
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
    fun `htmlDocumentEnd returns closing body and html tags`() {
        assertEquals("</body></html>", TranscriptRenderHelpers.HTML_DOCUMENT_END)
    }

    @Test
    fun `formatToolStatusHtml returns span with badge for pending status`() {
        val html = TranscriptRenderHelpers.formatToolStatusHtml("ls", ToolKind.READ, null)

        assertTrue(html.contains("background-color:#666666"))
        assertTrue(html.contains("ls"))
        assertTrue(html.contains("started"))
    }

    @Test
    fun `formatToolStatusHtml returns span with green badge for completed`() {
        val html =
            TranscriptRenderHelpers.formatToolStatusHtml("edit", ToolKind.EDIT, ToolCallStatus.COMPLETED)

        assertTrue(html.contains("background-color:#2d8a4e"))
        assertTrue(html.contains("edit"))
    }

    @Test
    fun `formatToolStatusHtml escaped special characters in title`() {
        val html = TranscriptRenderHelpers.formatToolStatusHtml("<script>", ToolKind.READ, ToolCallStatus.FAILED)

        assertTrue(html.contains("&lt;script&gt;"))
        assertTrue(!html.contains("<script>"))
    }
}
