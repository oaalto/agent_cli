package com.oaalto.agent.acp

import com.agentclientprotocol.model.ToolCallStatus

/** Badge color and label rules shared by HTML and Swing tool headers. */
internal object TranscriptBadgeStyle {
    fun colorHex(status: ToolCallStatus?): String =
        when (status) {
            ToolCallStatus.IN_PROGRESS -> "#d4a017"
            ToolCallStatus.COMPLETED -> "#2d8a4e"
            ToolCallStatus.FAILED -> "#c43c3c"
            else -> "#666666"
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
