package com.oaalto.agent.acp

import javax.swing.JEditorPane

/**
 * Manages appending content into a [JEditorPane] in `text/html` mode.
 *
 * Handles the HTML document wrapper, line-break separators between entries,
 * HTML escaping, and wrapping plain text in colored spans.
 */
internal class TranscriptHtmlAppender(
    private val pane: JEditorPane,
) {
    private var htmlBodyInitialized = false
    private var isFirstEntry = true

    // -- Public append methods -------------------------------------------------

    /**
     * Appends plain [text] that will be HTML-escaped and inserted inline
     * (no `<br>` separator). Used for streaming chunks.
     */
    fun appendText(text: String) {
        ensureBody()
        val escaped = TranscriptUpdateRenderer.escapeHtml(text)
        insertBeforeBodyEnd(escaped)
        pane.caretPosition = pane.document.length
    }

    /**
     * Appends plain [line] as a new line. The text is HTML-escaped and wrapped
     * in a colored span. A `<br>` is prepended unless this is the first entry.
     */
    fun appendLine(line: String) {
        ensureBody()
        val escaped = TranscriptUpdateRenderer.escapeHtml(line)
        val htmlLine =
            when {
                line.startsWith("> ") -> userSpan(escaped)
                else -> neutralSpan(escaped)
            }
        val separator = if (isFirstEntry) "" else TranscriptRenderHelpers.HTML_LINE_BREAK
        isFirstEntry = false
        insertBeforeBodyEnd(separator + htmlLine)
        pane.caretPosition = pane.document.length
    }

    /**
     * Appends a pre-rendered HTML [fragment] as a new line without escaping.
     * A `<br>` is prepended unless this is the first entry.
     */
    fun appendHtml(fragment: String) {
        ensureBody()
        val separator = if (isFirstEntry) "" else TranscriptRenderHelpers.HTML_LINE_BREAK
        isFirstEntry = false
        insertBeforeBodyEnd(separator + fragment)
        pane.caretPosition = pane.document.length
    }

    // -- Internal helpers ------------------------------------------------------

    private fun ensureBody() {
        if (!htmlBodyInitialized) {
            pane.text = TranscriptRenderHelpers.htmlDocumentStart()
            htmlBodyInitialized = true
        }
    }

    private fun insertBeforeBodyEnd(html: String) {
        val current = pane.text
        val bodyEnd = current.lastIndexOf("</body>")
        if (bodyEnd >= 0) {
            pane.text = current.substring(0, bodyEnd) + html + current.substring(bodyEnd)
        }
    }

    private fun userSpan(text: String): String =
        "<span style=\"color:#569cd6;font-family:monospace;font-size:12px\">$text</span>"

    private fun neutralSpan(text: String): String =
        "<span style=\"color:#d4d4d4;font-family:monospace;font-size:12px\">$text</span>"
}
