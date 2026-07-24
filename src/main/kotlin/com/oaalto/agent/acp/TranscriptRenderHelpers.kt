package com.oaalto.agent.acp

import com.agentclientprotocol.model.ToolCallStatus
import com.agentclientprotocol.model.ToolKind
import com.intellij.openapi.components.serviceOrNull

/**
 * HTML formatting helpers for transcript rendering.
 *
 * Each method returns a self-contained inline `<span>` fragment. Fragments
 * should be separated by [HTML_LINE_BREAK] when displayed as distinct lines.
 *
 * Colors are resolved dynamically from [TranscriptColorProvider] to respect
 * the current IDE theme.
 */
internal object TranscriptRenderHelpers {
    private const val FONT_FAMILY = "monospace"
    private const val FONT_SIZE = "12px"

    private val fallbackProvider: TranscriptColorProvider by lazy {
        // Fallback provider for tests or when service is unavailable
        DefaultTranscriptColorProvider()
    }

    private fun getProvider(): TranscriptColorProvider = serviceOrNull<TranscriptColorProvider>() ?: fallbackProvider

    fun escapeHtml(text: String): String =
        text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")

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
        val badgeColor = TranscriptBadgeStyle.colorHex(status)
        val badgeFgColor = TranscriptBadgeStyle.foregroundColorHex(status)
        val escapedTitle = escapeHtml(title)
        val escapedKind = escapeHtml(kindLabel)
        val badgeLabel = TranscriptBadgeStyle.label(status, escapedKind)
        val provider = getProvider()
        val textColor = provider.toHtml(provider.getTextForeground())
        val outerStyle = "color:$textColor;font-family:$FONT_FAMILY;font-size:$FONT_SIZE"
        val badgeStyle = "background-color:$badgeColor;color:$badgeFgColor;padding:1px 4px;border-radius:3px"
        val titleStyle = "color:$textColor;font-family:$FONT_FAMILY;font-size:$FONT_SIZE"
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
        val escaped = escapeHtml(message)
        val provider = getProvider()
        val errorColor = provider.toHtml(provider.getErrorForeground())
        val style = "color:$errorColor;font-family:$FONT_FAMILY;font-size:$FONT_SIZE"
        return "<span style=\"$style\">Error: $escaped</span>"
    }

    /**
     * Returns an HTML `<span>` for an auth failure message.
     */
    fun formatAuthFailureHtml(message: String): String {
        val escaped = escapeHtml(message)
        val provider = getProvider()
        val errorColor = provider.toHtml(provider.getErrorForeground())
        val style = "color:$errorColor;font-family:$FONT_FAMILY;font-size:$FONT_SIZE"
        return "<span style=\"$style\">Auth failed: $escaped</span>"
    }

    /**
     * Returns an HTML `<span>` for a terminal-create line.
     */
    fun formatTerminalCreateHtml(
        command: String,
        terminalId: String,
    ): String {
        val escapedCmd = escapeHtml(command)
        val escapedId = escapeHtml(terminalId)
        val provider = getProvider()
        val textColor = provider.toHtml(provider.getTextForeground())
        return (
            "<span style=\"color:$textColor;font-family:$FONT_FAMILY;font-size:$FONT_SIZE\">" +
                "[terminal] $escapedCmd (id=$escapedId)</span>"
        )
    }

    /**
     * Returns an HTML `<span>` for a permission-denied line.
     */
    fun formatPermissionDeniedHtml(title: String): String {
        val escaped = escapeHtml(title)
        val provider = getProvider()
        val errorColor = provider.toHtml(provider.getErrorForeground())
        return (
            "<span style=\"color:$errorColor;font-family:$FONT_FAMILY;font-size:$FONT_SIZE\">" +
                "[permission denied] $escaped</span>"
        )
    }
}
