package com.oaalto.agent.acp

import javax.swing.JEditorPane
import javax.swing.SwingUtilities

/**
 * Manages appending content into a [JEditorPane] in `text/html` mode.
 *
 * Handles the HTML document wrapper, line-break separators between entries,
 * HTML escaping, wrapping plain text in colored spans, and agent-message
 * streaming with an inline cursor at the live edge.
 *
 * [committedBodyHtml] is the source of truth for finalized transcript content;
 * the active stream block is spliced at the document tail on each chunk.
 *
 * All mutations are marshalled onto the EDT via [runOnEdt].
 */
internal class TranscriptHtmlAppender(
    private val pane: JEditorPane,
    private val runOnEdt: ((() -> Unit) -> Unit)? = null,
) {
    private var htmlBodyInitialized = false
    private var isFirstEntry = true
    private var committedBodyHtml = ""
    private var isAgentStreamActive = false
    private val streamingPlainText = StringBuilder()
    private var streamLineSeparator = ""
    private var activeStreamBlockHtml = ""

    private val onEdt: (() -> Unit) -> Unit =
        runOnEdt ?: { action ->
            if (SwingUtilities.isEventDispatchThread()) {
                action()
            } else {
                SwingUtilities.invokeLater(action)
            }
        }

    fun startOrContinueAgentStream(text: String) {
        if (text.isBlank()) return
        onEdt { startOrContinueAgentStreamOnEdt(text) }
    }

    fun finalizeAgentStream() {
        onEdt { finalizeAgentStreamOnEdt() }
    }

    fun isAgentStreamActive(): Boolean = isAgentStreamActive

    fun appendLine(line: String) {
        onEdt {
            val escaped = TranscriptRenderHelpers.escapeHtml(line)
            val entry =
                if (line.startsWith("> ")) {
                    TranscriptRenderHelpers.userPromptSpan(escaped)
                } else {
                    TranscriptRenderHelpers.plainLineSpan(escaped)
                }
            appendNewEntryOnEdt(entry)
        }
    }

    fun appendHtml(fragment: String) {
        onEdt { appendNewEntryOnEdt(fragment) }
    }

    internal fun displayBodyHtmlForTest(): String = currentBodyHtml()

    private fun startOrContinueAgentStreamOnEdt(text: String) {
        ensureBody()
        if (!isAgentStreamActive) {
            streamLineSeparator = if (isFirstEntry) "" else TranscriptRenderHelpers.HTML_LINE_BREAK
            isFirstEntry = false
            isAgentStreamActive = true
            streamingPlainText.clear()
        }
        streamingPlainText.append(text)
        updateActiveStreamBlock()
        TranscriptPaneHtmlOps.scrollToEndIfAtBottom(pane)
    }

    private fun finalizeAgentStreamOnEdt() {
        if (!isAgentStreamActive) return
        val escaped = TranscriptRenderHelpers.escapeHtml(streamingPlainText.toString())
        val finalized = TranscriptStreamingCursor.finalizedBlockHtml(escaped, streamLineSeparator)
        removeActiveStreamBlockFromPane()
        committedBodyHtml += finalized
        TranscriptPaneHtmlOps.insertBeforeBodyEnd(pane, finalized)
        isAgentStreamActive = false
        streamingPlainText.clear()
        activeStreamBlockHtml = ""
    }

    private fun appendNewEntryOnEdt(entryHtml: String) {
        finalizeAgentStreamOnEdt()
        ensureBody()
        val separator = if (isFirstEntry) "" else TranscriptRenderHelpers.HTML_LINE_BREAK
        isFirstEntry = false
        val delta = separator + entryHtml
        committedBodyHtml += delta
        TranscriptPaneHtmlOps.insertBeforeBodyEnd(pane, delta)
        TranscriptPaneHtmlOps.scrollToEndIfAtBottom(pane)
    }

    private fun ensureBody() {
        if (!htmlBodyInitialized) {
            TranscriptPaneHtmlOps.initEmptyBody(pane)
            htmlBodyInitialized = true
        }
    }

    private fun currentBodyHtml(): String =
        if (isAgentStreamActive) {
            val escaped = TranscriptRenderHelpers.escapeHtml(streamingPlainText.toString())
            committedBodyHtml + TranscriptStreamingCursor.streamBlockHtml(escaped, streamLineSeparator)
        } else {
            committedBodyHtml
        }

    private fun updateActiveStreamBlock() {
        val escaped = TranscriptRenderHelpers.escapeHtml(streamingPlainText.toString())
        val newBlock = TranscriptStreamingCursor.streamBlockHtml(escaped, streamLineSeparator)
        removeActiveStreamBlockFromPane()
        activeStreamBlockHtml = newBlock
        TranscriptPaneHtmlOps.insertBeforeBodyEnd(pane, newBlock)
    }

    private fun removeActiveStreamBlockFromPane() {
        if (activeStreamBlockHtml.isEmpty()) return
        if (!TranscriptPaneHtmlOps.removeSuffixBeforeBodyEnd(pane, activeStreamBlockHtml)) {
            resyncPaneFromState()
            return
        }
        activeStreamBlockHtml = ""
    }

    private fun resyncPaneFromState() {
        val body = currentBodyHtml()
        TranscriptPaneHtmlOps.replaceBody(pane, body)
        activeStreamBlockHtml =
            if (isAgentStreamActive) {
                val escaped = TranscriptRenderHelpers.escapeHtml(streamingPlainText.toString())
                TranscriptStreamingCursor.streamBlockHtml(escaped, streamLineSeparator)
            } else {
                ""
            }
    }
}
