package com.oaalto.agent.acp

import com.intellij.openapi.diagnostic.Logger
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import java.awt.Font
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import javax.swing.JPanel
import javax.swing.JTextPane
import javax.swing.border.EmptyBorder

/** Maps [TranscriptBlock] snapshots to Swing row components. */
internal object TranscriptBlockViewFactory {
    private val LOG = Logger.getInstance(TranscriptBlockViewFactory::class.java)
    private const val MONO_FAMILY = "Monospaced"
    private const val FONT_SIZE = 12
    private val ROW_BORDER = JBUI.Borders.empty(2, 0)

    fun create(
        block: TranscriptBlock,
        onToolToggle: (toolCallId: String) -> Unit,
    ): JPanel =
        when (block) {
            is TranscriptBlock.ToolCallBlock -> CollapsibleToolPanel(onToolToggle).apply { bind(block) }
            else -> WrappingTextRow().apply { bind(block) }
        }

    fun update(
        component: JPanel,
        block: TranscriptBlock,
    ) {
        when {
            component is CollapsibleToolPanel && block is TranscriptBlock.ToolCallBlock ->
                component.bind(block)
            component is WrappingTextRow && block !is TranscriptBlock.ToolCallBlock ->
                component.bind(block)
            component is CollapsibleToolPanel || block is TranscriptBlock.ToolCallBlock ->
                LOG.warn(
                    "Transcript block/component type mismatch: " +
                        "component=${component::class.simpleName}, block=${block::class.simpleName}",
                )
            else ->
                LOG.warn(
                    "Transcript text row type mismatch: " +
                        "component=${component::class.simpleName}, block=${block::class.simpleName}",
                )
        }
    }

    private class WrappingTextRow : JPanel(BorderLayout()) {
        private val textPane =
            JTextPane().apply {
                isEditable = false
                isOpaque = false
                border = EmptyBorder(0, 0, 0, 0)
                font = Font(MONO_FAMILY, Font.PLAIN, FONT_SIZE)
            }

        init {
            border = ROW_BORDER
            isOpaque = false
            alignmentX = Component.LEFT_ALIGNMENT
            add(textPane, BorderLayout.CENTER)
            addComponentListener(
                object : ComponentAdapter() {
                    override fun componentResized(event: ComponentEvent) {
                        adjustTextPaneWidth()
                    }
                },
            )
        }

        override fun getMaximumSize(): Dimension {
            adjustTextPaneWidth()
            val pref = preferredSize
            return Dimension(Int.MAX_VALUE, pref.height)
        }

        fun bind(block: TranscriptBlock) {
            textPane.bindTranscriptBlock(block)
            adjustTextPaneWidth()
            revalidate()
            repaint()
        }

        private fun adjustTextPaneWidth() {
            val width = width - insets.left - insets.right
            if (width <= 0) return
            textPane.setSize(width, Int.MAX_VALUE)
            val height = textPane.preferredSize.height
            textPane.preferredSize = Dimension(width, height)
        }
    }
}
