package com.oaalto.agent.acp.plan

import com.intellij.openapi.components.serviceOrNull
import com.oaalto.agent.acp.TranscriptRenderHelpers
import com.oaalto.agent.acp.transcript.model.PlanEntry
import com.oaalto.agent.acp.transcript.model.PlanEntryPriority
import com.oaalto.agent.acp.transcript.model.PlanEntryStatus
import com.oaalto.agent.acp.transcript.model.PlanVariant
import com.oaalto.agent.acp.transcript.theme.DefaultTranscriptColorProvider
import com.oaalto.agent.acp.transcript.theme.TranscriptColorProvider

/**
 * Renders plan entries and variants to HTML fragments.
 *
 * Each plan panel has a stable ID attribute for in-place replacement when
 * subsequent updates arrive for the same plan ID.
 *
 * Colors are resolved dynamically from [TranscriptColorProvider] to respect
 * the current IDE theme.
 */
@Suppress("TooManyFunctions")
internal object PlanPanelRenderer {
    private const val FONT_FAMILY = "monospace"
    private const val FONT_SIZE = "12px"

    private val provider: TranscriptColorProvider
        get() = serviceOrNull<TranscriptColorProvider>() ?: DefaultTranscriptColorProvider()

    private fun colorPending(): String = provider.toHtml(provider.getThoughtColor())

    private fun colorInProgress(): String =
        com.agentclientprotocol.model.ToolCallStatus.IN_PROGRESS.let {
            provider.toHtml(provider.getBadgeBackground(it))
        }

    private fun colorCompleted(): String =
        com.agentclientprotocol.model.ToolCallStatus.COMPLETED.let {
            provider.toHtml(provider.getBadgeBackground(it))
        }

    private fun colorHighPriority(): String = provider.toHtml(provider.getErrorForeground())

    private fun colorLowPriority(): String = provider.toHtml(provider.getThoughtColor())

    private fun colorMuted(): String = provider.toHtml(provider.getThoughtColor())

    private fun colorText(): String = provider.toHtml(provider.getTextForeground())

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
                dismissed -> colorMuted()
                isComplete -> colorCompleted()
                else -> colorText()
            }

        return buildString {
            append("font-family:$FONT_FAMILY;font-size:$FONT_SIZE;")
            append("margin:8px 0;padding:8px 12px;")
            append("border-left:3px solid $borderColor;")
            if (dismissed) {
                append("opacity:0.7;")
            }
        }
    }

    private fun buildHeaderHtml(dismissed: Boolean): String {
        val headerText = if (dismissed) "Plan [dismissed]" else "Plan"
        val headerColor = if (dismissed) colorMuted() else colorText()
        return "<div style=\"color:$headerColor;font-weight:bold;margin-bottom:6px;\">$headerText</div>"
    }

    private fun buildSummaryHtml(
        dismissed: Boolean,
        completedCount: Int,
        totalCount: Int,
    ): String {
        val summaryColor = if (dismissed) colorMuted() else colorCompleted()
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
        val iconColor = if (dismissed) colorMuted() else statusColor(entry.status)

        val priorityStyle =
            when {
                dismissed -> "color:${colorMuted()};"
                entry.priority == PlanEntryPriority.HIGH ->
                    "font-weight:bold;border-left:2px solid ${colorHighPriority()};padding-left:6px;"
                entry.priority == PlanEntryPriority.LOW -> "color:${colorLowPriority()};"
                else -> ""
            }

        val textColor = if (dismissed) colorMuted() else colorText()

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
                append("color:${colorMuted()};")
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
        val headerColor = if (dismissed) colorMuted() else colorText()
        val textColor = if (dismissed) colorMuted() else colorText()
        val panelStyle =
            buildString {
                append("font-family:$FONT_FAMILY;font-size:$FONT_SIZE;")
                append("margin:8px 0;padding:8px 12px;")
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
                append("color:${colorMuted()};")
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
            PlanEntryStatus.PENDING -> colorPending()
            PlanEntryStatus.IN_PROGRESS -> colorInProgress()
            PlanEntryStatus.COMPLETED -> colorCompleted()
        }
}
