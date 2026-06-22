package com.oaalto.agent.acp

import com.intellij.ui.JBColor
import java.awt.Color
import javax.swing.JTextPane

@Suppress("CyclomaticComplexMethod")
internal fun JTextPane.bindTranscriptBlock(block: TranscriptBlock) {
    when (block) {
        is TranscriptBlock.UserEcho -> bindUserEcho(block)
        is TranscriptBlock.Thought -> bindThought(block)
        is TranscriptBlock.StreamingAgentText -> bindStreamingAgent(block)
        is TranscriptBlock.FinalAgentText -> bindFinalAgent(block)
        is TranscriptBlock.PlainLine -> bindPlainLine(block)
        is TranscriptBlock.ErrorLine -> bindErrorLine(block)
        is TranscriptBlock.AuthFailureLine -> bindAuthFailureLine(block)
        is TranscriptBlock.ToolCallBlock -> Unit
        is TranscriptBlock.PlanBlock -> Unit // PlanBlock is handled by PlanPanel component
    }
}

private fun JTextPane.bindUserEcho(block: TranscriptBlock.UserEcho) {
    foreground = Color(TranscriptPalette.USER_ECHO_RGB)
    text = "> ${block.text}"
}

private fun JTextPane.bindThought(block: TranscriptBlock.Thought) {
    foreground = Color(TranscriptPalette.THOUGHT_RGB)
    text = "[thought] ${block.text}"
}

private fun JTextPane.bindStreamingAgent(block: TranscriptBlock.StreamingAgentText) {
    foreground =
        JBColor(
            Color(TranscriptPalette.AGENT_TEXT_DARK_RGB),
            Color(TranscriptPalette.AGENT_TEXT_LIGHT_RGB),
        )
    text = block.text + TranscriptStreamingCursor.CURSOR_CHAR
}

private fun JTextPane.bindFinalAgent(block: TranscriptBlock.FinalAgentText) {
    foreground =
        JBColor(
            Color(TranscriptPalette.AGENT_TEXT_DARK_RGB),
            Color(TranscriptPalette.AGENT_TEXT_LIGHT_RGB),
        )
    text = block.text
}

private fun JTextPane.bindPlainLine(block: TranscriptBlock.PlainLine) {
    foreground =
        if (block.isUserPrompt) {
            Color(TranscriptPalette.USER_ECHO_RGB)
        } else {
            JBColor(
                Color(TranscriptPalette.AGENT_TEXT_DARK_RGB),
                Color(TranscriptPalette.AGENT_TEXT_LIGHT_RGB),
            )
        }
    text = block.text
}

private fun JTextPane.bindErrorLine(block: TranscriptBlock.ErrorLine) {
    foreground = Color(TranscriptPalette.ERROR_RGB)
    text = TranscriptRenderer.formatError(block.message)
}

private fun JTextPane.bindAuthFailureLine(block: TranscriptBlock.AuthFailureLine) {
    foreground = Color(TranscriptPalette.ERROR_RGB)
    text = TranscriptRenderer.formatAuthFailure(block.message)
}
