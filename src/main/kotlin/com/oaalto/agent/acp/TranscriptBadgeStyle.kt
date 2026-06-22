package com.oaalto.agent.acp

import com.agentclientprotocol.model.ToolCallStatus
import com.intellij.openapi.components.serviceOrNull

/**
 * Badge color and label rules shared by HTML and Swing tool headers.
 *
 * Colors are resolved dynamically from [TranscriptColorProvider] to respect
 * the current IDE theme.
 */
internal object TranscriptBadgeStyle {
    /**
     * Returns the badge background color as a hex string for HTML rendering.
     * Colors are dynamically resolved from the color provider.
     * Falls back to hardcoded values when service is unavailable (e.g., in tests).
     */
    fun colorHex(status: ToolCallStatus?): String {
        val provider = serviceOrNull<TranscriptColorProvider>()
        return if (provider != null) {
            val color = provider.getBadgeBackground(status)
            provider.toHtml(color)
        } else {
            // Fallback for tests or when service is unavailable
            when (status) {
                ToolCallStatus.IN_PROGRESS -> "#d4a017"
                ToolCallStatus.COMPLETED -> "#2d8a4e"
                ToolCallStatus.FAILED -> "#c43c3c"
                else -> "#666666"
            }
        }
    }

    /**
     * Returns the badge foreground (text) color as a hex string for HTML rendering.
     */
    fun foregroundColorHex(status: ToolCallStatus?): String {
        val provider = serviceOrNull<TranscriptColorProvider>()
        return if (provider != null) {
            val color = provider.getBadgeForeground(status)
            provider.toHtml(color)
        } else {
            // Fallback for tests
            "#ffffff"
        }
    }

    fun label(
        status: ToolCallStatus?,
        kindLabel: String,
    ): String =
        when (status) {
            ToolCallStatus.COMPLETED -> "✓ $kindLabel"
            ToolCallStatus.FAILED -> "✗ $kindLabel"
            else -> kindLabel
        }
}
