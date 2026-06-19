package com.oaalto.agent.acp

import javax.swing.JEditorPane
import javax.swing.JScrollPane
import javax.swing.SwingUtilities

/** Low-level HTML document splice and scroll helpers for [TranscriptHtmlAppender]. */
internal object TranscriptPaneHtmlOps {
    private const val SCROLL_BOTTOM_THRESHOLD = 4

    fun initEmptyBody(pane: JEditorPane) {
        pane.text =
            TranscriptRenderHelpers.htmlDocumentStart() +
            TranscriptRenderHelpers.HTML_DOCUMENT_END
    }

    fun replaceBody(
        pane: JEditorPane,
        bodyHtml: String,
    ) {
        pane.text =
            TranscriptRenderHelpers.htmlDocumentStart() +
            bodyHtml +
            TranscriptRenderHelpers.HTML_DOCUMENT_END
    }

    fun insertBeforeBodyEnd(
        pane: JEditorPane,
        html: String,
    ) {
        val current = pane.text
        val bodyEnd = bodyEndIndex(current)
        if (bodyEnd >= 0) {
            pane.text = current.substring(0, bodyEnd) + html + current.substring(bodyEnd)
        }
    }

    fun removeSuffixBeforeBodyEnd(
        pane: JEditorPane,
        suffix: String,
    ): Boolean {
        if (suffix.isEmpty()) return true
        val current = pane.text
        val bodyEnd = bodyEndIndex(current)
        if (bodyEnd < 0) return false
        val beforeEnd = current.substring(0, bodyEnd)
        if (!beforeEnd.endsWith(suffix)) return false
        pane.text = beforeEnd.removeSuffix(suffix) + current.substring(bodyEnd)
        return true
    }

    fun scrollToEndIfAtBottom(pane: JEditorPane) {
        val scrollPane = SwingUtilities.getAncestorOfClass(JScrollPane::class.java, pane)
        if (scrollPane is JScrollPane) {
            val bar = scrollPane.verticalScrollBar
            val atBottom =
                bar.maximum <= bar.visibleAmount ||
                    bar.value + bar.visibleAmount >= bar.maximum - SCROLL_BOTTOM_THRESHOLD
            if (atBottom) {
                pane.caretPosition = pane.document.length
                bar.value = bar.maximum
            }
        } else {
            pane.caretPosition = pane.document.length
        }
    }

    private fun bodyEndIndex(text: String): Int = text.lastIndexOf("</body>")
}
