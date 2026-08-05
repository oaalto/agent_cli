package com.oaalto.agent.acp

import com.intellij.codeHighlighting.BackgroundEditorHighlighter
import com.intellij.ide.structureView.StructureViewBuilder
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorLocation
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import com.oaalto.agent.AgentCliLog
import com.oaalto.agent.AgentCliSessionContext
import com.oaalto.agent.AgentVirtualFile
import com.oaalto.agent.acp.auth.AuthPromptResult
import com.oaalto.agent.acp.auth.AuthPromptUi
import com.oaalto.agent.acp.filesystem.SessionScopeResolver
import com.oaalto.agent.acp.permission.PermissionPromptUi
import com.oaalto.agent.acp.ui.AcpUiMetrics
import com.oaalto.agent.acp.ui.AuthPromptPanel
import com.oaalto.agent.acp.ui.PermissionPromptPanel
import com.oaalto.agent.acp.ui.PromptInputBar
import com.oaalto.agent.acp.ui.SessionPickerAdapter
import com.oaalto.agent.acp.ui.ShellPaneHost
import com.oaalto.agent.settings.AgentSettingsState
import com.oaalto.agent.settings.LaunchMode
import com.oaalto.agent.worktree.WorktreeSessionBinderImpl
import com.oaalto.agent.worktree.resume.LaunchResumePlan
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.awt.BorderLayout
import java.beans.PropertyChangeListener
import java.beans.PropertyChangeSupport
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.SwingUtilities
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.resume

class AcpAgentEditor(
    private val project: Project,
    private val file: AgentVirtualFile,
    private val sessionControllerFactory: (AcpSessionListener) -> AcpSessionController = { listener ->
        AcpSessionControllerImpl(
            listener = listener,
            sessionOperationsFactory = AcpDefaultClientSessionOperationsFactory(project),
        )
    },
) : FileEditor,
    Disposable {
    private val propertyChangeSupport = PropertyChangeSupport(this)
    private val userData = UserDataHolderBase()
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val transcriptViewController =
        TranscriptViewController(
            project,
            ::runOnEdt,
            logContextProvider = { logContext() },
        )

    private fun snapshotBlocksOnEdt(): List<TranscriptBlock> {
        if (SwingUtilities.isEventDispatchThread()) {
            return transcriptViewController.blocksForTest()
        }
        val holder = arrayOf(emptyList<TranscriptBlock>())
        ApplicationManager.getApplication().invokeAndWait {
            holder[0] = transcriptViewController.blocksForTest()
        }
        return holder[0]
    }

    private val sessionTranscript =
        SessionTranscriptCoordinator(
            projectBasePath = project.basePath,
            callbacks =
                SessionTranscriptCallbacks(
                    blocksProvider = ::snapshotBlocksOnEdt,
                    restoreLines = { content -> transcriptViewController.restorePlainLines(content) },
                    appendPlainLine = { line -> transcriptViewController.appendPlainLine(line) },
                    logWarn = { message, throwable, context -> log.warn(message, throwable, context) },
                    logContext = { logContext() },
                ),
        )
    private val permissionPromptPanel = PermissionPromptPanel()
    private val authPromptPanel = AuthPromptPanel()
    private val shellPaneHost =
        ShellPaneHost(
            project = project,
            parentDisposable = this,
            logContextProvider = { logContext() },
        )
    private val promptInputBar =
        PromptInputBar { text ->
            applyPolicy(TranscriptFinalizePolicy.onPromptStarting())
            transcriptViewController.appendPlainLine("")
            transcriptViewController.appendPlainLine("> $text", isUserPrompt = true)
            transcriptViewController.appendPlainLine("")
            coroutineScope.launch {
                runCatching { sessionController.prompt(text) }.onFailure { throwable ->
                    rethrowIfCancellation(throwable)
                    log.warn("Failed to send ACP prompt", throwable, logContext())
                    applyPolicy(TranscriptFinalizePolicy.onPromptFailed())
                    transcriptViewController.appendError(
                        throwable.message ?: throwable.javaClass.simpleName,
                    )
                }
            }
        }
    private val transcriptFooter = TranscriptFooter()
    private val rootPanel = JPanel(BorderLayout())
    private val sessionListener =
        object : AcpSessionListener {
            override fun onStructuredUpdate(update: StructuredUpdate) {
                when (update) {
                    is StructuredUpdate.AvailableCommands ->
                        runOnEdt { promptInputBar.setAvailableCommands(update.commands) }
                    else -> transcriptViewController.apply(update)
                }
            }

            override fun onError(message: String) {
                transcriptViewController.appendError(message)
                runOnEdt { promptInputBar.setEnabled(false) }
            }

            override fun onUsageUpdate(usage: AccumulatedUsage) {
                runOnEdt {
                    transcriptFooter.updateUsage(usage.totalUsed, usage.contextSize, usage.totalCost)
                }
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
    private val worktreeBinder = WorktreeSessionBinderImpl()
    private var activeSessionId: String? = null

    init {
        transcriptViewController.errorMessageDecorator = sessionTranscript::decorateError
        transcriptViewController.onBlocksChanged = sessionTranscript::onBlocksChanged
        rootPanel.add(
            AcpEditorLayout.buildRootPanel(
                EditorLayoutComponents(
                    transcriptArea =
                        transcriptViewController.component.apply {
                            background = JBColor.PanelBackground
                            border = JBUI.Borders.empty(AcpUiMetrics.HORIZONTAL_INSET)
                        },
                    permissionPromptPanel = permissionPromptPanel,
                    authPromptPanel = authPromptPanel,
                    promptInputBar = promptInputBar,
                    shellPaneHost = shellPaneHost,
                    transcriptFooter = transcriptFooter,
                ),
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
        sessionTranscript.dispose()
        transcriptViewController.dispose()
        coroutineScope.launch {
            runCatching { sessionController.cancelPrompt() }
        }
        sessionController.dispose()
        coroutineScope.cancel()
    }

    private fun startSession() {
        val configuration = AgentSettingsState.getInstance().getConfigurationById(file.configurationId)
        if (configuration == null) {
            transcriptViewController.appendError("Agent configuration was removed or is unavailable.")
            promptInputBar.setEnabled(false)
            return
        }

        coroutineScope.launch {
            launchAndConnect(configuration)
        }
    }

    private suspend fun launchAndConnect(configuration: AgentSettingsState.AgentCliConfiguration) {
        val typedConfig = configuration
        val launchPlan =
            AcpProcessLauncher
                .buildLaunchPlan(
                    projectContext = project.toAgentProjectContext(),
                    configuration = typedConfig,
                    launchContext = file.launchContext,
                ).getOrElse { throwable ->
                    transcriptViewController.appendError(
                        throwable.message ?: "Failed to build launch command.",
                    )
                    runOnEdt { promptInputBar.setEnabled(false) }
                    return
                }

        transcriptViewController.appendPlainLine("Connecting to ${typedConfig.name}...")
        runCatching {
            applyStartResult(
                sessionController.start(
                    AcpSessionStartRequest(
                        launchPlan = launchPlan,
                        editorContext = buildEditorContext(launchPlan),
                        resumePlan = file.launchContext.resumePlan ?: LaunchResumePlan.AcpNewSession,
                        sessionPicker = SessionPickerAdapter(project),
                    ),
                ),
                typedConfig.name,
            )
        }.onFailure { throwable ->
            rethrowIfCancellation(throwable)
            log.warn("Failed to start ACP session", throwable, logContext())
            transcriptViewController.appendError(
                throwable.message ?: throwable.javaClass.simpleName,
            )
            runOnEdt { promptInputBar.setEnabled(false) }
        }
    }

    private fun buildEditorContext(launchPlan: AcpLaunchPlan): AcpEditorContext {
        val scopeRoot =
            SessionScopeResolver.hostScopeRoot(
                sessionWorkingDirectory = launchPlan.sessionWorkingDirectory,
                projectBasePath = project.basePath,
                workingDirectoryOverride = file.launchContext.workingDirectoryOverride,
            )
        return AcpEditorContext(
            project = project,
            configurationId = file.configurationId,
            launchContext = file.launchContext,
            scopeRoot = scopeRoot,
            listener = sessionListener,
            shellPaneHost = shellPaneHost,
            permissionPromptUi = permissionPromptUi,
            authPromptUi = authPromptUi,
        )
    }

    private fun applyStartResult(
        result: AcpSessionStartResult,
        configurationName: String,
    ) {
        activeSessionId = result.sessionId
        result.sessionId?.let { sessionId ->
            if (result.restoreTranscript) {
                sessionTranscript.restoreIfPresent(sessionId)
            }
            sessionTranscript.bindSession(sessionId)
        }
        persistWorktreeSessionId(result.sessionId)
        if (result.statusMessage.isNotBlank()) {
            transcriptViewController.appendPlainLine(result.statusMessage)
        }
        transcriptViewController.appendPlainLine("Connected to $configurationName.")
        runOnEdt {
            promptInputBar.setEnabled(true)
            promptInputBar.requestFocus()
        }
    }

    private fun persistWorktreeSessionId(sessionId: String?) {
        if (sessionId == null) return
        file.launchContext.worktreeId?.let { recordId ->
            worktreeBinder.persistSessionId(recordId, sessionId)
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

    fun buildDiagnosticsClipboardText(): String = sessionTranscript.clipboardText()

    private fun applyPolicy(updates: List<StructuredUpdate>) {
        updates.forEach(transcriptViewController::apply)
    }

    private fun logContext(sessionId: String? = activeSessionId): AgentCliSessionContext =
        AgentCliSessionContext(
            configId = file.configurationId,
            sessionId = sessionId,
            launchMode = LaunchMode.ACP_CLIENT,
            worktreePath = file.launchContext.workingDirectoryOverride,
        )

    companion object {
        private val log = AgentCliLog.getInstance(AcpAgentEditor::class.java)

        private fun rethrowIfCancellation(throwable: Throwable) {
            if (throwable is CancellationException) throw throwable
        }
    }
}
