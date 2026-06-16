package com.oaalto.agent.acp

import com.intellij.codeHighlighting.BackgroundEditorHighlighter
import com.intellij.ide.structureView.StructureViewBuilder
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorLocation
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Splitter
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.util.ui.JBUI
import com.oaalto.agent.AgentVirtualFile
import com.oaalto.agent.acp.ui.PromptInputBar
import com.oaalto.agent.acp.ui.ShellPaneHost
import com.oaalto.agent.settings.AgentSettingsState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.awt.BorderLayout
import java.awt.Font
import java.beans.PropertyChangeListener
import java.beans.PropertyChangeSupport
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.SwingUtilities

class AcpAgentEditor(
    private val project: Project,
    private val file: AgentVirtualFile,
    private val sessionControllerFactory: (AcpSessionListener) -> AcpSessionController = ::defaultSessionController,
) : FileEditor,
    Disposable {
    private val propertyChangeSupport = PropertyChangeSupport(this)
    private val userData = UserDataHolderBase()
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val transcriptArea =
        JBTextArea().apply {
            isEditable = false
            lineWrap = true
            wrapStyleWord = true
            font = Font(Font.MONOSPACED, Font.PLAIN, JBUI.scale(12))
            background = JBColor.PanelBackground
            foreground = JBColor.foreground()
            border = JBUI.Borders.empty(8)
        }
    private val shellPaneHost = ShellPaneHost()
    private val promptInputBar =
        PromptInputBar { text ->
            coroutineScope.launch {
                runCatching { sessionController.prompt(text) }.onFailure { throwable ->
                    logger.warn("Failed to send ACP prompt", throwable)
                    appendTranscriptLine(TranscriptRenderer.formatError(throwable.message ?: throwable.javaClass.simpleName))
                }
            }
        }
    private val rootPanel = JPanel(BorderLayout())
    private val sessionController: AcpSessionController =
        sessionControllerFactory(
            object : AcpSessionListener {
                override fun onTranscriptAppend(text: String) {
                    appendTranscriptText(text)
                }

                override fun onTranscriptLine(line: String) {
                    appendTranscriptLine(line)
                }

                override fun onError(message: String) {
                    appendTranscriptLine(TranscriptRenderer.formatError(message))
                    runOnEdt { promptInputBar.setEnabled(false) }
                }
            },
        )

    init {
        layoutEditor()
        promptInputBar.setEnabled(false)
        startSession()
    }

    override fun getComponent(): JComponent = rootPanel

    override fun getPreferredFocusedComponent(): JComponent = promptInputBar.component

    override fun getName(): String = "Agent CLI"

    override fun getFile(): VirtualFile = file

    override fun setState(state: FileEditorState) = Unit

    override fun isModified(): Boolean = false

    override fun isValid(): Boolean = file.isValid && !project.isDisposed

    override fun selectNotify() = Unit

    override fun deselectNotify() = Unit

    override fun addPropertyChangeListener(listener: PropertyChangeListener) {
        propertyChangeSupport.addPropertyChangeListener(listener)
    }

    override fun removePropertyChangeListener(listener: PropertyChangeListener) {
        propertyChangeSupport.removePropertyChangeListener(listener)
    }

    override fun getBackgroundHighlighter(): BackgroundEditorHighlighter? = null

    override fun getCurrentLocation(): FileEditorLocation? = null

    override fun getStructureViewBuilder(): StructureViewBuilder? = null

    override fun <T : Any?> getUserData(key: Key<T>): T? = userData.getUserData(key)

    override fun <T : Any?> putUserData(
        key: Key<T>,
        value: T?,
    ) {
        userData.putUserData(key, value)
    }

    override fun dispose() {
        coroutineScope.launch {
            runCatching { sessionController.cancelPrompt() }
        }
        sessionController.dispose()
        coroutineScope.cancel()
    }

    private fun layoutEditor() {
        val transcriptScroll =
            JBScrollPane(transcriptArea).apply {
                border = JBUI.Borders.empty()
            }
        val bottomSplitter =
            Splitter(true, 0.2f).apply {
                firstComponent = promptInputBar.component
                secondComponent = shellPaneHost.component
            }
        val mainSplitter =
            Splitter(true, 0.72f).apply {
                firstComponent = transcriptScroll
                secondComponent = bottomSplitter
            }
        rootPanel.add(mainSplitter, BorderLayout.CENTER)
        rootPanel.border = JBUI.Borders.empty()
    }

    private fun startSession() {
        val configuration = AgentSettingsState.getInstance().getConfigurationById(file.configurationId)
        if (configuration == null) {
            appendTranscriptLine(TranscriptRenderer.formatError("Agent configuration was removed or is unavailable."))
            promptInputBar.setEnabled(false)
            return
        }

        coroutineScope.launch {
            val launchPlan =
                AcpProcessLauncher
                    .buildLaunchPlan(
                        projectContext = project.toAgentProjectContext(),
                        configuration = configuration,
                        launchContext = file.launchContext,
                    ).getOrElse { throwable ->
                        appendTranscriptLine(TranscriptRenderer.formatError(throwable.message ?: "Failed to build launch command."))
                        runOnEdt { promptInputBar.setEnabled(false) }
                        return@launch
                    }

            runCatching {
                sessionController.connect(launchPlan)
                sessionController.newSession()
                appendTranscriptLine("Connected to ${configuration.name}.")
                runOnEdt {
                    promptInputBar.setEnabled(true)
                    promptInputBar.requestFocus()
                }
            }.onFailure { throwable ->
                logger.warn("Failed to start ACP session", throwable)
                appendTranscriptLine(TranscriptRenderer.formatError(throwable.message ?: throwable.javaClass.simpleName))
                runOnEdt { promptInputBar.setEnabled(false) }
            }
        }
    }

    private fun appendTranscriptText(text: String) {
        runOnEdt {
            transcriptArea.append(text)
            transcriptArea.caretPosition = transcriptArea.document.length
        }
    }

    private fun appendTranscriptLine(line: String) {
        runOnEdt {
            if (transcriptArea.text.isNotEmpty()) {
                transcriptArea.append("\n")
            }
            transcriptArea.append(line)
            transcriptArea.caretPosition = transcriptArea.document.length
        }
    }

    private fun runOnEdt(action: () -> Unit) {
        if (SwingUtilities.isEventDispatchThread()) {
            action()
        } else {
            ApplicationManager.getApplication().invokeLater(action, ModalityState.any())
        }
    }

    companion object {
        private val logger = Logger.getInstance(AcpAgentEditor::class.java)

        private fun defaultSessionController(listener: AcpSessionListener): AcpSessionController = AcpSessionControllerImpl(listener)
    }
}
