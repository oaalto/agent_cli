package com.oaalto.agent.acp.plan

import com.oaalto.agent.acp.PlanEntry
import com.oaalto.agent.acp.PlanEntryPriority
import com.oaalto.agent.acp.PlanEntryStatus
import com.oaalto.agent.acp.PlanVariant
import com.oaalto.agent.acp.TranscriptRenderHelpers

/**
 * Renders plan entries and variants to HTML fragments.
 *
 * Each plan panel has a stable ID attribute for in-place replacement when
 * subsequent updates arrive for the same plan ID.
 */
internal object PlanPanelRenderer {
    private const val FONT_FAMILY = "monospace"
    private const val FONT_SIZE = "12px"

    // Colors matching the transcript palette
    private const val COLOR_PENDING = "#888888"
    private const val COLOR_IN_PROGRESS = "#d4a017"
    private const val COLOR_COMPLETED = "#2d8a4e"
    private const val COLOR_HIGH_PRIORITY = "#c43c3c"
    private const val COLOR_LOW_PRIORITY = "#999999"
    private const val COLOR_MUTED = "#888888"
    private const val COLOR_PANEL_BG_DARK = "#313335"
    private const val COLOR_BORDER = "#444444"

    // Markdown truncation limit (first N lines)
    private const val MARKDOWN_TRUNCATE_LINES = 3

    // Icons per the PRD
    private const val ICON_PENDING = "[ ]"
    private const val ICON_IN_PROGRESS = "[→]"
    private const val ICON_COMPLETED = "[✓]"

    /**
     * Renders a plan block to an HTML fragment with a stable ID.
     *
     * @param planId The stable plan ID for DOM replacement
     * @param entries The plan entries to render
     * @param variant The plan variant type
     * @param dismissed Whether the plan has been dismissed
     * @return HTML fragment ready for insertion
     */
    fun render(
        planId: String,
        entries: List<PlanEntry>,
        variant: PlanVariant = PlanVariant.Items,
        dismissed: Boolean = false,
    ): String {
        val escapedPlanId = TranscriptRenderHelpers.escapeHtml(planId)

        return when (variant) {
            is PlanVariant.Items -> renderItemsPlan(escapedPlanId, entries, dismissed)
            is PlanVariant.File -> renderFilePlan(escapedPlanId, variant.uri, dismissed)
            is PlanVariant.Markdown -> renderMarkdownPlan(escapedPlanId, variant.content, dismissed)
        }
    }

    private fun renderItemsPlan(
        escapedPlanId: String,
        entries: List<PlanEntry>,
        dismissed: Boolean,
    ): String {
        if (entries.isEmpty()) {
            return renderEmptyPlan(escapedPlanId, dismissed)
        }

        val completedCount = entries.count { it.status == PlanEntryStatus.COMPLETED }
        val totalCount = entries.size

        val panelStyle = buildPanelStyle(dismissed, completedCount == totalCount)
        val headerHtml = buildHeaderHtml(dismissed)
        val entriesHtml =
            entries
                .mapIndexed { index, entry ->
                    renderEntry(index + 1, entry, dismissed)
                }.joinToString("")
        val summaryHtml = buildSummaryHtml(dismissed, completedCount, totalCount)

        return (
            "<div id=\"plan-$escapedPlanId\" style=\"$panelStyle\">" +
                headerHtml +
                entriesHtml +
                summaryHtml +
                "</div>"
        )
    }

    private fun buildPanelStyle(
        dismissed: Boolean,
        isComplete: Boolean,
    ): String {
        val borderColor =
            when {
                dismissed -> COLOR_MUTED
                isComplete -> COLOR_COMPLETED
                else -> COLOR_BORDER
            }

        return buildString {
            append("font-family:$FONT_FAMILY;font-size:$FONT_SIZE;")
            append("margin:8px 0;padding:8px 12px;")
            append("border-left:3px solid $borderColor;")
            if (dismissed) {
                append("background-color:#2A2A2A;")
                append("opacity:0.7;")
            }
        }
    }

    private fun buildHeaderHtml(dismissed: Boolean): String {
        val headerText = if (dismissed) "Plan [dismissed]" else "Plan"
        val headerColor = if (dismissed) COLOR_MUTED else "#cccccc"
        return "<div style=\"color:$headerColor;font-weight:bold;margin-bottom:6px;\">$headerText</div>"
    }

    private fun buildSummaryHtml(
        dismissed: Boolean,
        completedCount: Int,
        totalCount: Int,
    ): String {
        val summaryColor = if (dismissed) COLOR_MUTED else COLOR_COMPLETED
        val summaryText = if (dismissed) "Plan dismissed" else "$completedCount of $totalCount completed"
        return "<div style=\"text-align:right;color:$summaryColor;margin-top:6px;\">$summaryText</div>"
    }

    private fun renderEntry(
        number: Int,
        entry: PlanEntry,
        dismissed: Boolean = false,
    ): String {
        val escapedContent = TranscriptRenderHelpers.escapeHtml(entry.content)
        val icon = statusIcon(entry.status)
        val iconColor = if (dismissed) COLOR_MUTED else statusColor(entry.status)

        val priorityStyle =
            when {
                dismissed -> "color:$COLOR_MUTED;"
                entry.priority == PlanEntryPriority.HIGH ->
                    "font-weight:bold;border-left:2px solid $COLOR_HIGH_PRIORITY;padding-left:6px;"
                entry.priority == PlanEntryPriority.LOW -> "color:$COLOR_LOW_PRIORITY;"
                else -> ""
            }

        val textColor = if (dismissed) COLOR_MUTED else "#d4d4d4"

        val entryStyle =
            buildString {
                append("margin:4px 0;display:flex;align-items:baseline;")
                append(priorityStyle)
            }

        return (
            "<div style=\"$entryStyle\">" +
                "<span style=\"color:$iconColor;margin-right:8px;\">$icon</span>" +
                "<span style=\"color:$textColor;\">$number. $escapedContent</span>" +
                "</div>"
        )
    }

    private fun renderFilePlan(
        escapedPlanId: String,
        uri: String,
        dismissed: Boolean,
    ): String {
        val escapedUri = TranscriptRenderHelpers.escapeHtml(uri)
        val prefix = if (dismissed) "[plan file - dismissed]" else "[plan file]"
        val panelStyle =
            buildString {
                append("font-family:$FONT_FAMILY;font-size:$FONT_SIZE;")
                append("margin:8px 0;padding:6px 12px;")
                append("color:$COLOR_MUTED;")
                if (dismissed) {
                    append("opacity:0.7;")
                }
            }

        return (
            "<div id=\"plan-$escapedPlanId\" style=\"$panelStyle\">" +
                "$prefix $escapedUri" +
                "</div>"
        )
    }

    private fun renderMarkdownPlan(
        escapedPlanId: String,
        content: String,
        dismissed: Boolean,
    ): String {
        // Truncate to first N lines with ellipsis if longer
        val lines = content.lines()
        val truncated =
            if (lines.size > MARKDOWN_TRUNCATE_LINES) {
                lines.take(MARKDOWN_TRUNCATE_LINES).joinToString("\n") + "\n…"
            } else {
                content
            }
        val escaped = TranscriptRenderHelpers.escapeHtml(truncated)
        val headerText = if (dismissed) "Plan (markdown) [dismissed]" else "Plan (markdown)"
        val headerColor = if (dismissed) COLOR_MUTED else "#cccccc"
        val textColor = if (dismissed) COLOR_MUTED else "#d4d4d4"
        val panelStyle =
            buildString {
                append("font-family:$FONT_FAMILY;font-size:$FONT_SIZE;")
                append("margin:8px 0;padding:8px 12px;")
                append("background-color:$COLOR_PANEL_BG_DARK;")
                append("white-space:pre-wrap;")
                if (dismissed) {
                    append("opacity:0.7;")
                }
            }

        return (
            "<div id=\"plan-$escapedPlanId\" style=\"$panelStyle\">" +
                "<div style=\"color:$headerColor;font-weight:bold;margin-bottom:6px;\">$headerText</div>" +
                "<pre style=\"margin:0;color:$textColor;\">$escaped</pre>" +
                "</div>"
        )
    }

    private fun renderEmptyPlan(
        escapedPlanId: String,
        dismissed: Boolean = false,
    ): String {
        val text = if (dismissed) "[empty plan - dismissed]" else "[empty plan]"
        val panelStyle =
            buildString {
                append("font-family:$FONT_FAMILY;font-size:$FONT_SIZE;")
                append("margin:8px 0;padding:4px 12px;")
                append("color:$COLOR_MUTED;")
                if (dismissed) {
                    append("opacity:0.7;")
                }
            }

        return (
            "<div id=\"plan-$escapedPlanId\" style=\"$panelStyle\">" +
                text +
                "</div>"
        )
    }

    private fun statusIcon(status: PlanEntryStatus): String =
        when (status) {
            PlanEntryStatus.PENDING -> ICON_PENDING
            PlanEntryStatus.IN_PROGRESS -> ICON_IN_PROGRESS
            PlanEntryStatus.COMPLETED -> ICON_COMPLETED
        }

    private fun statusColor(status: PlanEntryStatus): String =
        when (status) {
            PlanEntryStatus.PENDING -> COLOR_PENDING
            PlanEntryStatus.IN_PROGRESS -> COLOR_IN_PROGRESS
            PlanEntryStatus.COMPLETED -> COLOR_COMPLETED
        }
}
