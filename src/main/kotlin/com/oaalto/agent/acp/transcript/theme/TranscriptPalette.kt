package com.oaalto.agent.acp.transcript.theme

import com.intellij.openapi.components.service
import com.intellij.ui.JBColor
import java.awt.Color

/**
 * Shared color constants for structured transcript Swing components.
 *
 * This object serves as a facade that delegates to [TranscriptColorProvider]
 * for theme-aware color resolution. All properties are computed dynamically
 * to respect the current IDE theme.
 */
internal object TranscriptPalette {
    private val provider: TranscriptColorProvider
        get() = service<TranscriptColorProvider>()

    /** User echo text color as JBColor */
    val USER_ECHO_COLOR: Color
        get() = provider.getUserEchoColor()

    @Deprecated("Use USER_ECHO_COLOR instead", ReplaceWith("USER_ECHO_COLOR.rgb"))
    val USER_ECHO_RGB: Int
        get() = USER_ECHO_COLOR.rgb

    /** Thought/muted text color as JBColor */
    val THOUGHT_COLOR: Color
        get() = provider.getThoughtColor()

    @Deprecated("Use THOUGHT_COLOR instead", ReplaceWith("THOUGHT_COLOR.rgb"))
    val THOUGHT_RGB: Int
        get() = THOUGHT_COLOR.rgb

    /** Agent text color - automatically adapts to theme */
    val AGENT_TEXT_COLOR: Color
        get() = provider.getTextForeground()

    @Deprecated("Use AGENT_TEXT_COLOR instead", ReplaceWith("AGENT_TEXT_COLOR"))
    val AGENT_TEXT_DARK_RGB: Int
        get() = AGENT_TEXT_COLOR.rgb

    @Deprecated("Use AGENT_TEXT_COLOR instead", ReplaceWith("AGENT_TEXT_COLOR"))
    val AGENT_TEXT_LIGHT_RGB: Int
        get() = AGENT_TEXT_COLOR.rgb

    /** Error text color as JBColor */
    val ERROR_COLOR: Color
        get() = provider.getErrorForeground()

    @Deprecated("Use ERROR_COLOR instead", ReplaceWith("ERROR_COLOR.rgb"))
    val ERROR_RGB: Int
        get() = ERROR_COLOR.rgb

    /** Card/panel background color - automatically adapts to theme */
    val CARD_BACKGROUND_COLOR: Color
        get() = JBColor.PanelBackground

    @Deprecated("Use CARD_BACKGROUND_COLOR instead", ReplaceWith("CARD_BACKGROUND_COLOR"))
    val CARD_BACKGROUND_DARK_RGB: Int
        get() = CARD_BACKGROUND_COLOR.rgb

    @Deprecated("Use CARD_BACKGROUND_COLOR instead", ReplaceWith("CARD_BACKGROUND_COLOR"))
    val CARD_BACKGROUND_LIGHT_RGB: Int
        get() = CARD_BACKGROUND_COLOR.rgb

    /** Card header background color */
    val CARD_HEADER_COLOR: Color
        get() = JBColor.PanelBackground

    @Deprecated("Use CARD_HEADER_COLOR instead", ReplaceWith("CARD_HEADER_COLOR"))
    val CARD_HEADER_DARK_RGB: Int
        get() = CARD_HEADER_COLOR.rgb

    @Deprecated("Use CARD_HEADER_COLOR instead", ReplaceWith("CARD_HEADER_COLOR"))
    val CARD_HEADER_LIGHT_RGB: Int
        get() = CARD_HEADER_COLOR.rgb

    /** Card border color - automatically adapts to theme */
    val CARD_BORDER_COLOR: Color
        get() = JBColor.border()

    @Deprecated("Use CARD_BORDER_COLOR instead", ReplaceWith("CARD_BORDER_COLOR"))
    val CARD_BORDER_DARK_RGB: Int
        get() = CARD_BORDER_COLOR.rgb

    @Deprecated("Use CARD_BORDER_COLOR instead", ReplaceWith("CARD_BORDER_COLOR"))
    val CARD_BORDER_LIGHT_RGB: Int
        get() = CARD_BORDER_COLOR.rgb

    /** Chevron/arrow muted color */
    val MUTED_CHEVRON_COLOR: Color
        get() = JBColor.GRAY

    @Deprecated("Use MUTED_CHEVRON_COLOR instead", ReplaceWith("MUTED_CHEVRON_COLOR"))
    val MUTED_CHEVRON_DARK_RGB: Int
        get() = MUTED_CHEVRON_COLOR.rgb

    @Deprecated("Use MUTED_CHEVRON_COLOR instead", ReplaceWith("MUTED_CHEVRON_COLOR"))
    val MUTED_CHEVRON_LIGHT_RGB: Int
        get() = MUTED_CHEVRON_COLOR.rgb

    /** Tool title color - automatically adapts to theme */
    val TOOL_TITLE_COLOR: Color
        get() = provider.getTextForeground()

    @Deprecated("Use TOOL_TITLE_COLOR instead", ReplaceWith("TOOL_TITLE_COLOR"))
    val TOOL_TITLE_DARK_RGB: Int
        get() = TOOL_TITLE_COLOR.rgb

    @Deprecated("Use TOOL_TITLE_COLOR instead", ReplaceWith("TOOL_TITLE_COLOR"))
    val TOOL_TITLE_LIGHT_RGB: Int
        get() = TOOL_TITLE_COLOR.rgb
}
