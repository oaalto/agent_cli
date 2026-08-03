package com.oaalto.agent.acp

import java.awt.Dimension
import javax.swing.JComponent
import javax.swing.JEditorPane
import javax.swing.JTextPane

/** Keeps a vertical Box column child within the available transcript width after resize. */
internal fun applyTranscriptColumnWidth(
    child: JComponent,
    width: Int,
) {
    if (applyTranscriptCodeBlockWidth(child, width)) {
        return
    }
    if (child is JTextPane || child is JEditorPane) {
        child.setSize(width, Int.MAX_VALUE)
        val height = child.preferredSize.height
        child.preferredSize = Dimension(width, height)
        child.maximumSize = Dimension(Int.MAX_VALUE, height)
    } else {
        val height = child.preferredSize.height
        child.setSize(width, height)
        child.preferredSize = Dimension(width, height)
        child.maximumSize = Dimension(Int.MAX_VALUE, height)
    }
}
