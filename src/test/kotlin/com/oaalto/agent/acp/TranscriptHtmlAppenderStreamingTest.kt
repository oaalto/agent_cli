package com.oaalto.agent.acp

import com.agentclientprotocol.model.ContentBlock
import com.agentclientprotocol.model.SessionUpdate
import com.agentclientprotocol.model.ToolCallId
import com.agentclientprotocol.model.ToolCallStatus
import com.agentclientprotocol.model.ToolKind
import javax.swing.JEditorPane
import javax.swing.JScrollPane
import javax.swing.SwingUtilities
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TranscriptHtmlAppenderStreamingTest {
    @Test
    fun `streaming chunks show cursor until finalize`() {
        val (appender, pane) = appender()

        appender.startOrContinueAgentStream("Hel")
        appender.startOrContinueAgentStream("lo")

        assertTrue(TranscriptStreamingCursor.hasCursor(appender.displayBodyHtmlForTest()))
        assertTrue(appender.displayBodyHtmlForTest().contains("Hello"))

        appender.finalizeAgentStream()

        assertFalse(TranscriptStreamingCursor.hasCursor(appender.displayBodyHtmlForTest()))
        assertTrue(appender.displayBodyHtmlForTest().contains("Hello"))
        assertFalse(appender.isAgentStreamActive())
        assertFalse(TranscriptStreamingCursor.hasCursor(paneBodyHtml(pane)))
    }

    @Test
    fun `cursor survives JEditorPane html round trip`() {
        val (appender, pane) = appender()

        appender.startOrContinueAgentStream("Hi")
        assertTrue(TranscriptStreamingCursor.hasCursor(paneBodyHtml(pane)))

        appender.finalizeAgentStream()
        assertFalse(TranscriptStreamingCursor.hasCursor(paneBodyHtml(pane)))
        assertTrue(paneBodyHtml(pane).contains("Hi"))
    }

    @Test
    fun `empty and whitespace chunks do not start stream`() {
        val appender = appender().first

        appender.startOrContinueAgentStream("")
        appender.startOrContinueAgentStream("   ")

        assertFalse(appender.isAgentStreamActive())
        assertFalse(TranscriptStreamingCursor.hasCursor(appender.displayBodyHtmlForTest()))
    }

    @Test
    fun `second stream cycle after finalize gets a new cursor`() {
        val appender = appender().first

        appender.startOrContinueAgentStream("first")
        appender.finalizeAgentStream()
        appender.startOrContinueAgentStream("second")

        assertTrue(TranscriptStreamingCursor.hasCursor(appender.displayBodyHtmlForTest()))
        assertTrue(appender.displayBodyHtmlForTest().contains("first"))
        assertTrue(appender.displayBodyHtmlForTest().contains("second"))
    }

    @Test
    fun `html special characters are escaped in streamed chunks`() {
        val appender = appender().first

        appender.startOrContinueAgentStream("<tag> & more")

        assertTrue(appender.displayBodyHtmlForTest().contains("&lt;tag&gt; &amp; more"))
        assertFalse(appender.displayBodyHtmlForTest().contains("<tag> & more"))
    }

    @Test
    fun `double finalize is safe`() {
        val appender = appender().first

        appender.startOrContinueAgentStream("text")
        appender.finalizeAgentStream()
        val afterFirst = appender.displayBodyHtmlForTest()
        appender.finalizeAgentStream()

        assertEquals(afterFirst, appender.displayBodyHtmlForTest())
    }

    @Test
    fun `append line finalizes active stream before new entry`() {
        val appender = appender().first

        appender.startOrContinueAgentStream("partial")
        appender.appendLine("status")

        assertFalse(appender.isAgentStreamActive())
        assertFalse(TranscriptStreamingCursor.hasCursor(appender.displayBodyHtmlForTest()))
        assertTrue(appender.displayBodyHtmlForTest().contains("partial"))
        assertTrue(appender.displayBodyHtmlForTest().contains("status"))
    }

    @Test
    fun `streaming does not scroll when viewport is not at bottom`() {
        val pane = JEditorPane("text/html", "")
        val scrollPane = JScrollPane(pane)
        scrollPane.setSize(300, 100)
        pane.setSize(300, 800)
        val appender = appender(pane)

        repeat(30) { appender.appendLine("line $it") }

        runOnTestEdt {
            scrollPane.verticalScrollBar.value = 0
        }
        val scrollBefore = scrollPane.verticalScrollBar.value

        appender.startOrContinueAgentStream("chunk")

        assertEquals(scrollBefore, scrollPane.verticalScrollBar.value)
    }

    @Test
    fun `streaming updates caret when no scroll ancestor`() {
        val pane = JEditorPane("text/html", "")
        val appender = appender(pane)

        appender.startOrContinueAgentStream("chunk")

        assertEquals(pane.document.length, pane.caretPosition)
    }

    private fun appender(): Pair<TranscriptHtmlAppender, JEditorPane> {
        val pane = JEditorPane("text/html", "")
        return appender(pane) to pane
    }

    private fun appender(pane: JEditorPane): TranscriptHtmlAppender = TranscriptHtmlAppender(pane, ::runOnTestEdt)

    private fun paneBodyHtml(pane: JEditorPane): String {
        val text = pane.text
        val bodyStart = text.indexOf("<body>")
        val bodyEnd = text.lastIndexOf("</body>")
        if (bodyStart < 0 || bodyEnd < 0) return text
        return text.substring(bodyStart + "<body>".length, bodyEnd)
    }

    private fun runOnTestEdt(action: () -> Unit) {
        if (SwingUtilities.isEventDispatchThread()) {
            action()
        } else {
            SwingUtilities.invokeAndWait(action)
        }
    }
}

class AcpPromptEventDispatcherTest {
    @Test
    fun `agent chunks append without finalize`() {
        val listener = RecordingListener()

        AcpPromptEventDispatcher.dispatchSessionUpdate(
            SessionUpdate.AgentMessageChunk(content = ContentBlock.Text("Hi")),
            listener,
        )

        assertEquals(listOf("Hi"), listener.appendedChunks)
        assertEquals(0, listener.finalizeCount)
    }

    @Test
    fun `tool call finalizes before structured tool update`() {
        val listener = RecordingListener()

        AcpPromptEventDispatcher.dispatchSessionUpdate(
            SessionUpdate.ToolCall(
                toolCallId = ToolCallId("1"),
                title = "read file",
                kind = ToolKind.READ,
                status = ToolCallStatus.IN_PROGRESS,
            ),
            listener,
        )

        assertEquals(1, listener.finalizeCount)
        assertTrue(listener.toolUpdates.any { it.title == "read file" })
    }

    @Test
    fun `prompt completion finalizes stream`() {
        val listener = RecordingListener()

        AcpPromptEventDispatcher.dispatchPromptCompleted(listener)

        assertEquals(1, listener.finalizeCount)
    }

    @Test
    fun `chunk then tool sequence produces append finalize and tool update`() {
        val listener = RecordingListener()

        AcpPromptEventDispatcher.dispatchSessionUpdate(
            SessionUpdate.AgentMessageChunk(content = ContentBlock.Text("Thinking")),
            listener,
        )
        AcpPromptEventDispatcher.dispatchSessionUpdate(
            SessionUpdate.ToolCall(
                toolCallId = ToolCallId("1"),
                title = "read README.md",
                kind = ToolKind.READ,
                status = ToolCallStatus.IN_PROGRESS,
            ),
            listener,
        )
        AcpPromptEventDispatcher.dispatchPromptCompleted(listener)

        assertEquals(listOf("Thinking"), listener.appendedChunks)
        assertEquals(2, listener.finalizeCount)
        assertTrue(listener.toolUpdates.any { it.title == "read README.md" })
    }

    private class RecordingListener : AcpSessionListener {
        val updates = mutableListOf<StructuredUpdate>()

        val appendedChunks: List<String>
            get() = updates.filterIsInstance<StructuredUpdate.AppendAgentText>().map { it.text }

        val finalizeCount: Int
            get() = updates.count { it is StructuredUpdate.FinalizeAgentStream }

        val toolUpdates: List<StructuredUpdate.StartOrUpdateToolCall>
            get() = updates.filterIsInstance<StructuredUpdate.StartOrUpdateToolCall>()

        override fun onStructuredUpdate(update: StructuredUpdate) {
            updates += update
        }

        override fun onError(message: String) = Unit

        override fun onUsageUpdate(usage: AccumulatedUsage) = Unit
    }
}
