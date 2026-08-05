package com.oaalto.agent.acp

import com.intellij.util.ui.JBUI
import com.oaalto.agent.AgentCliLog
import com.oaalto.agent.acp.transcript.model.TranscriptBlock
import com.oaalto.agent.acp.transcript.render.TranscriptRenderer
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
        if (!matches(block)) {
            log.warn(
                "SimpleTextRowAdapter update type mismatch: " +
                    "component=${row::class.simpleName}, block=${block::class.simpleName}",
                context = context.logContextProvider(),
            )
            return false
        }
        if (row is SimpleTextRow) {
            row.bind(block)
            return true
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
                        widthAdjustment()
                    }
                },
            )
        }

        override fun getMaximumSize(): Dimension {
            widthAdjustment()
            val pref = preferredSize
            return Dimension(Int.MAX_VALUE, pref.height)
        }

        private fun widthAdjustment() {
            val w = width - insets.left - insets.right
            if (w > 0) {
                applyTranscriptColumnWidth(textPane, w)
            }
        }

        fun bind(block: TranscriptBlock) {
            textPane.text = formatSimpleText(block)
            textPane.foreground = resolveColor(block)
            revalidate()
            repaint()
        }

        private fun formatSimpleText(block: TranscriptBlock): String =
            when (block) {
                is TranscriptBlock.UserEcho -> "> ${block.text}"
                is TranscriptBlock.Thought -> "[thought] ${block.text}"
                is TranscriptBlock.PlainLine -> block.text
                is TranscriptBlock.ErrorLine -> TranscriptRenderer.formatError(block.message)
                is TranscriptBlock.AuthFailureLine -> TranscriptRenderer.formatAuthFailure(block.message)
                else -> ""
            }

        private fun resolveColor(block: TranscriptBlock): java.awt.Color {
            val cp = context.colorProvider
            return when (block) {
                is TranscriptBlock.UserEcho -> cp.getUserEchoColor()
                is TranscriptBlock.Thought -> cp.getThoughtColor()
                is TranscriptBlock.PlainLine ->
                    if (block.isUserPrompt) cp.getUserEchoColor() else cp.getTextForeground()
                is TranscriptBlock.ErrorLine -> cp.getErrorForeground()
                is TranscriptBlock.AuthFailureLine -> cp.getErrorForeground()
                else -> cp.getTextForeground()
            }
        }
    }
}
