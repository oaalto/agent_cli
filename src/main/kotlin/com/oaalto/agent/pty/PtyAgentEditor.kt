package com.oaalto.agent.pty

import com.intellij.codeHighlighting.BackgroundEditorHighlighter
import com.intellij.ide.DataManager
import com.intellij.ide.structureView.StructureViewBuilder
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.ActionUiKind
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorLocation
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.keymap.KeymapUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.util.execution.ParametersListUtil
import com.intellij.util.ui.JBUI
import com.oaalto.agent.AgentCliLog
import com.oaalto.agent.AgentCommandBuilder
import com.oaalto.agent.AgentLaunchContext
import com.oaalto.agent.AgentVirtualFile
import com.oaalto.agent.AgentWslCommandRequest
import com.oaalto.agent.WorkingDirectoryResolver
import com.oaalto.agent.WslPathResolver
import com.oaalto.agent.acp.ui.AcpUiMetrics
import com.oaalto.agent.settings.AgentSettingsState
import com.oaalto.agent.settings.LaunchMode
import com.oaalto.agent.toAgentCliSessionContext
import org.jetbrains.plugins.terminal.DefaultTerminalRunnerFactory
import org.jetbrains.plugins.terminal.ShellStartupOptions
import java.awt.BorderLayout
import java.awt.Component
import java.awt.KeyEventDispatcher
import java.awt.KeyboardFocusManager
import java.awt.event.KeyEvent
import java.beans.PropertyChangeListener
import java.beans.PropertyChangeSupport
import java.nio.file.Files
import java.nio.file.Path
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.SwingUtilities

class PtyAgentEditor(
    private val project: Project,
    private val file: AgentVirtualFile,
) : FileEditor,
    Disposable {
    private val propertyChangeSupport = PropertyChangeSupport(this)
    private val userData = UserDataHolderBase()
    private val rootPanel = JPanel(BorderLayout())
    private var terminalFocusComponent: JComponent? = null
    private val keyEventDispatcher = KeyEventDispatcher { event -> handleEditorNavigationShortcut(event) }

    init {
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(keyEventDispatcher)
        startTerminalSession()
    }

    override fun getComponent(): JComponent = rootPanel

    override fun getPreferredFocusedComponent(): JComponent = terminalFocusComponent ?: rootPanel

    override fun getName(): String = "Agent CLI"

    override fun getFile(): VirtualFile = file

    override fun setState(state: FileEditorState) = Unit

    override fun isModified(): Boolean = false

    override fun isValid(): Boolean = file.isValid

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
        KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(keyEventDispatcher)
    }

    private fun handleEditorNavigationShortcut(event: KeyEvent): Boolean {
        val focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().focusOwner
        val terminalComponent = terminalFocusComponent
        val canHandle =
            event.id == KeyEvent.KEY_PRESSED &&
                !event.isConsumed &&
                !project.isDisposed &&
                focusOwner != null &&
                terminalComponent != null &&
                SwingUtilities.isDescendingFrom(focusOwner, terminalComponent)
        if (!canHandle) {
            return false
        }
        val handled =
            NAVIGATION_ACTION_IDS.any { actionId ->
                KeymapUtil.isEventForAction(event, actionId) &&
                    invokeIdeAction(actionId, focusOwner!!, event)
            }
        if (handled) {
            event.consume()
        }
        return handled
    }

    private fun invokeIdeAction(
        actionId: String,
        focusOwner: Component,
        event: KeyEvent,
    ): Boolean {
        val action = ActionManager.getInstance().getAction(actionId) ?: return false
        val dataContext = DataManager.getInstance().getDataContext(focusOwner)
        val actionEvent =
            AnActionEvent.createEvent(
                action,
                dataContext,
                action.templatePresentation.clone(),
                ActionPlaces.KEYBOARD_SHORTCUT,
                ActionUiKind.NONE,
                event,
            )
        ActionUtil.performAction(action, actionEvent)
        return true
    }

    private fun startTerminalSession() {
        val configuration =
            resolvePtyConfiguration(file.configurationId) {
                logTerminalFailure("Agent configuration was removed or is unavailable.")
            }
                ?: return
        val binaryPath = configuration.binaryPath.trim()
        val startupRequest =
            when {
                binaryPath.isBlank() -> {
                    logTerminalFailure("Agent binary path is empty for configuration '${configuration.name}'.")
                    null
                }
                else -> buildTerminalStartupRequest(configuration, binaryPath)
            }
        if (startupRequest != null) {
            launchTerminalWidget(binaryPath, startupRequest)
        }
    }

    private fun buildTerminalStartupRequest(
        configuration: AgentSettingsState.AgentCliConfiguration,
        binaryPath: String,
    ): TerminalStartupRequest? {
        val parsedArguments = ParametersListUtil.parse(configuration.arguments)
        val launchContext = file.launchContext
        val effectiveArguments =
            buildList {
                addAll(parsedArguments)
                addAll(launchContext.additionalArguments)
            }
        return when (resolvePtyExecutionTarget(configuration.executionTarget)) {
            AgentSettingsState.ExecutionTarget.LOCAL ->
                buildLocalTerminalStartupRequest(
                    configuration = configuration,
                    binaryPath = binaryPath,
                    effectiveArguments = effectiveArguments,
                    launchContext = launchContext,
                )
            AgentSettingsState.ExecutionTarget.WSL ->
                buildWslTerminalStartupRequest(
                    configuration = configuration,
                    binaryPath = binaryPath,
                    effectiveArguments = effectiveArguments,
                    launchContext = launchContext,
                )
        }
    }

    private fun buildLocalTerminalStartupRequest(
        configuration: AgentSettingsState.AgentCliConfiguration,
        binaryPath: String,
        effectiveArguments: List<String>,
        launchContext: AgentLaunchContext,
    ): TerminalStartupRequest? =
        when {
            binaryPath.contains("/") && !Files.isExecutable(Path.of(binaryPath)) -> {
                logTerminalFailure("Agent binary is not executable:\n$binaryPath")
                null
            }
            else -> {
                val workingDirectory =
                    WorkingDirectoryResolver.resolve(
                        configuredWorkingDirectory = configuration.workingDirectory,
                        overrideWorkingDirectory = launchContext.workingDirectoryOverride,
                        projectBasePath = project.basePath,
                    )
                when {
                    !Files.isDirectory(Path.of(workingDirectory)) -> {
                        logTerminalFailure("Working directory does not exist:\n$workingDirectory")
                        null
                    }
                    else -> {
                        val effectiveRunArguments =
                            applyCursorResumeFallbackForLocal(
                                binaryPath = binaryPath,
                                arguments = effectiveArguments,
                                workingDirectory = workingDirectory,
                            )
                        buildTerminalCommand {
                            AgentCommandBuilder.buildLocalCommand(
                                binaryPath = binaryPath,
                                arguments = effectiveRunArguments,
                                useNodeShellWrapper = configuration.useNodeShellWrapper,
                            )
                        }?.let { command ->
                            TerminalStartupRequest(workingDirectory = workingDirectory, command = command)
                        }
                    }
                }
            }
        }

    private fun buildWslTerminalStartupRequest(
        configuration: AgentSettingsState.AgentCliConfiguration,
        binaryPath: String,
        effectiveArguments: List<String>,
        launchContext: AgentLaunchContext,
    ): TerminalStartupRequest? {
        val resolvedWslWorkingDirectory =
            WslPathResolver.resolveWslWorkingDirectory(
                configuredWorkingDirectory = configuration.workingDirectory,
                overrideWorkingDirectory = launchContext.workingDirectoryOverride,
                projectBasePath = project.basePath,
            )
        val overrideValue = launchContext.workingDirectoryOverride?.trim().orEmpty()
        val configured = configuration.workingDirectory.trim()
        val basePath = project.basePath?.trim().orEmpty()
        val rawPath =
            when {
                overrideValue.isNotBlank() -> overrideValue
                configured.isNotBlank() -> configured
                basePath.isNotBlank() -> basePath
                else -> ""
            }
        if (rawPath.isNotBlank() && WslPathResolver.mapToWslPath(rawPath) == null) {
            logTerminalFailure(
                "Working directory could not be mapped to a WSL path:\n" +
                    "${configuration.workingDirectory}\n\n" +
                    "Use one of:\n" +
                    "- Linux path (for example /home/user/project)\n" +
                    "- WSL UNC path (for example \\\\wsl.localhost\\Ubuntu\\home\\user\\project)\n" +
                    "- Windows drive path (for example D:\\project)",
            )
            return null
        }
        val effectiveDistribution =
            configuration.wslDistribution
                .trim()
                .ifBlank { resolvedWslWorkingDirectory.inferredDistribution.orEmpty() }
        val hostWorkingDirectory = WslPathResolver.resolveHostWorkingDirectory(project.basePath)
        val effectiveRunArguments =
            applyCursorResumeFallbackForWsl(
                binaryPath = binaryPath,
                arguments = effectiveArguments,
                wslDistribution = effectiveDistribution,
                wslWorkingDirectory = resolvedWslWorkingDirectory.linuxPath,
                hostWorkingDirectory = hostWorkingDirectory,
            )
        return buildTerminalCommand {
            AgentCommandBuilder.buildWslCommand(
                AgentWslCommandRequest(
                    binaryPath = binaryPath,
                    arguments = effectiveRunArguments,
                    wslDistribution = effectiveDistribution,
                    wslWorkingDirectory = resolvedWslWorkingDirectory.linuxPath,
                    useNodeShellWrapper = configuration.useNodeShellWrapper,
                ),
            )
        }?.let { command ->
            TerminalStartupRequest(workingDirectory = hostWorkingDirectory, command = command)
        }
    }

    private fun launchTerminalWidget(
        binaryPath: String,
        startupRequest: TerminalStartupRequest,
    ) {
        val startupOptions =
            ShellStartupOptions
                .Builder()
                .workingDirectory(startupRequest.workingDirectory)
                .shellCommand(startupRequest.command)
                .build()
        runCatching {
            val runner = DefaultTerminalRunnerFactory.getInstance().createLocalRunner(project)
            val terminalWidget = runner.startShellTerminalWidget(this, startupOptions, false)
            terminalFocusComponent = terminalWidget.preferredFocusableComponent
            rootPanel.removeAll()
            rootPanel.add(terminalWidget.component, BorderLayout.CENTER)
            rootPanel.border = JBUI.Borders.empty()
        }.onFailure { throwable ->
            logTerminalFailure(
                message =
                    terminalFailureDetail(
                        "Failed to initialize terminal widget for '$binaryPath'",
                        throwable,
                    ),
                throwable = throwable,
            )
        }
    }

    private fun buildTerminalCommand(builder: () -> List<String>): List<String>? =
        kotlin
            .runCatching(builder)
            .getOrElse { throwable ->
                logTerminalFailure(
                    message = terminalFailureDetail("Failed to build agent launch command", throwable),
                    throwable = throwable,
                )
                null
            }

    private fun terminalFailureDetail(
        prefix: String,
        throwable: Throwable,
    ): String = "$prefix:\n${throwable.message ?: throwable.javaClass.simpleName}"

    private fun logTerminalFailure(
        message: String,
        throwable: Throwable? = null,
    ) {
        log.error(
            message = "PTY terminal launch failed: $message",
            throwable = throwable,
            context = file.toAgentCliSessionContext(LaunchMode.PTY_PASSTHROUGH),
        )
        showError(message)
    }

    private fun showError(message: String) {
        val area =
            JBTextArea(message).apply {
                isEditable = false
                isOpaque = false
                lineWrap = true
                wrapStyleWord = true
                border = JBUI.Borders.empty(AcpUiMetrics.ERROR_PANEL_INSET)
                foreground = JBColor.foreground()
            }
        val scrollPane =
            JBScrollPane(area).apply {
                border = JBUI.Borders.empty()
                isOpaque = false
                viewport.isOpaque = false
                background = JBColor.PanelBackground
                viewport.background = JBColor.PanelBackground
            }
        rootPanel.removeAll()
        rootPanel.background = JBColor.PanelBackground
        rootPanel.add(scrollPane, BorderLayout.CENTER)

        ApplicationManager.getApplication().invokeLater({
            if (!project.isDisposed) {
                rootPanel.revalidate()
                rootPanel.repaint()
            }
        }, ModalityState.any())
    }

    companion object {
        private val log = AgentCliLog.getInstance(PtyAgentEditor::class.java)
        private val NAVIGATION_ACTION_IDS =
            listOf(
                IdeActions.ACTION_PREVIOUS_EDITOR_TAB,
                IdeActions.ACTION_NEXT_EDITOR_TAB,
                IdeActions.ACTION_PREVIOUS_TAB,
                IdeActions.ACTION_NEXT_TAB,
                IdeActions.ACTION_GOTO_BACK,
                IdeActions.ACTION_GOTO_FORWARD,
            )
    }

    private data class TerminalStartupRequest(
        val workingDirectory: String,
        val command: List<String>,
    )
}
