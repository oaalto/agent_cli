package com.oaalto.agent.acp.ui

import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.SwingConstants

class ShellPaneHost {
    private val placeholder =
        JBLabel("Shell pane idle — terminal/create wiring ships in PRD 3.", SwingConstants.CENTER).apply {
            foreground = JBColor.GRAY
            border = JBUI.Borders.empty(16)
        }
    private val panel =
        JPanel(BorderLayout()).apply {
            border = JBUI.Borders.compound(JBUI.Borders.customLine(JBColor.border()), JBUI.Borders.empty(4))
            add(placeholder, BorderLayout.CENTER)
        }

    val component: JComponent
        get() = panel
}
