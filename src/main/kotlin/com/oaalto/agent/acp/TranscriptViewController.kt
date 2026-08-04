package com.oaalto.agent.acp

import com.intellij.openapi.project.Project
import com.oaalto.agent.AgentCliSessionContext
import javax.swing.JComponent
import javax.swing.SwingUtilities

/** EDT-safe bridge between [TranscriptModel] and [TranscriptPanel]. */
internal class TranscriptViewController(
    project: Project,
    private val runOnEdt: ((() -> Unit) -> Unit)? = null,
    codeBlockViewFactory: TranscriptCodeBlockViewFactory = EditorFactoryTranscriptCodeBlockViewFactory(project),
    logContextProvider: () -> AgentCliSessionContext? = { null },
) {
    private val model = TranscriptModel()
    var onBlocksChanged: (() -> Unit)? = null
    var errorMessageDecorator: ((String) -> String)? = null
    private val transcriptPanel: TranscriptPanel =
        TranscriptPanel.create(
            project = project,
            onToolToggle = { toolCallId ->
                applyOnEdt {
                    model.toggleToolExpansion(toolCallId)
                    transcriptPanel.sync(model.blocks())
                }
            },
            codeBlockViewFactory = codeBlockViewFactory,
            logContextProvider = logContextProvider,
        )

    val component: JComponent get() = transcriptPanel.component

    fun apply(update: StructuredUpdate) {
        applyOnEdt {
            model.apply(update)
            transcriptPanel.sync(model.blocks())
            onBlocksChanged?.invoke()
        }
    }

    fun appendPlainLine(
        line: String,
        isUserPrompt: Boolean = line.startsWith("> "),
    ) {
        apply(StructuredUpdate.AppendPlainLine(line, isUserPrompt))
    }

    fun appendError(message: String) {
        val displayMessage = errorMessageDecorator?.invoke(message) ?: message
        apply(StructuredUpdate.AppendError(displayMessage))
    }

    fun restorePlainLines(content: String) {
        if (content.isEmpty()) return
        content.lineSequence().forEach { line ->
            val isUserPrompt = line.startsWith("> ")
            appendPlainLine(line, isUserPrompt = isUserPrompt)
        }
    }

    fun finalizeAgentStream() {
        apply(StructuredUpdate.FinalizeAgentStream)
    }

    fun dispose() {
        val action = { transcriptPanel.disposeAll() }
        if (SwingUtilities.isEventDispatchThread()) {
            action()
        } else {
            SwingUtilities.invokeAndWait(action)
        }
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
