package com.oaalto.agent

import com.oaalto.agent.settings.AgentSettingsState
import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.ActionUiKind
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.codeHighlighting.BackgroundEditorHighlighter
import com.intellij.ide.structureView.StructureViewBuilder
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.diagnostic.Logger
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
import org.jetbrains.plugins.terminal.DefaultTerminalRunnerFactory
import org.jetbrains.plugins.terminal.ShellStartupOptions
import java.awt.Component
import java.awt.KeyEventDispatcher
import java.awt.KeyboardFocusManager
import java.awt.BorderLayout
import java.awt.event.KeyEvent
import java.beans.PropertyChangeListener
import java.beans.PropertyChangeSupport
import java.nio.file.Files
import java.nio.file.Path
import java.util.Locale
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.SwingUtilities

class AgentFileEditor(
    private val project: Project,
    private val file: AgentVirtualFile,
) : FileEditor, Disposable {
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

    override fun getPreferredFocusedComponent(): JComponent =
        terminalFocusComponent ?: rootPanel

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

    override fun <T : Any?> putUserData(key: Key<T>, value: T?) {
        userData.putUserData(key, value)
    }

    override fun dispose() {
        KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(keyEventDispatcher)
        // Terminal widget is registered with this editor as parent disposable.
    }

    private fun handleEditorNavigationShortcut(event: KeyEvent): Boolean {
        if (event.id != KeyEvent.KEY_PRESSED || event.isConsumed || project.isDisposed) {
            return false
        }

        val focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().focusOwner ?: return false
        val terminalComponent = terminalFocusComponent
        if (terminalComponent == null || !SwingUtilities.isDescendingFrom(focusOwner, terminalComponent)) {
            return false
        }

        for (actionId in NAVIGATION_ACTION_IDS) {
            if (KeymapUtil.isEventForAction(event, actionId) && invokeIdeAction(actionId, focusOwner, event)) {
                event.consume()
                return true
            }
        }
        return false
    }

    private fun invokeIdeAction(actionId: String, focusOwner: Component, event: KeyEvent): Boolean {
        val action = ActionManager.getInstance().getAction(actionId) ?: return false
        val dataContext = DataManager.getInstance().getDataContext(focusOwner)
        val actionEvent = AnActionEvent.createEvent(
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
        val configuration = resolveConfiguration() ?: return
        val binaryPath = configuration.binaryPath.trim()
        if (binaryPath.isBlank()) {
            showError("Agent binary path is empty for configuration '${configuration.name}'.")
            return
        }

        val parsedArguments = ParametersListUtil.parse(configuration.arguments)
        val target = resolveExecutionTarget(configuration.executionTarget)
        val startupRequest = when (target) {
            AgentSettingsState.ExecutionTarget.LOCAL -> {
                if (binaryPath.contains("/") && !Files.isExecutable(Path.of(binaryPath))) {
                    showError("Agent binary is not executable:\n$binaryPath")
                    return
                }
                val workingDirectory = resolveWorkingDirectory(configuration.workingDirectory)
                if (!Files.isDirectory(Path.of(workingDirectory))) {
                    showError("Working directory does not exist:\n$workingDirectory")
                    return
                }
                TerminalStartupRequest(
                    workingDirectory = workingDirectory,
                    command = buildList {
                        add(binaryPath)
                        addAll(parsedArguments)
                    },
                )
            }
            AgentSettingsState.ExecutionTarget.WSL -> {
                val linuxWorkingDirectory = resolveWslWorkingDirectory(
                    configuredWorkingDirectory = configuration.workingDirectory,
                )
                if (linuxWorkingDirectory == null) {
                    showError(
                        "Working directory could not be mapped to a WSL path:\n${configuration.workingDirectory}\n\n" +
                            "Use one of:\n" +
                            "- Linux path (for example /home/user/project)\n" +
                            "- WSL UNC path (for example \\\\wsl.localhost\\Ubuntu\\home\\user\\project)\n" +
                            "- Windows drive path (for example D:\\project)",
                    )
                    return
                }
                TerminalStartupRequest(
                    workingDirectory = resolveHostWorkingDirectory(),
                    command = buildWslCommand(
                        binaryPath = binaryPath,
                        arguments = parsedArguments,
                        wslDistribution = configuration.wslDistribution,
                        wslWorkingDirectory = linuxWorkingDirectory,
                    ),
                )
            }
        }

        val startupOptions = ShellStartupOptions.Builder()
            .workingDirectory(startupRequest.workingDirectory)
            .shellCommand(startupRequest.command)
            .build()
        try {
            val runner = DefaultTerminalRunnerFactory.getInstance().createLocalRunner(project)
            val terminalWidget = runner.startShellTerminalWidget(this, startupOptions, false)
            terminalFocusComponent = terminalWidget.preferredFocusableComponent
            rootPanel.removeAll()
            rootPanel.add(terminalWidget.component, BorderLayout.CENTER)
            rootPanel.border = JBUI.Borders.empty()
        } catch (t: Throwable) {
            logger.warn("Failed to initialize terminal widget for '$binaryPath'", t)
            showError("Failed to initialize terminal widget:\n${t.message ?: t.javaClass.simpleName}")
        }
    }

    private fun resolveConfiguration(): AgentSettingsState.AgentCliConfiguration? {
        val configuration = AgentSettingsState.getInstance().getConfigurationById(file.configurationId)
        if (configuration == null) {
            showError("Agent configuration was removed or is unavailable.")
        }
        return configuration
    }

    private fun resolveWorkingDirectory(configuredWorkingDirectory: String): String {
        val configured = configuredWorkingDirectory.trim()
        return when {
            configured.isNotBlank() -> configured
            !project.basePath.isNullOrBlank() -> project.basePath!!
            else -> System.getProperty("user.home")
        }
    }

    private fun resolveExecutionTarget(rawTarget: String): AgentSettingsState.ExecutionTarget {
        val normalized = rawTarget.trim().uppercase(Locale.ROOT)
        return AgentSettingsState.ExecutionTarget.entries.firstOrNull { it.name == normalized }
            ?: AgentSettingsState.ExecutionTarget.LOCAL
    }

    private fun resolveWslWorkingDirectory(
        configuredWorkingDirectory: String,
    ): String? {
        val configured = configuredWorkingDirectory.trim()
        if (configured.isNotBlank()) {
            return mapToWslPath(configured)
        }

        val basePath = project.basePath?.trim().orEmpty()
        if (basePath.isNotBlank()) {
            return mapToWslPath(basePath)
        }
        return "/home"
    }

    private fun mapToWslPath(rawPath: String): String? {
        val trimmed = rawPath.trim()
        if (trimmed.isBlank()) return null
        if (trimmed.startsWith("/") || trimmed.startsWith("~")) {
            return trimmed
        }

        val windowsStylePath = trimmed.replace('/', '\\')
        UNC_WSL_PREFIXES.firstOrNull { prefix ->
            windowsStylePath.startsWith(prefix, ignoreCase = true)
        }?.let { prefix ->
            val withoutPrefix = windowsStylePath.substring(prefix.length)
            val segments = withoutPrefix.split('\\').filter { it.isNotBlank() }
            if (segments.isEmpty()) return null
            val linuxSegments = segments.drop(1)
            return if (linuxSegments.isEmpty()) "/" else "/" + linuxSegments.joinToString("/")
        }

        WINDOWS_DRIVE_PATH_REGEX.matchEntire(windowsStylePath)?.let { match ->
            val drive = match.groupValues[1].lowercase(Locale.ROOT)
            val rest = match.groupValues[2].replace('\\', '/').trim('/')
            return if (rest.isBlank()) "/mnt/$drive" else "/mnt/$drive/$rest"
        }

        if (!windowsStylePath.contains('\\')) {
            return trimmed
        }
        return null
    }

    private fun buildWslCommand(
        binaryPath: String,
        arguments: List<String>,
        wslDistribution: String,
        wslWorkingDirectory: String,
    ): List<String> {
        return buildList {
            add("wsl.exe")
            val distribution = wslDistribution.trim()
            if (distribution.isNotBlank()) {
                add("--distribution")
                add(distribution)
            }
            add("--cd")
            add(wslWorkingDirectory)
            add("--")
            add("/bin/bash")
            add("-lc")
            add(buildBashCommandLine(binaryPath, arguments))
        }
    }

    private fun buildBashCommandLine(binaryPath: String, arguments: List<String>): String {
        return buildList {
            add(binaryPath)
            addAll(arguments)
        }.joinToString(" ") { quoteForBash(it) }
    }

    private fun quoteForBash(value: String): String = "'${value.replace("'", "'\"'\"'")}'"

    private fun resolveHostWorkingDirectory(): String {
        val candidates = listOf(
            project.basePath,
            System.getProperty("user.home"),
            System.getProperty("java.io.tmpdir"),
        )
        return candidates.asSequence()
            .mapNotNull { it?.trim()?.takeIf(String::isNotBlank) }
            .firstOrNull { path ->
                kotlin.runCatching { Files.isDirectory(Path.of(path)) }.getOrDefault(false)
            }
            ?: System.getProperty("user.home")
    }

    private fun showError(message: String) {
        val area = JBTextArea(message).apply {
            isEditable = false
            isOpaque = false
            lineWrap = true
            wrapStyleWord = true
            border = JBUI.Borders.empty(12)
            foreground = JBColor.foreground()
        }
        val scrollPane = JBScrollPane(area).apply {
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
        private val logger = Logger.getInstance(AgentFileEditor::class.java)
        private val UNC_WSL_PREFIXES = listOf("\\\\wsl.localhost\\", "\\\\wsl$\\")
        private val WINDOWS_DRIVE_PATH_REGEX = Regex("""^([A-Za-z]):\\(.*)$""")
        private val NAVIGATION_ACTION_IDS = listOf(
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
