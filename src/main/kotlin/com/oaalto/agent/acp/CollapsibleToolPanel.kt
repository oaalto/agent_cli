package com.oaalto.agent.acp

import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import com.oaalto.agent.acp.ui.AcpUiMetrics
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Cursor
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.BorderFactory
import javax.swing.JEditorPane
import javax.swing.JLabel
import javax.swing.JPanel

/**
 * Collapsible tool-call card: badge header + optional HTML body (Step 4 content).
 */
internal class CollapsibleToolPanel(
    private val onToggle: (toolCallId: String) -> Unit,
) : JPanel(BorderLayout()) {
    private val headerPanel =
        JPanel(FlowLayout(FlowLayout.LEFT, JBUI.scale(4), 0)).apply {
            isOpaque = true
            background =
                JBColor(
                    Color(TranscriptPalette.CARD_HEADER_DARK_RGB),
                    Color(TranscriptPalette.CARD_HEADER_LIGHT_RGB),
                )
            border = JBUI.Borders.empty(4, 6)
            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            isFocusable = true
        }
    private val badgeLabel =
        JLabel().apply {
            isOpaque = true
            border = BorderFactory.createEmptyBorder(1, 4, 1, 4)
        }
    private val titleLabel = JLabel()
    private val chevronLabel =
        JLabel().apply {
            foreground =
                JBColor(
                    Color(TranscriptPalette.MUTED_CHEVRON_DARK_RGB),
                    Color(TranscriptPalette.MUTED_CHEVRON_LIGHT_RGB),
                )
        }
    private val bodyPane =
        JEditorPane("text/html", "").apply {
            isEditable = false
            isOpaque = false
            border = JBUI.Borders.emptyLeft(20)
        }
    private var boundToolCallId: String = ""
    private var expandable = false

    init {
        border =
            BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(
                    JBColor(
                        Color(TranscriptPalette.CARD_BORDER_DARK_RGB),
                        Color(TranscriptPalette.CARD_BORDER_LIGHT_RGB),
                    ),
                ),
                JBUI.Borders.empty(0, 0, AcpUiMetrics.COMPACT_INSET, 0),
            )
        background =
            JBColor(
                Color(TranscriptPalette.CARD_BACKGROUND_DARK_RGB),
                Color(TranscriptPalette.CARD_BACKGROUND_LIGHT_RGB),
            )
        isOpaque = true
        headerPanel.add(badgeLabel)
        headerPanel.add(titleLabel)
        headerPanel.add(chevronLabel)
        add(headerPanel, BorderLayout.NORTH)
        add(bodyPane, BorderLayout.CENTER)
        bodyPane.isVisible = false

        val toggleListener =
            object : MouseAdapter() {
                override fun mouseClicked(event: MouseEvent) {
                    if (expandable) {
                        onToggle(boundToolCallId)
                    }
                }
            }
        headerPanel.addMouseListener(toggleListener)
        headerPanel.addKeyListener(
            object : KeyAdapter() {
                override fun keyPressed(event: KeyEvent) {
                    if (expandable && (event.keyCode == KeyEvent.VK_SPACE || event.keyCode == KeyEvent.VK_ENTER)) {
                        onToggle(boundToolCallId)
                    }
                }
            },
        )
        addComponentListener(
            object : ComponentAdapter() {
                override fun componentResized(event: ComponentEvent) {
                    if (bodyPane.isVisible) {
                        adjustBodyPaneHeight()
                    }
                }
            },
        )
    }

    fun bind(block: TranscriptBlock.ToolCallBlock) {
        boundToolCallId = block.toolCallId
        val kindLabel =
            block.kind
                ?.name
                ?.lowercase()
                ?.replace('_', ' ') ?: "tool"
        val badgeText = TranscriptBadgeStyle.label(block.status, kindLabel)
        badgeLabel.text = badgeText
        badgeLabel.background = Color.decode(TranscriptBadgeStyle.colorHex(block.status))
        badgeLabel.foreground = Color.WHITE
        titleLabel.text = block.title
        titleLabel.foreground =
            JBColor(
                Color(TranscriptPalette.TOOL_TITLE_DARK_RGB),
                Color(TranscriptPalette.TOOL_TITLE_LIGHT_RGB),
            )

        expandable = block.hasBodyContent
        if (expandable) {
            headerPanel.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            chevronLabel.text = if (block.expanded) "▼" else "▶"
            bodyPane.text = bodyHtml(block.contentFragments)
            bodyPane.isVisible = block.expanded
            if (block.expanded) {
                adjustBodyPaneHeight()
            } else {
                bodyPane.preferredSize = Dimension(0, 0)
            }
        } else {
            headerPanel.cursor = Cursor.getDefaultCursor()
            chevronLabel.text = ""
            bodyPane.text = ""
            bodyPane.isVisible = false
            bodyPane.preferredSize = Dimension(0, 0)
        }
        revalidate()
        repaint()
    }

    private fun adjustBodyPaneHeight() {
        val width = width - insets.left - insets.right
        if (width <= 0) return
        bodyPane.setSize(width, Int.MAX_VALUE)
        val height = bodyPane.preferredSize.height
        bodyPane.preferredSize = Dimension(width, height)
        revalidate()
    }

    private fun bodyHtml(fragments: List<String>): String {
        if (fragments.isEmpty()) return ""
        return (
            TranscriptRenderHelpers.htmlDocumentStart() +
                fragments.joinToString(TranscriptRenderHelpers.HTML_LINE_BREAK) +
                TranscriptRenderHelpers.HTML_DOCUMENT_END
        )
    }
}
