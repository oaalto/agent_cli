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

/**
 * Container for editor layout UI components to avoid long parameter lists.
 */
internal data class EditorLayoutComponents(
    val transcriptArea: JComponent,
    val permissionPromptPanel: PermissionPromptPanel,
    val authPromptPanel: AuthPromptPanel,
    val promptInputBar: PromptInputBar,
    val shellPaneHost: ShellPaneHost,
    val transcriptFooter: JComponent,
)

internal object AcpEditorLayout {
    const val PROMPT_SPLIT_RATIO = 0.2f
    const val TRANSCRIPT_SPLIT_RATIO = 0.72f

    fun buildRootPanel(components: EditorLayoutComponents): JPanel {
        val transcriptColumn =
            JPanel(BorderLayout()).apply {
                add(JBScrollPane(components.transcriptArea), BorderLayout.CENTER)
                add(components.permissionPromptPanel.component, BorderLayout.SOUTH)
                add(components.authPromptPanel.component, BorderLayout.NORTH)
            }
        val bottomSplitter =
            Splitter(true, PROMPT_SPLIT_RATIO).apply {
                firstComponent = components.promptInputBar.component
                secondComponent = components.shellPaneHost.component
            }
        val mainSplitter =
            Splitter(true, TRANSCRIPT_SPLIT_RATIO).apply {
                firstComponent = transcriptColumn
                secondComponent = bottomSplitter
            }
        return JPanel(BorderLayout()).apply {
            add(mainSplitter, BorderLayout.CENTER)
            add(components.transcriptFooter, BorderLayout.SOUTH)
            border = JBUI.Borders.empty()
        }
    }
}
