package com.oaalto.agent.acp.ui

import com.agentclientprotocol.model.PermissionOption
import com.agentclientprotocol.model.PermissionOptionKind
import com.agentclientprotocol.model.RequestPermissionOutcome
import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import kotlinx.coroutines.CompletableDeferred
import java.awt.BorderLayout
import java.awt.Font
import java.awt.GridLayout
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants

class PermissionPromptPanel {
    private val titleLabel =
        JLabel("", SwingConstants.LEFT).apply {
            font = Font(Font.MONOSPACED, Font.PLAIN, JBUI.scale(12))
            border = JBUI.Borders.empty(4, 8)
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
            add(titleLabel, BorderLayout.CENTER)
            add(buttonsPanel, BorderLayout.SOUTH)
        }
    private var pending: CompletableDeferred<RequestPermissionOutcome>? = null

    val component: JComponent
        get() = panel

    fun suspendForPrompt(
        title: String,
        options: List<PermissionOption>,
    ): CompletableDeferred<RequestPermissionOutcome> {
        pending?.complete(RequestPermissionOutcome.Cancelled)
        val deferred = CompletableDeferred<RequestPermissionOutcome>()
        pending = deferred
        titleLabel.text = "[permission] $title"
        buttonsPanel.removeAll()
        options.forEach { option ->
            buttonsPanel.add(
                JButton(option.name).apply {
                    font = Font(Font.MONOSPACED, Font.PLAIN, JBUI.scale(11))
                    addActionListener {
                        complete(RequestPermissionOutcome.Selected(option.optionId))
                    }
                },
            )
        }
        buttonsPanel.add(
            JButton("Cancel").apply {
                font = Font(Font.MONOSPACED, Font.PLAIN, JBUI.scale(11))
                addActionListener {
                    complete(RequestPermissionOutcome.Cancelled)
                }
            },
        )
        panel.isVisible = true
        panel.revalidate()
        panel.repaint()
        return deferred
    }

    fun hide() {
        panel.isVisible = false
        panel.revalidate()
        panel.repaint()
    }

    fun cancelPending() {
        complete(RequestPermissionOutcome.Cancelled)
    }

    private fun complete(outcome: RequestPermissionOutcome) {
        val deferred = pending ?: return
        pending = null
        hide()
        deferred.complete(outcome)
    }

    companion object {
        fun labelFor(kind: PermissionOptionKind): String =
            when (kind) {
                PermissionOptionKind.ALLOW_ONCE -> "Allow once"
                PermissionOptionKind.ALLOW_ALWAYS -> "Allow always"
                PermissionOptionKind.REJECT_ONCE -> "Reject once"
                PermissionOptionKind.REJECT_ALWAYS -> "Reject always"
            }
    }
}
