package com.oaalto.agent.acp.ui

import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import javax.swing.JComponent
import javax.swing.JPanel

class PromptInputBar(
    private val onSubmit: (String) -> Unit,
) {
    private val inputField =
        JBTextField().apply {
            emptyText.text = "Send a prompt (Enter to submit)"
            addKeyListener(
                object : KeyAdapter() {
                    override fun keyPressed(event: KeyEvent) {
                        if (event.keyCode == KeyEvent.VK_ENTER && !event.isShiftDown) {
                            event.consume()
                            submitCurrentText()
                        }
                    }
                },
            )
        }
    private val panel =
        JPanel(BorderLayout()).apply {
            border = JBUI.Borders.empty(8, 12, 8, 12)
            add(inputField, BorderLayout.CENTER)
        }

    val component: JComponent
        get() = panel

    fun requestFocus() {
        inputField.requestFocusInWindow()
    }

    fun setEnabled(enabled: Boolean) {
        inputField.isEnabled = enabled
    }

    private fun submitCurrentText() {
        if (!inputField.isEnabled) return
        val text = inputField.text.trim()
        if (text.isEmpty()) return
        inputField.text = ""
        onSubmit(text)
    }
}
