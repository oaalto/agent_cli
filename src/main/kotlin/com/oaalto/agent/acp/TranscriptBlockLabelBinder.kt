package com.oaalto.agent.acp

import com.intellij.openapi.components.serviceOrNull
import com.intellij.ui.JBColor
import javax.swing.JTextPane

@Suppress("CyclomaticComplexMethod")
internal fun JTextPane.bindTranscriptBlock(block: TranscriptBlock) {
    val colorProvider = serviceOrNull<TranscriptColorProvider>()
    when (block) {
        is TranscriptBlock.UserEcho -> bindUserEcho(block, colorProvider)
        is TranscriptBlock.Thought -> bindThought(block, colorProvider)
        is TranscriptBlock.StreamingAgentText -> bindStreamingAgent(block, colorProvider)
        is TranscriptBlock.FinalAgentText -> bindFinalAgent(block, colorProvider)
        is TranscriptBlock.PlainLine -> bindPlainLine(block, colorProvider)
        is TranscriptBlock.ErrorLine -> bindErrorLine(block, colorProvider)
        is TranscriptBlock.AuthFailureLine -> bindAuthFailureLine(block, colorProvider)
        is TranscriptBlock.ToolCallBlock -> Unit
        is TranscriptBlock.PlanBlock -> Unit // PlanBlock is handled by PlanPanel component
    }
}

private fun JTextPane.bindUserEcho(
    block: TranscriptBlock.UserEcho,
    provider: TranscriptColorProvider?,
) {
    foreground = provider?.getUserEchoColor() ?: JBColor.BLUE
    text = "> ${block.text}"
}

private fun JTextPane.bindThought(
    block: TranscriptBlock.Thought,
    provider: TranscriptColorProvider?,
) {
    foreground = provider?.getThoughtColor() ?: JBColor.GRAY
    text = "[thought] ${block.text}"
}

private fun JTextPane.bindStreamingAgent(
    block: TranscriptBlock.StreamingAgentText,
    provider: TranscriptColorProvider?,
) {
    foreground = provider?.getTextForeground() ?: JBColor.foreground()
    text = normalizeAgentFences(block.text) + TranscriptStreamingCursor.CURSOR_CHAR
}

private fun JTextPane.bindFinalAgent(
    block: TranscriptBlock.FinalAgentText,
    provider: TranscriptColorProvider?,
) {
    foreground = provider?.getTextForeground() ?: JBColor.foreground()
    text = block.text
}

private fun JTextPane.bindPlainLine(
    block: TranscriptBlock.PlainLine,
    provider: TranscriptColorProvider?,
) {
    foreground =
        if (block.isUserPrompt) {
            provider?.getUserEchoColor() ?: JBColor.BLUE
        } else {
            provider?.getTextForeground() ?: JBColor.foreground()
        }
    text = block.text
}

private fun JTextPane.bindErrorLine(
    block: TranscriptBlock.ErrorLine,
    provider: TranscriptColorProvider?,
) {
    foreground = provider?.getErrorForeground() ?: JBColor.RED
    text = TranscriptRenderer.formatError(block.message)
}

private fun JTextPane.bindAuthFailureLine(
    block: TranscriptBlock.AuthFailureLine,
    provider: TranscriptColorProvider?,
) {
    foreground = provider?.getErrorForeground() ?: JBColor.RED
    text = TranscriptRenderer.formatAuthFailure(block.message)
}
