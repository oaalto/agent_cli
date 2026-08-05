package com.oaalto.agent.acp.transcript.view

import com.agentclientprotocol.model.Cost
import com.intellij.ui.JBColor
import java.awt.FlowLayout
import java.text.NumberFormat
import javax.swing.JLabel
import javax.swing.JPanel

/**
 * Sticky footer status bar for the ACP transcript panel.
 * Displays cumulative token usage (`used / size`) and optional cost from [Cost] data.
 *
 * Usage label turns orange when `used / size > 0.8` to indicate approaching context limits.
 * Cost label is hidden when no cost data is available.
 */
internal class TranscriptFooter : JPanel(FlowLayout(FlowLayout.RIGHT, HORIZONTAL_GAP, VERTICAL_GAP)) {
    companion object {
        private const val HORIZONTAL_GAP = 8
        private const val VERTICAL_GAP = 4
        private const val HIGH_USAGE_THRESHOLD = 0.8f
    }

    internal val usageLabel = JLabel()
    internal val costLabel = JLabel()

    init {
        background = JBColor.PanelBackground
        usageLabel.foreground = JBColor.GRAY
        costLabel.foreground = JBColor.GRAY
        add(usageLabel)
        add(costLabel)
    }

    /**
     * Update the footer display with the given usage metrics.
     *
     * @param used cumulative tokens used
     * @param size total context window size
     * @param cost optional accumulated cost
     */
    fun updateUsage(
        used: Long,
        size: Long,
        cost: Cost?,
    ) {
        usageLabel.text = "${formatTokenCount(used)} / ${formatTokenCount(size)} tokens"
        costLabel.text = cost?.let { formatCost(it) }.orEmpty()
        costLabel.isVisible = cost != null

        // Color shift when exceeding 80% of context window
        usageLabel.foreground =
            if (size > 0 && used.toFloat() / size > HIGH_USAGE_THRESHOLD) {
                JBColor.ORANGE
            } else {
                JBColor.GRAY
            }
    }

    private fun formatTokenCount(count: Long): String = NumberFormat.getInstance().format(count)

    private fun formatCost(cost: Cost): String {
        val currencySymbol =
            when (cost.currency.uppercase()) {
                "USD" -> "$"
                "EUR" -> "€"
                "GBP" -> "£"
                else -> "${cost.currency} "
            }
        return "$currencySymbol${cost.amount}"
    }
}
