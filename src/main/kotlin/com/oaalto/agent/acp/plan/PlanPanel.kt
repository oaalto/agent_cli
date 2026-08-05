package com.oaalto.agent.acp.plan

import com.agentclientprotocol.model.ToolCallStatus
import com.intellij.openapi.components.serviceOrNull
import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import com.oaalto.agent.acp.PlanEntry
import com.oaalto.agent.acp.PlanEntryPriority
import com.oaalto.agent.acp.PlanEntryStatus
import com.oaalto.agent.acp.TranscriptBlock
import com.oaalto.agent.acp.TranscriptRenderHelpers
import com.oaalto.agent.acp.transcript.theme.DefaultTranscriptColorProvider
import com.oaalto.agent.acp.transcript.theme.TranscriptColorProvider
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.Dimension
import java.awt.Font
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JLabel
import javax.swing.JPanel

/** Lazily accessed color provider for theme-aware colors */
private val colorProvider: TranscriptColorProvider
    get() = serviceOrNull<TranscriptColorProvider>() ?: DefaultTranscriptColorProvider()

internal class PlanPanel : JPanel(BorderLayout()) {
    private val contentColumn =
        JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            isOpaque = false
            alignmentX = Component.LEFT_ALIGNMENT
        }

    private var boundBlockId: String? = null
    private var boundPlanId: String? = null

    init {
        isOpaque = true
        background = colorProvider.getPanelBackground()
        border =
            JBUI.Borders.compound(
                BorderFactory.createEmptyBorder(PADDING_LARGE, PADDING_XLARGE, PADDING_LARGE, PADDING_XLARGE),
                BorderFactory.createMatteBorder(
                    BORDER_WIDTH,
                    LEFT_BORDER_THICKNESS,
                    BORDER_WIDTH,
                    BORDER_WIDTH,
                    JBColor.border(),
                ),
            )
        alignmentX = Component.LEFT_ALIGNMENT
        add(contentColumn, BorderLayout.CENTER)
    }

    /**
     * Disposes of this panel's resources.
     * For API consistency with other panel types.
     */
    fun dispose() {
        // No resources to dispose, but method provided for API consistency
    }

    fun bind(block: TranscriptBlock.PlanBlock) {
        if (boundBlockId == block.blockId && boundPlanId == block.planId) {
            updateInPlace(block)
            return
        }
        boundBlockId = block.blockId
        boundPlanId = block.planId
        rebuildContent(block)
    }

    private fun rebuildContent(block: TranscriptBlock.PlanBlock) {
        contentColumn.removeAll()

        // Apply dismissed styling if plan is dismissed
        if (block.dismissed) {
            background = JBColor.PanelBackground
        } else {
            background = colorProvider.getPanelBackground()
        }

        val headerText = if (block.dismissed) "$HEADER_TEXT $DISMISSED_INDICATOR" else HEADER_TEXT
        val headerLabel =
            JLabel(headerText).apply {
                font = Font(FONT_FAMILY, Font.BOLD, FONT_SIZE)
                foreground =
                    if (block.dismissed) {
                        colorProvider.getThoughtColor()
                    } else {
                        colorProvider.getTextForeground()
                    }
                alignmentX = Component.LEFT_ALIGNMENT
            }
        contentColumn.add(headerLabel)
        contentColumn.add(Box.createVerticalStrut(PADDING_MEDIUM))

        block.entries.forEachIndexed { index, entry ->
            val entryRow = createEntryRow(index + FIRST_ENTRY_NUMBER, entry, block.dismissed)
            contentColumn.add(entryRow)
            contentColumn.add(Box.createVerticalStrut(PADDING_SMALL))
        }

        if (block.entries.isNotEmpty()) {
            addSummaryFooter(block)
        }

        revalidate()
        repaint()
    }

    private fun addSummaryFooter(block: TranscriptBlock.PlanBlock) {
        val completed = block.completedCount
        val total = block.entries.size
        val isComplete = block.isFullyComplete

        val summaryColor =
            when {
                block.dismissed -> colorProvider.getThoughtColor()
                isComplete -> colorProvider.getBadgeBackground(com.agentclientprotocol.model.ToolCallStatus.COMPLETED)
                else -> colorProvider.getThoughtColor()
            }

        val summaryText =
            when {
                block.dismissed -> DISMISSED_SUMMARY_TEXT
                else -> "$completed of $total completed"
            }

        val summaryLabel =
            JLabel(summaryText).apply {
                font = Font(FONT_FAMILY, Font.PLAIN, FONT_SIZE_SMALL)
                foreground = summaryColor
                alignmentX = Component.RIGHT_ALIGNMENT
            }
        contentColumn.add(Box.createVerticalStrut(PADDING_TINY))
        contentColumn.add(summaryLabel)
    }

    private fun updateInPlace(block: TranscriptBlock.PlanBlock) {
        rebuildContent(block)
    }

    private fun createEntryRow(
        number: Int,
        entry: PlanEntry,
        dismissed: Boolean = false,
    ): JPanel {
        val row =
            JPanel(BorderLayout()).apply {
                isOpaque = false
                alignmentX = Component.LEFT_ALIGNMENT
            }

        val iconLabel = createIconLabel(entry.status, dismissed)
        val contentLabel = createContentLabel(number, entry, dismissed)
        val leftPanel = createLeftPanel(iconLabel, entry.priority)

        row.add(leftPanel, BorderLayout.WEST)
        row.add(contentLabel, BorderLayout.CENTER)

        if (!dismissed) {
            applyPriorityBorder(row, leftPanel, entry.priority)
        }

        row.maximumSize = Dimension(Int.MAX_VALUE, row.preferredSize.height)

        return row
    }

    private fun createIconLabel(
        status: PlanEntryStatus,
        dismissed: Boolean = false,
    ): JLabel {
        val icon = statusIcon(status)
        val iconColor = if (dismissed) colorProvider.getThoughtColor() else statusColor(status)

        return JLabel(icon).apply {
            font = Font(FONT_FAMILY, Font.PLAIN, FONT_SIZE)
            foreground = iconColor
        }
    }

    private fun createContentLabel(
        number: Int,
        entry: PlanEntry,
        dismissed: Boolean = false,
    ): JLabel {
        val contentText = "$number. ${TranscriptRenderHelpers.escapeHtml(entry.content)}"
        val textColor =
            if (dismissed) {
                colorProvider.getThoughtColor()
            } else {
                priorityTextColor(entry.priority)
            }

        return JLabel(contentText).apply {
            font = if (dismissed) Font(FONT_FAMILY, Font.PLAIN, FONT_SIZE) else fontForPriority(entry.priority)
            foreground = textColor
        }
    }

    private fun createLeftPanel(
        iconLabel: JLabel,
        priority: PlanEntryPriority,
    ): JPanel {
        val leftPad = if (priority == PlanEntryPriority.HIGH) PADDING_MEDIUM else 0

        return JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            isOpaque = false
            add(iconLabel)
            add(Box.createHorizontalStrut(PADDING_LARGE))
            if (leftPad > 0) {
                border = BorderFactory.createEmptyBorder(BORDER_WIDTH, leftPad, BORDER_WIDTH, BORDER_WIDTH)
            }
        }
    }

    private fun applyPriorityBorder(
        row: JPanel,
        leftPanel: JPanel,
        priority: PlanEntryPriority,
    ) {
        if (priority == PlanEntryPriority.HIGH) {
            row.border =
                BorderFactory.createMatteBorder(
                    BORDER_WIDTH,
                    HIGH_PRIORITY_BORDER_THICKNESS,
                    BORDER_WIDTH,
                    BORDER_WIDTH,
                    colorProvider.getErrorForeground(),
                )
            leftPanel.border = BorderFactory.createEmptyBorder(BORDER_WIDTH, PADDING_MEDIUM, BORDER_WIDTH, BORDER_WIDTH)
        }
    }

    private fun fontForPriority(priority: PlanEntryPriority): Font {
        val style = if (priority == PlanEntryPriority.HIGH) Font.BOLD else Font.PLAIN
        return Font(FONT_FAMILY, style, FONT_SIZE)
    }

    private fun priorityTextColor(priority: PlanEntryPriority): Color =
        when (priority) {
            PlanEntryPriority.HIGH -> colorProvider.getTextForeground()
            PlanEntryPriority.MEDIUM -> colorProvider.getTextForeground()
            PlanEntryPriority.LOW -> colorProvider.getThoughtColor()
        }

    private fun statusIcon(status: PlanEntryStatus): String =
        when (status) {
            PlanEntryStatus.PENDING -> PENDING_ICON
            PlanEntryStatus.IN_PROGRESS -> IN_PROGRESS_ICON
            PlanEntryStatus.COMPLETED -> COMPLETED_ICON
        }

    private fun statusColor(status: PlanEntryStatus): Color =
        when (status) {
            PlanEntryStatus.PENDING -> colorProvider.getThoughtColor()
            PlanEntryStatus.IN_PROGRESS -> colorProvider.getBadgeBackground(ToolCallStatus.IN_PROGRESS)
            PlanEntryStatus.COMPLETED -> colorProvider.getBadgeBackground(ToolCallStatus.COMPLETED)
        }

    companion object {
        private const val FONT_FAMILY = "Monospaced"
        private const val FONT_SIZE = 12
        private const val FONT_SIZE_SMALL = 11

        private const val FIRST_ENTRY_NUMBER = 1

        private const val PADDING_TINY = 2
        private const val PADDING_SMALL = 4
        private const val PADDING_MEDIUM = 6
        private const val PADDING_LARGE = 8
        private const val PADDING_XLARGE = 12

        private const val BORDER_WIDTH = 0
        private const val LEFT_BORDER_THICKNESS = 3
        private const val HIGH_PRIORITY_BORDER_THICKNESS = 2

        private const val HEADER_TEXT = "Plan"
        private const val DISMISSED_INDICATOR = "[dismissed]"
        private const val DISMISSED_SUMMARY_TEXT = "Plan dismissed"
        private const val PENDING_ICON = "[ ]"
        private const val IN_PROGRESS_ICON = "[→]"
        private const val COMPLETED_ICON = "[✓]"
    }
}
