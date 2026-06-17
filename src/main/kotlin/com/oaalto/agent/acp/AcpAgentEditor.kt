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
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBTextArea
import com.intellij.util.ui.JBUI
import com.oaalto.agent.AgentVirtualFile
import com.oaalto.agent.acp.auth.AuthPromptResult
import com.oaalto.agent.acp.auth.AuthPromptUi
import com.oaalto.agent.acp.filesystem.SessionScopeResolver
import com.oaalto.agent.acp.permission.PermissionPromptUi
import com.oaalto.agent.acp.ui.AuthPromptPanel
import com.oaalto.agent.acp.ui.PermissionPromptPanel
import com.oaalto.agent.acp.ui.PromptInputBar
import com.oaalto.agent.acp.ui.SessionPickerDialog
import com.oaalto.agent.acp.ui.ShellPaneHost
import com.oaalto.agent.settings.AgentSettingsState
import com.oaalto.agent.worktree.AgentWorktreeStateService
import com.oaalto.agent.worktree.resume.LaunchResumePlan
import com.oaalto.agent.worktree.resume.SessionSummary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.awt.BorderLayout
import java.awt.Font
import java.beans.PropertyChangeListener
import java.beans.PropertyChangeSupport
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.SwingUtilities
import kotlin.coroutines.resume

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
    private val permissionPromptPanel = PermissionPromptPanel()
    private val authPromptPanel = AuthPromptPanel()
    private val shellPaneHost = ShellPaneHost(project, this)
    private val promptInputBar =
        PromptInputBar { text ->
            coroutineScope.launch {
                runCatching { sessionController.prompt(text) }.onFailure { throwable ->
                    logger.warn("Failed to send ACP prompt", throwable)
                    appendTranscriptLine(
                        TranscriptRenderer.formatError(
                            throwable.message ?: throwable.javaClass.simpleName,
                        ),
                    )
                }
            }
        }
    private val rootPanel = JPanel(BorderLayout())
    private val sessionListener =
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
        }
    private val permissionPromptUi =
        PermissionPromptUi { title, options ->
            val deferred =
                onEdtAsync {
                    permissionPromptPanel.suspendForPrompt(title, options)
                }
            deferred.await()
        }
    private val authPromptUi =
        object : AuthPromptUi {
            override suspend fun promptApiKey(
                methodName: String,
                description: String?,
            ): AuthPromptResult {
                val deferred =
                    onEdtAsync {
                        authPromptPanel.suspendForApiKey(methodName, description)
                    }
                return deferred.await()
            }

            override suspend fun promptOAuthLink(
                methodName: String,
                description: String?,
                link: String,
            ): AuthPromptResult {
                val deferred =
                    onEdtAsync {
                        authPromptPanel.suspendForOAuth(methodName, description, link)
                    }
                return deferred.await()
            }

            override suspend fun waitForTerminalAuthCompletion(
                methodName: String,
                description: String?,
            ): AuthPromptResult {
                val deferred =
                    onEdtAsync {
                        authPromptPanel.suspendForTerminalAuth(methodName, description)
                    }
                return deferred.await()
            }
        }
    private val sessionController: AcpSessionController = sessionControllerFactory(sessionListener)

    init {
        rootPanel.add(
            AcpEditorLayout.buildRootPanel(
                transcriptArea = transcriptArea,
                permissionPromptPanel = permissionPromptPanel,
                authPromptPanel = authPromptPanel,
                promptInputBar = promptInputBar,
                shellPaneHost = shellPaneHost,
            ),
            BorderLayout.CENTER,
        )
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
        permissionPromptPanel.cancelPending()
        authPromptPanel.cancelPending()
        coroutineScope.launch {
            runCatching { sessionController.cancelPrompt() }
        }
        sessionController.dispose()
        coroutineScope.cancel()
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
                        appendTranscriptLine(
                            TranscriptRenderer.formatError(
                                throwable.message ?: "Failed to build launch command.",
                            ),
                        )
                        runOnEdt { promptInputBar.setEnabled(false) }
                        return@launch
                    }

            runCatching {
                val scopeRoot =
                    SessionScopeResolver.hostScopeRoot(
                        sessionWorkingDirectory = launchPlan.sessionWorkingDirectory,
                        projectBasePath = project.basePath,
                        workingDirectoryOverride = file.launchContext.workingDirectoryOverride,
                    )
                val editorContext =
                    AcpEditorContext(
                        project = project,
                        configurationId = file.configurationId,
                        launchContext = file.launchContext,
                        scopeRoot = scopeRoot,
                        listener = sessionListener,
                        shellPaneHost = shellPaneHost,
                        permissionPromptUi = permissionPromptUi,
                        authPromptUi = authPromptUi,
                    )
                sessionController.connect(launchPlan, editorContext)
                openSessionFromResumePlan(launchPlan.sessionWorkingDirectory)
                appendTranscriptLine("Connected to ${configuration.name}.")
                runOnEdt {
                    promptInputBar.setEnabled(true)
                    promptInputBar.requestFocus()
                }
            }.onFailure { throwable ->
                logger.warn("Failed to start ACP session", throwable)
                appendTranscriptLine(
                    TranscriptRenderer.formatError(throwable.message ?: throwable.javaClass.simpleName),
                )
                runOnEdt { promptInputBar.setEnabled(false) }
            }
        }
    }

    private fun appendTranscriptText(text: String) {
        runOnEdt {
            transcriptArea.append(TranscriptRenderer.normalizeTranscriptText(text))
            transcriptArea.caretPosition = transcriptArea.document.length
        }
    }

    private suspend fun openSessionFromResumePlan(sessionWorkingDirectory: String) {
        when (val plan = file.launchContext.resumePlan) {
            is LaunchResumePlan.AcpLoad -> {
                runCatching {
                    sessionController.loadSession(plan.sessionId)
                    persistBoundSessionId(plan.sessionId)
                }.onFailure { throwable ->
                    logger.warn("ACP session load failed for ${plan.sessionId}", throwable)
                    appendTranscriptLine(
                        "Stored session is unavailable (${throwable.message ?: "load failed"}). " +
                            "Choose a session to resume or start fresh.",
                    )
                    pickSessionOrStartFresh(sessionWorkingDirectory)
                }
            }
            is LaunchResumePlan.AcpPickSession -> {
                if (plan.candidates.isNotEmpty()) {
                    pickSessionFromCandidates(plan.candidates)
                } else {
                    appendTranscriptLine(
                        "No stored ACP session for this worktree. Choose a session to resume or start fresh.",
                    )
                    pickSessionOrStartFresh(sessionWorkingDirectory)
                }
            }
            LaunchResumePlan.AcpNewSession, null -> {
                sessionController.newSession()
                persistCurrentSessionId()
            }
            is LaunchResumePlan.Pty -> error("PTY resume plan is not valid for ACP editor")
        }
    }

    private suspend fun pickSessionOrStartFresh(sessionWorkingDirectory: String) {
        val sessions =
            runCatching {
                sessionController.listSessions(sessionWorkingDirectory)
            }.getOrElse { throwable ->
                logger.warn("ACP listSessions failed", throwable)
                appendTranscriptLine(
                    TranscriptRenderer.formatError(
                        throwable.message ?: "Failed to list sessions",
                    ),
                )
                emptyList()
            }
        pickSessionFromCandidates(sessions)
    }

    private suspend fun pickSessionFromCandidates(candidates: List<SessionSummary>) {
        if (candidates.isEmpty()) {
            sessionController.newSession()
            persistCurrentSessionId()
            appendTranscriptLine("Started a new ACP session.")
            return
        }
        val selectedId =
            onEdtAsync {
                SessionPickerDialog.show(project, candidates)
            }
        if (selectedId == null) {
            sessionController.newSession()
            persistCurrentSessionId()
            appendTranscriptLine("Started a new ACP session.")
            return
        }
        runCatching {
            sessionController.loadSession(selectedId)
            persistBoundSessionId(selectedId)
            appendTranscriptLine("Resumed session $selectedId.")
        }.onFailure { throwable ->
            logger.warn("ACP session load failed for picker selection $selectedId", throwable)
            appendTranscriptLine(
                TranscriptRenderer.formatError(
                    throwable.message ?: "Failed to load selected session",
                ),
            )
            sessionController.newSession()
            persistCurrentSessionId()
        }
    }

    private fun persistCurrentSessionId() {
        val sessionId = sessionController.currentSessionId() ?: return
        persistBoundSessionId(sessionId)
    }

    private fun persistBoundSessionId(sessionId: String) {
        val recordId = file.launchContext.worktreeId ?: return
        AgentWorktreeStateService.getInstance().setAcpSessionId(recordId, sessionId)
    }

    private fun appendTranscriptLine(line: String) {
        runOnEdt {
            if (transcriptArea.text.isNotEmpty()) {
                transcriptArea.append("\n")
            }
            transcriptArea.append(TranscriptRenderer.normalizeTranscriptText(line))
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

    private suspend fun <T> onEdtAsync(block: () -> T): T =
        suspendCancellableCoroutine { continuation ->
            ApplicationManager.getApplication().invokeLater({
                if (continuation.isActive) {
                    continuation.resume(block())
                }
            }, ModalityState.any())
        }

    companion object {
        private val logger = Logger.getInstance(AcpAgentEditor::class.java)

        private fun defaultSessionController(listener: AcpSessionListener): AcpSessionController =
            AcpSessionControllerImpl(listener)
    }
}
