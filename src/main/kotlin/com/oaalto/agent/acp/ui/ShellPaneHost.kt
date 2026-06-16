package com.oaalto.agent.acp.ui

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import org.jetbrains.plugins.terminal.DefaultTerminalRunnerFactory
import org.jetbrains.plugins.terminal.ShellStartupOptions
import org.jetbrains.plugins.terminal.ShellTerminalWidget
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel

/**
 * Hosts the Shell pane PTY. A second [attachTerminal] replaces the active widget (3.0 policy).
 */
class ShellPaneHost(
    private val project: Project,
    private val parentDisposable: Disposable,
) {
    private val panel =
        JPanel(BorderLayout()).apply {
            border = JBUI.Borders.compound(JBUI.Borders.customLine(JBColor.border()), JBUI.Borders.empty(4))
        }
    private var activeWidget: ShellTerminalWidget? = null

    val component: JComponent
        get() = panel

    fun attachTerminal(widget: ShellTerminalWidget) {
        replaceWidget(widget)
    }

    fun startAuthCommand(
        command: List<String>,
        env: Map<String, String>,
    ): ShellTerminalWidget {
        val startupOptions =
            ShellStartupOptions
                .Builder()
                .shellCommand(command)
                .envVariables(env)
                .build()
        val widget = createWidget(startupOptions)
        replaceWidget(widget)
        return widget
    }

    fun startCommand(
        command: String,
        args: List<String>,
        cwd: String?,
        env: List<Pair<String, String>>,
    ): ShellTerminalWidget {
        val shellCommand =
            buildList {
                add(command)
                addAll(args)
            }
        val builder =
            ShellStartupOptions
                .Builder()
                .shellCommand(shellCommand)
                .envVariables(env.toMap())
        cwd?.trim()?.takeIf { it.isNotEmpty() }?.let(builder::workingDirectory)
        val widget = createWidget(builder.build())
        replaceWidget(widget)
        return widget
    }

    fun clear() {
        activeWidget?.let { widget ->
            runCatching { widget.close() }
        }
        activeWidget = null
        panel.removeAll()
        panel.revalidate()
        panel.repaint()
    }

    private fun createWidget(startupOptions: ShellStartupOptions): ShellTerminalWidget {
        val runner = DefaultTerminalRunnerFactory.getInstance().createLocalRunner(project)
        return ShellTerminalWidget.asShellJediTermWidget(
            runner.startShellTerminalWidget(parentDisposable, startupOptions, false),
        ) ?: error("Failed to create Shell pane terminal widget.")
    }

    private fun replaceWidget(widget: ShellTerminalWidget) {
        activeWidget?.let { existing ->
            runCatching { existing.close() }
        }
        activeWidget = widget
        panel.removeAll()
        panel.add(widget.component, BorderLayout.CENTER)
        panel.revalidate()
        panel.repaint()
    }

    companion object {
        fun formatCommandLabel(
            command: String,
            args: List<String>,
        ): String = (listOf(command) + args).joinToString(" ").trim()
    }
}
