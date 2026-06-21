package com.oaalto.agent.acp

import com.intellij.openapi.diagnostic.Logger
import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.Dimension
import java.awt.Font
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JComponent
import javax.swing.JEditorPane
import javax.swing.JPanel
import javax.swing.JTextPane
import javax.swing.border.EmptyBorder

/** Maps [TranscriptBlock] snapshots to Swing row components. */
internal class TranscriptBlockViewFactory(
    private val codeBlockViewFactory: TranscriptCodeBlockViewFactory,
) {
    private val log = Logger.getInstance(TranscriptBlockViewFactory::class.java)

    fun create(
        block: TranscriptBlock,
        onToolToggle: (toolCallId: String) -> Unit,
    ): JPanel =
        when (block) {
            is TranscriptBlock.ToolCallBlock ->
                CollapsibleToolPanel(onToolToggle, codeBlockViewFactory).apply { bind(block) }
            else -> AgentTextRow(codeBlockViewFactory).apply { bind(block) }
        }

    fun update(
        component: JPanel,
        block: TranscriptBlock,
    ) {
        when {
            component is CollapsibleToolPanel && block is TranscriptBlock.ToolCallBlock ->
                component.bind(block)
            component is AgentTextRow && block !is TranscriptBlock.ToolCallBlock ->
                component.bind(block)
            component is CollapsibleToolPanel || block is TranscriptBlock.ToolCallBlock ->
                log.warn(
                    "Transcript block/component type mismatch: " +
                        "component=${component::class.simpleName}, block=${block::class.simpleName}",
                )
            else ->
                log.warn(
                    "Transcript text row type mismatch: " +
                        "component=${component::class.simpleName}, block=${block::class.simpleName}",
                )
        }
    }

    fun disposeRow(component: JPanel) {
        when (component) {
            is CollapsibleToolPanel -> component.disposeCodeComponents()
            is AgentTextRow -> component.disposeCodeComponents()
        }
    }

    private class AgentTextRow(
        private val codeBlockViewFactory: TranscriptCodeBlockViewFactory,
    ) : JPanel(BorderLayout()) {
        private val contentColumn =
            JPanel().apply {
                layout = BoxLayout(this, BoxLayout.Y_AXIS)
                isOpaque = false
                alignmentX = Component.LEFT_ALIGNMENT
            }
        private val disposableCodeComponents = mutableListOf<JComponent>()
        private var segmentedFinalText: String? = null

        init {
            border = ROW_BORDER
            isOpaque = false
            alignmentX = Component.LEFT_ALIGNMENT
            add(contentColumn, BorderLayout.CENTER)
            addComponentListener(
                object : ComponentAdapter() {
                    override fun componentResized(event: ComponentEvent) {
                        adjustWidths()
                    }
                },
            )
        }

        override fun getMaximumSize(): Dimension {
            adjustWidths()
            val pref = preferredSize
            return Dimension(Int.MAX_VALUE, pref.height)
        }

        fun bind(block: TranscriptBlock) {
            when (block) {
                is TranscriptBlock.StreamingAgentText ->
                    bindSimpleTextPane { it.bindTranscriptBlock(block) }
                is TranscriptBlock.FinalAgentText -> bindFinalAgentText(block)
                else ->
                    bindSimpleTextPane { it.bindTranscriptBlock(block) }
            }
            adjustWidths()
            revalidate()
            repaint()
        }

        fun disposeCodeComponents() {
            disposableCodeComponents.forEach(codeBlockViewFactory::dispose)
            disposableCodeComponents.clear()
            segmentedFinalText = null
        }

        private fun bindFinalAgentText(block: TranscriptBlock.FinalAgentText) {
            val segments = segmentFencedCodeBlocks(block.text)
            if (!segments.containsCode()) {
                segmentedFinalText = null
                bindSimpleTextPane { it.bindTranscriptBlock(block) }
                return
            }
            if (segmentedFinalText == block.text) {
                return
            }
            rebuildSegmentedFinalAgentText(segments)
            segmentedFinalText = block.text
        }

        private fun bindSimpleTextPane(apply: (JTextPane) -> Unit) {
            segmentedFinalText = null
            if (contentColumn.componentCount == 1 && disposableCodeComponents.isEmpty()) {
                val existing = contentColumn.getComponent(0)
                if (existing is JTextPane) {
                    apply(existing)
                    return
                }
            }
            disposeCodeComponents()
            contentColumn.removeAll()
            val pane = createTextPane()
            apply(pane)
            contentColumn.add(pane)
        }

        private fun rebuildSegmentedFinalAgentText(segments: List<TextSegment>) {
            disposeCodeComponents()
            contentColumn.removeAll()
            var highlightedBlocks = 0
            segments.forEach { segment ->
                when (segment) {
                    is TextSegment.Prose ->
                        if (segment.text.isNotEmpty()) {
                            contentColumn.add(
                                createProsePane(TranscriptTextTruncation.truncate(segment.text)),
                            )
                        }
                    is TextSegment.Code -> {
                        val displayCode = TranscriptTextTruncation.truncate(segment.text)
                        if (highlightedBlocks < TranscriptToolCallContentRenderer.MAX_HIGHLIGHTED_CODE_BLOCKS) {
                            highlightedBlocks += 1
                            val codeComponent =
                                codeBlockViewFactory
                                    .createReadOnlyCodeBlock(segment.languageId, displayCode)
                                    .also {
                                        it.alignmentX = Component.LEFT_ALIGNMENT
                                        it.maximumSize = Dimension(Int.MAX_VALUE, it.preferredSize.height)
                                    }
                            disposableCodeComponents += codeComponent
                            contentColumn.add(codeComponent)
                        } else {
                            contentColumn.add(createPlainPreHtmlPart(displayCode))
                        }
                    }
                }
                contentColumn.add(Box.createVerticalStrut(JBUI.scale(BODY_PART_GAP)))
            }
        }

        private fun createTextPane(): JTextPane =
            JTextPane().apply {
                isEditable = false
                isOpaque = false
                border = EmptyBorder(0, 0, 0, 0)
                font = Font(MONO_FAMILY, Font.PLAIN, FONT_SIZE)
            }

        private fun createProsePane(text: String): JTextPane =
            createTextPane().apply {
                foreground =
                    JBColor(
                        Color(TranscriptPalette.AGENT_TEXT_DARK_RGB),
                        Color(TranscriptPalette.AGENT_TEXT_LIGHT_RGB),
                    )
                this.text = text
            }

        private fun createPlainPreHtmlPart(text: String): JEditorPane =
            JEditorPane(
                "text/html",
                TranscriptRenderHelpers.htmlDocumentStart() +
                    TranscriptToolCallTextBodyRenderer.renderPlainPreBody(text) +
                    TranscriptRenderHelpers.HTML_DOCUMENT_END,
            ).apply {
                isEditable = false
                isOpaque = false
                border = EmptyBorder(0, 0, 0, 0)
                alignmentX = Component.LEFT_ALIGNMENT
                putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true)
            }

        private fun adjustWidths() {
            val width = width - insets.left - insets.right
            if (width <= 0) return
            contentColumn.components.forEach { child ->
                when (child) {
                    is JTextPane -> {
                        child.setSize(width, Int.MAX_VALUE)
                        val height = child.preferredSize.height
                        child.preferredSize = Dimension(width, height)
                        child.maximumSize = Dimension(Int.MAX_VALUE, height)
                    }
                    is JComponent -> {
                        child.setSize(width, child.preferredSize.height)
                        child.maximumSize = Dimension(Int.MAX_VALUE, child.preferredSize.height)
                    }
                }
            }
            contentColumn.setSize(width, Int.MAX_VALUE)
            contentColumn.preferredSize = Dimension(width, contentColumn.preferredSize.height)
        }
    }

    companion object {
        private const val MONO_FAMILY = "Monospaced"
        private const val FONT_SIZE = 12
        private const val BODY_PART_GAP = 4
        private val ROW_BORDER = JBUI.Borders.empty(2, 0)
    }
}
