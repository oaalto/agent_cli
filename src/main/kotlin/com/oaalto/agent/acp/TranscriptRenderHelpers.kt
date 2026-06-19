package com.oaalto.agent.acp

import com.agentclientprotocol.model.ToolCallStatus
import com.agentclientprotocol.model.ToolKind

/**
 * HTML formatting helpers for transcript rendering.
 *
 * Each method returns a self-contained inline `<span>` fragment. Fragments
 * should be separated by [HTML_LINE_BREAK] when displayed as distinct lines.
 */
internal object TranscriptRenderHelpers {
    private const val FONT_FAMILY = "monospace"
    private const val FONT_SIZE = "12px"

    /** `<br>` separator for use between HTML entries. */
    internal const val HTML_LINE_BREAK: String = "<br>"

    /** Closing tags for the HTML document. */
    internal const val HTML_DOCUMENT_END: String = "</body></html>"

    /**
     * Returns the initial HTML document wrapper. Call once before appending fragments.
     */
    fun htmlDocumentStart(bodyStyle: String = ""): String =
        "<html><head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\"></head>" +
            "<body style=\"$bodyStyle\">"

    /**
     * Returns an HTML `<span>` for a tool-call status line with a colored badge.
     *
     * Layout: `[badge: kind + optional icon] [muted title]` — no bracket syntax or
     * parenthetical status labels.
     */
    fun formatToolStatusHtml(
        title: String,
        kind: ToolKind?,
        status: ToolCallStatus?,
    ): String {
        val kindLabel = kind?.name?.lowercase()?.replace('_', ' ') ?: "tool"
        val badgeColor = badgeColorFor(status)
        val escapedTitle = TranscriptUpdateRenderer.escapeHtml(title)
        val escapedKind = TranscriptUpdateRenderer.escapeHtml(kindLabel)
        val badgeLabel = badgeLabelFor(status, escapedKind)
        val outerStyle = "color:#cccccc;font-family:$FONT_FAMILY;font-size:$FONT_SIZE"
        val badgeStyle = "background-color:$badgeColor;color:#ffffff;padding:1px 4px;border-radius:3px"
        val titleStyle = "color:#cccccc;font-family:$FONT_FAMILY;font-size:$FONT_SIZE"
        return (
            "<span style=\"$outerStyle\">" +
                "<span style=\"$badgeStyle\">$badgeLabel</span> " +
                "<span style=\"$titleStyle\">$escapedTitle</span>" +
                "</span>"
        )
    }

    /**
     * Returns an HTML `<span>` for an error message (red foreground).
     */
    fun formatErrorHtml(message: String): String {
        val escaped = TranscriptUpdateRenderer.escapeHtml(message)
        val style = "color:#f44747;font-family:$FONT_FAMILY;font-size:$FONT_SIZE"
        return "<span style=\"$style\">Error: $escaped</span>"
    }

    /**
     * Returns an HTML `<span>` for an auth failure message.
     */
    fun formatAuthFailureHtml(message: String): String {
        val escaped = TranscriptUpdateRenderer.escapeHtml(message)
        val style = "color:#f44747;font-family:$FONT_FAMILY;font-size:$FONT_SIZE"
        return "<span style=\"$style\">Auth failed: $escaped</span>"
    }

    /**
     * Returns an HTML `<span>` for a terminal-create line.
     */
    fun formatTerminalCreateHtml(
        command: String,
        terminalId: String,
    ): String {
        val escapedCmd = TranscriptUpdateRenderer.escapeHtml(command)
        val escapedId = TranscriptUpdateRenderer.escapeHtml(terminalId)
        return (
            "<span style=\"color:#cccccc;font-family:$FONT_FAMILY;font-size:$FONT_SIZE\">" +
                "[terminal] $escapedCmd (id=$escapedId)</span>"
        )
    }

    /**
     * Returns an HTML `<span>` for a permission-denied line.
     */
    fun formatPermissionDeniedHtml(title: String): String {
        val escaped = TranscriptUpdateRenderer.escapeHtml(title)
        return (
            "<span style=\"color:#f44747;font-family:$FONT_FAMILY;font-size:$FONT_SIZE\">" +
                "[permission denied] $escaped</span>"
        )
    }

    private fun badgeColorFor(status: ToolCallStatus?): String =
        when (status) {
            ToolCallStatus.IN_PROGRESS -> "#d4a017"
            ToolCallStatus.COMPLETED -> "#2d8a4e"
            ToolCallStatus.FAILED -> "#c43c3c"
            else -> "#666666"
        }

    internal fun badgeLabelFor(
        status: ToolCallStatus?,
        escapedKind: String,
    ): String =
        when (status) {
            ToolCallStatus.COMPLETED -> "✓ $escapedKind"
            ToolCallStatus.FAILED -> "✗ $escapedKind"
            else -> escapedKind
        }
}
