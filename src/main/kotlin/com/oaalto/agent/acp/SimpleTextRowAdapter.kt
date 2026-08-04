package com.oaalto.agent.acp

import com.intellij.util.ui.JBUI
import com.oaalto.agent.AgentCliLog
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import java.awt.Font
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import javax.swing.JPanel
import javax.swing.JTextPane
import javax.swing.border.EmptyBorder

/**
 * Adapter for simple text transcript blocks.
 *
 * Covers: [TranscriptBlock.UserEcho], [TranscriptBlock.Thought], [TranscriptBlock.PlainLine],
 * [TranscriptBlock.ErrorLine], [TranscriptBlock.AuthFailureLine].
 *
 * Uses a single [JTextPane] row shell, separate from agent text rows per ADR 0006.
 */
internal const val SIMPLE_TEXT_ROW_MARKER = "transcript.simpleTextRow"

/** Check if a panel was created by SimpleTextRowAdapter. */
internal fun isSimpleTextRow(panel: JPanel): Boolean = panel.getClientProperty(SIMPLE_TEXT_ROW_MARKER) == true

internal class SimpleTextRowAdapter : TranscriptBlockRowAdapter {
    private val log = AgentCliLog.getInstance(SimpleTextRowAdapter::class.java)

    override fun matches(block: TranscriptBlock): Boolean =
        block is TranscriptBlock.UserEcho ||
            block is TranscriptBlock.Thought ||
            block is TranscriptBlock.PlainLine ||
            block is TranscriptBlock.ErrorLine ||
            block is TranscriptBlock.AuthFailureLine

    override fun create(
        context: RowContext,
        block: TranscriptBlock,
        onToolToggle: (String) -> Unit,
    ): JPanel = SimpleTextRow(context).apply { bind(block) }

    override fun update(
        context: RowContext,
        row: JPanel,
        block: TranscriptBlock,
    ): Boolean {
        if (row is SimpleTextRow) {
            row.bind(block)
            return true
        }
        if (!matches(block)) {
            log.warn(
                "SimpleTextRowAdapter update type mismatch: " +
                    "component=${row::class.simpleName}, block=${block::class.simpleName}",
                context = context.logContextProvider(),
            )
            return false
        }
        log.warn(
            "SimpleTextRowAdapter update mismatch: " +
                "component=${row::class.simpleName} is not SimpleTextRow, block=${block::class.simpleName}",
            context = context.logContextProvider(),
        )
        return false
    }

    override fun dispose(
        context: RowContext,
        row: JPanel,
    ) {
        // SimpleTextRow has no disposable resources.
    }

    private class SimpleTextRow(
        private val context: RowContext,
    ) : JPanel(BorderLayout()) {
        private val textPane: JTextPane =
            JTextPane().apply {
                isEditable = false
                isOpaque = false
                border = EmptyBorder(0, 0, 0, 0)
                font = Font("Monospaced", Font.PLAIN, 12)
                alignmentX = Component.LEFT_ALIGNMENT
            }

        init {
            putClientProperty(SIMPLE_TEXT_ROW_MARKER, true)
            border = JBUI.Borders.empty(2, 0)
            isOpaque = false
            alignmentX = Component.LEFT_ALIGNMENT
            add(textPane, BorderLayout.CENTER)
            addComponentListener(
                object : ComponentAdapter() {
                    override fun componentResized(event: ComponentEvent) {
                        val w = width - insets.left - insets.right
                        if (w > 0) {
                            textPane.preferredSize = Dimension(w, textPane.preferredSize.height)
                        }
                    }
                },
            )
        }

        override fun getMaximumSize(): Dimension {
            val pref = preferredSize
            return Dimension(Int.MAX_VALUE, pref.height)
        }

        fun bind(block: TranscriptBlock) {
            val colorProvider = context.colorProvider
            textPane.foreground = colorProvider.getTextForeground()
            when (block) {
                is TranscriptBlock.UserEcho -> {
                    textPane.foreground = colorProvider.getUserEchoColor()
                    textPane.text = "> ${block.text}"
                }
                is TranscriptBlock.Thought -> {
                    textPane.foreground = colorProvider.getThoughtColor()
                    textPane.text = "[thought] ${block.text}"
                }
                is TranscriptBlock.PlainLine -> {
                    textPane.foreground =
                        if (block.isUserPrompt) {
                            colorProvider.getUserEchoColor()
                        } else {
                            colorProvider.getTextForeground()
                        }
                    textPane.text = block.text
                }
                is TranscriptBlock.ErrorLine -> {
                    textPane.foreground = colorProvider.getErrorForeground()
                    textPane.text = TranscriptRenderer.formatError(block.message)
                }
                is TranscriptBlock.AuthFailureLine -> {
                    textPane.foreground = colorProvider.getErrorForeground()
                    textPane.text = TranscriptRenderer.formatAuthFailure(block.message)
                }
                else -> Unit
            }
            revalidate()
            repaint()
        }
    }
}
