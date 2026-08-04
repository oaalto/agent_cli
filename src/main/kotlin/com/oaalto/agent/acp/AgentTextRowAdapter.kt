package com.oaalto.agent.acp

import com.intellij.util.ui.JBUI
import com.oaalto.agent.AgentCliLog
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import java.awt.Font
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import javax.swing.BoxLayout
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTextPane
import javax.swing.SwingUtilities
import javax.swing.border.EmptyBorder

/** Client property marker for panels created by AgentTextRowAdapter. */
internal const val AGENT_TEXT_ROW_MARKER = "transcript.agentTextRow"

/** Check if a panel was created by AgentTextRowAdapter. */
internal fun isAgentTextRow(panel: JPanel): Boolean = panel.getClientProperty(AGENT_TEXT_ROW_MARKER) == true

private const val MONO_FAMILY = "Monospaced"
private const val FONT_SIZE = 12
private val ROW_BORDER = JBUI.Borders.empty(2, 0)

/**
 * Adapter for [TranscriptBlock.StreamingAgentText] and [TranscriptBlock.FinalAgentText].
 *
 * Uses a multi-part row shell with contentColumn + body-part widgets per ADR 0006.
 * Streaming cursor display, finalize rebuild, hyperlink handling, and code-editor disposal live here.
 */
internal class AgentTextRowAdapter : TranscriptBlockRowAdapter {
    private val log = AgentCliLog.getInstance(AgentTextRowAdapter::class.java)

    override fun matches(block: TranscriptBlock): Boolean =
        block is TranscriptBlock.StreamingAgentText || block is TranscriptBlock.FinalAgentText

    override fun create(
        context: RowContext,
        block: TranscriptBlock,
        onToolToggle: (String) -> Unit,
    ): JPanel {
        require(matches(block)) { "Expected agent text block, got ${block::class.simpleName}" }
        return AgentTextRow(context).apply {
            putClientProperty(AGENT_TEXT_ROW_MARKER, true)
            bind(block)
        }
    }

    override fun update(
        context: RowContext,
        row: JPanel,
        block: TranscriptBlock,
    ): Boolean {
        if (!matches(block)) return false
        if (row is AgentTextRow) {
            row.bind(block)
            return true
        }
        log.warn(
            "AgentTextRowAdapter update mismatch: " +
                "component=${row::class.simpleName} is not AgentTextRow, block=${block::class.simpleName}",
            context = context.logContextProvider(),
        )
        return false
    }

    override fun dispose(
        context: RowContext,
        row: JPanel,
    ) {
        if (row is AgentTextRow) {
            row.disposeCodeComponents()
        }
    }

    /**
     * Agent text row shell: contentColumn with body-part widgets.
     *
     * Separate from simple-text rows per ADR 0006.
     */
    private class AgentTextRow(
        private val context: RowContext,
    ) : JPanel(BorderLayout()) {
        private val contentColumn: JPanel =
            JPanel().apply {
                layout = BoxLayout(this, BoxLayout.Y_AXIS)
                isOpaque = false
                alignmentX = Component.LEFT_ALIGNMENT
            }
        private val disposableCodeComponents = mutableListOf<JComponent>()
        private var renderedFinalText: String? = null
        private val mapper: TranscriptBodyPartWidgetMapper =
            TranscriptBodyPartWidgetMapper(
                codeBlockViewFactory = context.codeBlockViewFactory,
                colorProvider = context.colorProvider,
                profile = BodyPartRenderProfile.AGENT,
            )

        init {
            putClientProperty(AGENT_TEXT_ROW_MARKER, true)
            border = ROW_BORDER
            isOpaque = false
            alignmentX = Component.LEFT_ALIGNMENT
            add(contentColumn, BorderLayout.CENTER)
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

        fun bind(block: TranscriptBlock) {
            when (block) {
                is TranscriptBlock.FinalAgentText -> bindFinalAgent(block)
                is TranscriptBlock.StreamingAgentText -> bindStreamingAgent(block)
                else -> {
                    // Should not happen: matches() guards against non-agent blocks
                    // but keep else for Kotlin exhaustiveness
                }
            }
            widthAdjustment()
            revalidate()
            repaint()
        }

        private fun bindFinalAgent(block: TranscriptBlock.FinalAgentText) {
            val parts =
                TranscriptContentRenderer.renderMarkdownText(
                    block.text,
                    ContentRenderOptions.AGENT_TEXT,
                )
            // Skip rebuild if same text already rendered
            if (parts.isNotEmpty() && renderedFinalText == block.text) {
                return
            }
            rebuildBodyParts(parts)
            renderedFinalText = block.text
            if (disposableCodeComponents.isNotEmpty()) {
                SwingUtilities.invokeLater {
                    widthAdjustment()
                    revalidate()
                    repaint()
                }
            }
        }

        private fun bindStreamingAgent(block: TranscriptBlock.StreamingAgentText) {
            setupSimpleTextPane { pane ->
                pane.foreground = context.colorProvider.getTextForeground()
                pane.text = normalizeAgentFences(block.text) + TranscriptStreamingCursor.CURSOR_CHAR
            }
        }

        fun disposeCodeComponents() {
            disposableCodeComponents.forEach(context.codeBlockViewFactory::dispose)
            disposableCodeComponents.clear()
            renderedFinalText = null
        }

        private fun widthAdjustment() {
            val w = width - insets.left - insets.right
            if (w <= 0) return
            contentColumn.components.filterIsInstance<JComponent>().forEach { child ->
                applyTranscriptColumnWidth(child, w)
            }
        }

        private fun setupSimpleTextPane(apply: (JTextPane) -> Unit) {
            renderedFinalText = null
            // Reuse existing JTextPane if available (streaming in-place update)
            if (contentColumn.componentCount == 1 && disposableCodeComponents.isEmpty()) {
                val existing = contentColumn.getComponent(0)
                if (existing is JTextPane) {
                    apply(existing)
                    return
                }
            }
            // Clear and create new text pane
            disposeCodeComponents()
            contentColumn.removeAll()
            val pane =
                JTextPane().apply {
                    isEditable = false
                    isOpaque = false
                    border = EmptyBorder(0, 0, 0, 0)
                    font = Font(MONO_FAMILY, Font.PLAIN, FONT_SIZE)
                    alignmentX = Component.LEFT_ALIGNMENT
                }
            apply(pane)
            contentColumn.add(pane)
        }

        private fun rebuildBodyParts(parts: List<TranscriptBodyPart>) {
            disposeCodeComponents()
            contentColumn.removeAll()

            if (parts.isEmpty()) return

            val (column, disposables) = mapper.buildBodyColumn(parts)
            disposableCodeComponents.clear()
            disposableCodeComponents.addAll(disposables)

            // Transfer widgets from mapper's column to contentColumn
            val widgets = column.components
            for (widget in widgets) {
                contentColumn.add(widget)
            }
        }
    }
}
