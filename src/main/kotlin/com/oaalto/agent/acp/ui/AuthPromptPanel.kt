package com.oaalto.agent.acp.ui

import com.intellij.ui.JBColor
import com.intellij.ui.components.JBTextArea
import com.intellij.util.ui.JBUI
import com.oaalto.agent.acp.auth.AuthMethodSupport
import com.oaalto.agent.acp.auth.AuthPromptResult
import kotlinx.coroutines.CompletableDeferred
import java.awt.BorderLayout
import java.awt.Font
import java.awt.GridLayout
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTextField

class AuthPromptPanel {
    private val messageArea =
        JBTextArea().apply {
            isEditable = false
            lineWrap = true
            wrapStyleWord = true
            font = Font(Font.MONOSPACED, Font.PLAIN, JBUI.scale(12))
            border = JBUI.Borders.empty(4, 8)
            background = JBColor.PanelBackground
            foreground = JBColor.foreground()
            rows = 3
        }
    private val inputField =
        JTextField().apply {
            font = Font(Font.MONOSPACED, Font.PLAIN, JBUI.scale(12))
            border = JBUI.Borders.empty(4, 8)
            isVisible = false
        }
    private val buttonsPanel =
        JPanel(GridLayout(1, 0, JBUI.scale(4), 0)).apply {
            border = JBUI.Borders.empty(4, 8, 8, 8)
        }
    private val panel =
        JPanel(BorderLayout()).apply {
            border =
                JBUI.Borders.compound(
                    JBUI.Borders.customLine(JBColor.border()),
                    JBUI.Borders.empty(4),
                )
            background = JBColor.PanelBackground
            isVisible = false
            add(messageArea, BorderLayout.NORTH)
            add(inputField, BorderLayout.CENTER)
            add(buttonsPanel, BorderLayout.SOUTH)
        }
    private var pending: CompletableDeferred<AuthPromptResult>? = null

    val component: JComponent
        get() = panel

    fun suspendForApiKey(
        methodName: String,
        description: String?,
    ): CompletableDeferred<AuthPromptResult> {
        resetDeferred()
        messageArea.text =
            AuthMethodSupport.formatAuthMessage(
                methodName = methodName,
                description = description,
                actionLine = "Enter API key:",
            )
        inputField.text = ""
        inputField.isVisible = true
        buttonsPanel.removeAll()
        buttonsPanel.add(continueButton())
        buttonsPanel.add(cancelButton())
        show()
        return pending!!
    }

    fun suspendForOAuth(
        methodName: String,
        description: String?,
        link: String,
    ): CompletableDeferred<AuthPromptResult> {
        resetDeferred()
        messageArea.text =
            AuthMethodSupport.formatAuthMessage(
                methodName = methodName,
                description = description,
                actionLine = link,
            )
        inputField.isVisible = false
        buttonsPanel.removeAll()
        buttonsPanel.add(
            JButton("Open link").apply {
                font = Font(Font.MONOSPACED, Font.PLAIN, JBUI.scale(11))
                addActionListener {
                    runCatching {
                        java.awt.Desktop
                            .getDesktop()
                            .browse(java.net.URI(link))
                    }
                }
            },
        )
        buttonsPanel.add(continueButton())
        buttonsPanel.add(cancelButton())
        show()
        return pending!!
    }

    fun suspendForTerminalAuth(
        methodName: String,
        description: String?,
    ): CompletableDeferred<AuthPromptResult> {
        resetDeferred()
        messageArea.text =
            AuthMethodSupport.formatAuthMessage(
                methodName = methodName,
                description = description,
                actionLine = "Complete login in the Shell pane, then continue.",
            )
        inputField.isVisible = false
        buttonsPanel.removeAll()
        buttonsPanel.add(continueButton())
        buttonsPanel.add(cancelButton())
        show()
        return pending!!
    }

    fun cancelPending() {
        complete(AuthPromptResult.Cancelled)
    }

    private fun resetDeferred() {
        pending?.complete(AuthPromptResult.Cancelled)
        pending = CompletableDeferred()
    }

    private fun continueButton(): JButton =
        JButton("Continue").apply {
            font = Font(Font.MONOSPACED, Font.PLAIN, JBUI.scale(11))
            addActionListener {
                complete(AuthPromptResult.Continue)
            }
        }

    private fun cancelButton(): JButton =
        JButton("Cancel").apply {
            font = Font(Font.MONOSPACED, Font.PLAIN, JBUI.scale(11))
            addActionListener {
                complete(AuthPromptResult.Cancelled)
            }
        }

    private fun show() {
        panel.isVisible = true
        panel.revalidate()
        panel.repaint()
    }

    private fun hide() {
        panel.isVisible = false
        panel.revalidate()
        panel.repaint()
    }

    private fun complete(result: AuthPromptResult) {
        val deferred = pending ?: return
        pending = null
        hide()
        deferred.complete(result)
    }
}
