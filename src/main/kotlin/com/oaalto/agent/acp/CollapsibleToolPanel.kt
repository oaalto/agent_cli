package com.oaalto.agent.acp

import com.intellij.openapi.components.serviceOrNull
import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import com.oaalto.agent.acp.transcript.theme.DefaultTranscriptColorProvider
import com.oaalto.agent.acp.transcript.theme.TranscriptBadgeStyle
import com.oaalto.agent.acp.transcript.theme.TranscriptColorProvider
import com.oaalto.agent.acp.ui.AcpUiMetrics
import java.awt.BorderLayout
import java.awt.Component
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
import javax.swing.BoxLayout
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

/**
 * Collapsible tool-call card: badge header + optional mixed HTML / highlighted code body.
 */
internal class CollapsibleToolPanel(
    private val onToggle: (toolCallId: String) -> Unit,
    private val codeBlockViewFactory: TranscriptCodeBlockViewFactory,
) : JPanel(BorderLayout()) {
    private val colorProvider: TranscriptColorProvider
        get() = serviceOrNull<TranscriptColorProvider>() ?: DefaultTranscriptColorProvider()

    private val headerPanel =
        JPanel(FlowLayout(FlowLayout.LEFT, JBUI.scale(4), 0)).apply {
            isOpaque = true
            background = JBColor.PanelBackground
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
            foreground = JBColor.GRAY
        }
    private val bodyContainer =
        JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            isOpaque = false
            border = JBUI.Borders.empty()
            alignmentX = LEFT_ALIGNMENT
        }
    private var boundToolCallId: String = ""
    private var expandable = false
    private var boundBodyParts: List<TranscriptBodyPart>? = null
    private var bodyBuilt = false
    private val mapper: TranscriptBodyPartWidgetMapper =
        TranscriptBodyPartWidgetMapper(
            codeBlockViewFactory = codeBlockViewFactory,
            colorProvider = colorProvider,
            profile = BodyPartRenderProfile.TOOL,
        )
    internal val disposableCodeComponents = mutableListOf<JComponent>()

    init {
        border =
            BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(JBColor.border()),
                JBUI.Borders.empty(0, 0, AcpUiMetrics.COMPACT_INSET, 0),
            )
        background = JBColor.PanelBackground
        isOpaque = true
        alignmentX = Component.LEFT_ALIGNMENT
        headerPanel.add(badgeLabel)
        headerPanel.add(titleLabel)
        headerPanel.add(chevronLabel)
        add(headerPanel, BorderLayout.NORTH)
        add(bodyContainer, BorderLayout.CENTER)
        bodyContainer.isVisible = false

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
                    if (bodyContainer.isVisible) {
                        adjustBodyHeight()
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
        badgeLabel.background = colorProvider.getBadgeBackground(block.status)
        badgeLabel.foreground = colorProvider.getBadgeForeground(block.status)
        val displayTitle = TranscriptRenderer.displayToolTitle(block.title, block.toolCallId)
        titleLabel.text = displayTitle
        titleLabel.isVisible = displayTitle.isNotEmpty()
        titleLabel.foreground = colorProvider.getTextForeground()

        expandable = block.hasBodyContent
        if (expandable) {
            bindExpandableBody(block)
        } else {
            bindNonExpandableBody()
        }
        revalidate()
        repaint()
    }

    private fun bindExpandableBody(block: TranscriptBlock.ToolCallBlock) {
        headerPanel.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        chevronLabel.text = if (block.expanded) "▼" else "▶"
        if (block.expanded) {
            ensureExpandedBody(block.bodyParts)
            bodyContainer.isVisible = true
            adjustBodyHeight()
        } else {
            clearBodyIfBuilt()
            bodyContainer.isVisible = false
            bodyContainer.preferredSize = Dimension(0, 0)
        }
    }

    private fun bindNonExpandableBody() {
        headerPanel.cursor = Cursor.getDefaultCursor()
        chevronLabel.text = ""
        clearBodyIfBuilt()
        bodyContainer.isVisible = false
        bodyContainer.preferredSize = Dimension(0, 0)
    }

    private fun ensureExpandedBody(parts: List<TranscriptBodyPart>) {
        if (!bodyBuilt || boundBodyParts != parts) {
            rebuildBody(parts)
            boundBodyParts = parts
            bodyBuilt = true
        }
    }

    private fun clearBodyIfBuilt() {
        if (!bodyBuilt) return
        clearBody()
        boundBodyParts = null
        bodyBuilt = false
    }

    fun disposeCodeComponents() {
        disposableCodeComponents.forEach(codeBlockViewFactory::dispose)
        disposableCodeComponents.clear()
    }

    private fun rebuildBody(parts: List<TranscriptBodyPart>) {
        clearBody()
        val (column, disposables) = mapper.buildBodyColumn(parts)
        disposableCodeComponents.addAll(disposables)
        // Move mapper's column children into bodyContainer
        val widgets = column.components
        for (widget in widgets) {
            bodyContainer.add(widget)
        }
    }

    private fun clearBody() {
        disposeCodeComponents()
        bodyContainer.removeAll()
    }

    private fun adjustBodyHeight() {
        val width = width - insets.left - insets.right
        if (width <= 0) return
        bodyContainer.components.filterIsInstance<JComponent>().forEach { child ->
            applyTranscriptColumnWidth(child, width)
        }
        bodyContainer.setSize(width, 0)
        val height = bodyContainer.preferredSize.height.coerceAtLeast(1)
        bodyContainer.preferredSize = Dimension(width, height)
        revalidate()
    }

    override fun getMaximumSize(): Dimension {
        val pref = preferredSize
        return Dimension(Int.MAX_VALUE, pref.height)
    }
}
