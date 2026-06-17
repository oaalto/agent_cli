package com.oaalto.agent.acp

import com.intellij.openapi.ui.Splitter
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import com.oaalto.agent.acp.ui.AuthPromptPanel
import com.oaalto.agent.acp.ui.PermissionPromptPanel
import com.oaalto.agent.acp.ui.PromptInputBar
import com.oaalto.agent.acp.ui.ShellPaneHost
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel

internal object AcpEditorLayout {
    const val PROMPT_SPLIT_RATIO = 0.2f
    const val TRANSCRIPT_SPLIT_RATIO = 0.72f

    fun buildRootPanel(
        transcriptArea: JComponent,
        permissionPromptPanel: PermissionPromptPanel,
        authPromptPanel: AuthPromptPanel,
        promptInputBar: PromptInputBar,
        shellPaneHost: ShellPaneHost,
    ): JPanel {
        val transcriptColumn =
            JPanel(BorderLayout()).apply {
                add(JBScrollPane(transcriptArea), BorderLayout.CENTER)
                add(permissionPromptPanel.component, BorderLayout.SOUTH)
                add(authPromptPanel.component, BorderLayout.NORTH)
            }
        val bottomSplitter =
            Splitter(true, PROMPT_SPLIT_RATIO).apply {
                firstComponent = promptInputBar.component
                secondComponent = shellPaneHost.component
            }
        val mainSplitter =
            Splitter(true, TRANSCRIPT_SPLIT_RATIO).apply {
                firstComponent = transcriptColumn
                secondComponent = bottomSplitter
            }
        return JPanel(BorderLayout()).apply {
            add(mainSplitter, BorderLayout.CENTER)
            border = JBUI.Borders.empty()
        }
    }
}
