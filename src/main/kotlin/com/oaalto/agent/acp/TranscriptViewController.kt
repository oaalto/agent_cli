package com.oaalto.agent.acp

import javax.swing.JComponent
import javax.swing.SwingUtilities

/** EDT-safe bridge between [TranscriptModel] and [TranscriptPanel]. */
internal class TranscriptViewController(
    private val runOnEdt: ((() -> Unit) -> Unit)? = null,
) {
    private val model = TranscriptModel()
    private val transcriptPanel: TranscriptPanel =
        TranscriptPanel { toolCallId ->
            applyOnEdt {
                model.toggleToolExpansion(toolCallId)
                transcriptPanel.sync(model.blocks())
            }
        }

    val component: JComponent get() = transcriptPanel.component

    fun apply(update: StructuredUpdate) {
        applyOnEdt {
            model.apply(update)
            transcriptPanel.sync(model.blocks())
            transcriptPanel.scrollToEndIfAtBottom()
        }
    }

    fun appendPlainLine(
        line: String,
        isUserPrompt: Boolean = line.startsWith("> "),
    ) {
        apply(StructuredUpdate.AppendPlainLine(line, isUserPrompt))
    }

    fun appendError(message: String) {
        apply(StructuredUpdate.AppendError(message))
    }

    fun finalizeAgentStream() {
        apply(StructuredUpdate.FinalizeAgentStream)
    }

    internal fun blocksForTest(): List<TranscriptBlock> = model.blocks()

    private val onEdt: (() -> Unit) -> Unit =
        runOnEdt ?: { action ->
            if (SwingUtilities.isEventDispatchThread()) {
                action()
            } else {
                SwingUtilities.invokeLater(action)
            }
        }

    private fun applyOnEdt(action: () -> Unit) {
        onEdt(action)
    }
}
