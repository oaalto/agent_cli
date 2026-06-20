package com.oaalto.agent.acp

import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import java.awt.Component
import javax.swing.BoxLayout
import javax.swing.JComponent
import javax.swing.JPanel

/** Vertical structured transcript container with incremental block sync. */
internal class TranscriptPanel(
    private val onToolToggle: (toolCallId: String) -> Unit,
) : JPanel() {
    private val column =
        JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            isOpaque = false
            alignmentX = Component.LEFT_ALIGNMENT
        }
    private val scrollPane: JBScrollPane =
        JBScrollPane(column).apply {
            border = JBUI.Borders.empty()
            verticalScrollBar.unitIncrement = 16
        }
    private val componentsByBlockId = linkedMapOf<String, JPanel>()

    init {
        layout = java.awt.BorderLayout()
        isOpaque = false
        add(scrollPane, java.awt.BorderLayout.CENTER)
    }

    val component: JComponent get() = scrollPane

    fun sync(blocks: List<TranscriptBlock>) {
        val seenIds = linkedSetOf<String>()
        blocks.forEachIndexed { index, block ->
            seenIds += block.blockId
            val existing = componentsByBlockId[block.blockId]
            if (existing == null) {
                val created = TranscriptBlockViewFactory.create(block, onToolToggle)
                componentsByBlockId[block.blockId] = created
                insertAt(index, created)
            } else {
                TranscriptBlockViewFactory.update(existing, block)
                ensureOrder(index, existing)
            }
        }
        componentsByBlockId.keys.filter { it !in seenIds }.forEach { id ->
            componentsByBlockId.remove(id)?.let { column.remove(it) }
        }
        column.revalidate()
        column.repaint()
    }

    fun scrollToEndIfAtBottom() {
        val bar = scrollPane.verticalScrollBar
        val atBottom =
            bar.maximum <= bar.visibleAmount ||
                bar.value + bar.visibleAmount >= bar.maximum - SCROLL_BOTTOM_THRESHOLD
        if (atBottom) {
            bar.value = bar.maximum
        }
    }

    private fun insertAt(
        index: Int,
        row: JPanel,
    ) {
        val clamped = index.coerceIn(0, column.componentCount)
        if (clamped == column.componentCount) {
            column.add(row)
        } else {
            column.add(row, clamped)
        }
    }

    private fun ensureOrder(
        index: Int,
        row: JPanel,
    ) {
        val currentIndex = column.components.indexOf(row)
        if (currentIndex < 0 || currentIndex == index) return
        column.remove(row)
        val clamped = index.coerceIn(0, column.componentCount)
        if (clamped == column.componentCount) {
            column.add(row)
        } else {
            column.add(row, clamped)
        }
    }

    companion object {
        private const val SCROLL_BOTTOM_THRESHOLD = 4
    }
}
