package com.oaalto.agent.acp

import com.agentclientprotocol.model.ToolCallStatus
import com.intellij.ui.JBColor
import com.intellij.util.ui.UIUtil
import java.awt.Color
import java.util.Locale

/**
 * Central color authority for ACP transcript UI components.
 *
 * Provides theme-aware colors that automatically adapt to the IDE's light/dark theme.
 */
internal interface TranscriptColorProvider {
    /** Background color for transcript panels. */
    fun getPanelBackground(): Color

    /** Primary text foreground color. */
    fun getTextForeground(): Color

    /** Error/warning text foreground color. */
    fun getErrorForeground(): Color

    /** Hyperlink text foreground color. */
    fun getLinkForeground(): Color

    /** User echo text color (for user prompts). */
    fun getUserEchoColor(): Color

    /** Thought/muted text color. */
    fun getThoughtColor(): Color

    /** Background color for status badges (pending, success, error). */
    fun getBadgeBackground(status: ToolCallStatus?): Color

    /** Foreground color for status badges (pending, success, error). */
    fun getBadgeForeground(status: ToolCallStatus?): Color

    /** Convert a [Color] to a lowercase CSS hex string (e.g., "#d4a017"). */
    fun toHtml(color: Color): String
}

/**
 * Default [TranscriptColorProvider] implementation using IntelliJ Platform JBColor utilities.
 *
 * Automatically adapts to IDE theme changes through JBColor's built-in theme switching.
 */
internal class DefaultTranscriptColorProvider : TranscriptColorProvider {
    override fun getPanelBackground(): Color = JBColor.PanelBackground

    override fun getTextForeground(): Color = JBColor.foreground()

    override fun getErrorForeground(): Color =
        JBColor(
            UIUtil.getErrorForeground(),
            UIUtil.getErrorForeground(),
        )

    override fun getLinkForeground(): Color = JBColor.BLUE

    override fun getUserEchoColor(): Color = JBColor.BLUE

    override fun getThoughtColor(): Color =
        JBColor(
            UIUtil.getLabelDisabledForeground(),
            UIUtil.getLabelDisabledForeground(),
        )

    override fun getBadgeBackground(status: ToolCallStatus?): Color =
        when (status) {
            ToolCallStatus.IN_PROGRESS -> JBColor.ORANGE
            ToolCallStatus.COMPLETED -> JBColor.GREEN
            ToolCallStatus.FAILED -> JBColor.RED
            else -> JBColor.GRAY
        }

    override fun getBadgeForeground(status: ToolCallStatus?): Color = JBColor.WHITE

    override fun toHtml(color: Color): String =
        String.format(Locale.US, "#%02x%02x%02x", color.red, color.green, color.blue)
}
