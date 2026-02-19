package com.oaalto.agent

import com.oaalto.agent.settings.AgentSettingsState
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
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.util.execution.ParametersListUtil
import com.intellij.util.ui.JBUI
import org.jetbrains.plugins.terminal.DefaultTerminalRunnerFactory
import org.jetbrains.plugins.terminal.ShellStartupOptions
import java.awt.BorderLayout
import java.beans.PropertyChangeListener
import java.beans.PropertyChangeSupport
import java.nio.file.Files
import java.nio.file.Path
import javax.swing.JComponent
import javax.swing.JPanel

class AgentFileEditor(
    private val project: Project,
    private val file: AgentVirtualFile,
) : FileEditor, Disposable {
    private val propertyChangeSupport = PropertyChangeSupport(this)
    private val userData = UserDataHolderBase()
    private val rootPanel = JPanel(BorderLayout())

    init {
        startTerminalSession()
    }

    override fun getComponent(): JComponent = rootPanel

    override fun getPreferredFocusedComponent(): JComponent = rootPanel

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
        // Terminal widget is registered with this editor as parent disposable.
    }

    private fun startTerminalSession() {
        val configuration = resolveConfiguration() ?: return
        val binaryPath = configuration.binaryPath.trim()
        if (binaryPath.isBlank()) {
            showError("Agent binary path is empty for configuration '${configuration.name}'.")
            return
        }
        if (binaryPath.contains("/") && !Files.isExecutable(Path.of(binaryPath))) {
            showError("Agent binary is not executable:\n$binaryPath")
            return
        }

        val workingDirectory = resolveWorkingDirectory(configuration.workingDirectory)
        if (!Files.isDirectory(Path.of(workingDirectory))) {
            showError("Working directory does not exist:\n$workingDirectory")
            return
        }

        val command = buildList {
            add(binaryPath)
            addAll(ParametersListUtil.parse(configuration.arguments))
        }
        val startupOptions = ShellStartupOptions.Builder()
            .workingDirectory(workingDirectory)
            .shellCommand(command)
            .build()
        try {
            val runner = DefaultTerminalRunnerFactory.getInstance().createLocalRunner(project)
            val terminalWidget = runner.startShellTerminalWidget(this, startupOptions, false)
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

    private fun showError(message: String) {
        val area = JBTextArea(message)
        area.isEditable = false
        area.isOpaque = false
        area.lineWrap = true
        area.wrapStyleWord = true
        area.border = JBUI.Borders.empty(12)
        rootPanel.add(JBScrollPane(area), BorderLayout.CENTER)

        ApplicationManager.getApplication().invokeLater({
            if (!project.isDisposed) {
                rootPanel.revalidate()
                rootPanel.repaint()
            }
        }, ModalityState.any())
    }

    companion object {
        private val logger = Logger.getInstance(AgentFileEditor::class.java)
    }
}
