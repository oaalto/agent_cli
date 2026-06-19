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
    fun htmlDocumentStart(bodyStyle: String = ""): String = "<html><body style=\"$bodyStyle\">"

    /**
     * Returns an HTML `<span>` for a tool-call status line with a colored badge.
     */
    fun formatToolStatusHtml(
        title: String,
        kind: ToolKind?,
        status: ToolCallStatus?,
    ): String {
        val kindLabel = kind?.name?.lowercase()?.replace('_', ' ') ?: "tool"
        val statusLabel = status?.name?.lowercase()?.replace('_', ' ') ?: "started"
        val badgeColor = badgeColorFor(status)
        val escapedTitle = TranscriptUpdateRenderer.escapeHtml(title)
        val escapedKind = TranscriptUpdateRenderer.escapeHtml(kindLabel)
        val escapedStatus = TranscriptUpdateRenderer.escapeHtml(statusLabel)
        return (
            "<span style=\"color:#cccccc;font-family:$FONT_FAMILY;font-size:$FONT_SIZE\">" +
                "[<span style=\"background-color:$badgeColor;color:#ffffff;padding:1px 4px;border-radius:3px\">" +
                "$escapedKind</span>] $escapedTitle ($escapedStatus)</span>"
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
}
