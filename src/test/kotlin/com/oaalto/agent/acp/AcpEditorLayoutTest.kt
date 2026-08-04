package com.oaalto.agent.acp

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Splitter
import com.intellij.ui.components.JBScrollPane
import com.oaalto.agent.acp.ui.AuthPromptPanel
import com.oaalto.agent.acp.ui.PermissionPromptPanel
import com.oaalto.agent.acp.ui.PromptInputBar
import com.oaalto.agent.acp.ui.ShellPaneHost
import java.awt.BorderLayout
import javax.swing.JPanel
import kotlin.test.Test
import kotlin.test.assertSame

class AcpEditorLayoutTest {
    @Test
    fun `transcript scroll pane is not wrapped in a second scroll pane`() {
        val transcriptScroll = JBScrollPane(JPanel())
        val disposable = Disposable { }
        val root =
            AcpEditorLayout.buildRootPanel(
                EditorLayoutComponents(
                    transcriptArea = transcriptScroll,
                    permissionPromptPanel = PermissionPromptPanel(),
                    authPromptPanel = AuthPromptPanel(),
                    promptInputBar = PromptInputBar {},
                    shellPaneHost = ShellPaneHost(fakeProject(), disposable),
                    transcriptFooter = JPanel(),
                ),
            )

        val transcriptColumn = transcriptColumn(root)
        val center = borderLayoutCenter(transcriptColumn)
        assertSame(
            transcriptScroll,
            center,
            "TranscriptPanel scroll pane must be the CENTER child, not wrapped in another JBScrollPane",
        )
    }

    private fun transcriptColumn(root: JPanel): JPanel {
        val mainSplitter = borderLayoutCenter(root) as Splitter
        return mainSplitter.firstComponent as JPanel
    }

    private fun borderLayoutCenter(panel: JPanel): java.awt.Component? {
        val layout = panel.layout as BorderLayout
        for (i in 0 until panel.componentCount) {
            val child = panel.getComponent(i)
            if (layout.getConstraints(child) == BorderLayout.CENTER) {
                return child
            }
        }
        return null
    }

    private fun fakeProject(): Project =
        java.lang.reflect.Proxy.newProxyInstance(
            Project::class.java.classLoader,
            arrayOf(Project::class.java),
        ) { _, method, _ ->
            when (method.name) {
                "isDisposed" -> false
                "getBasePath" -> null
                "toString" -> "FakeProject"
                else -> defaultValue(method.returnType)
            }
        } as Project

    private fun defaultValue(returnType: Class<*>): Any? =
        when (returnType) {
            Boolean::class.javaPrimitiveType, Boolean::class.javaObjectType -> false
            Int::class.javaPrimitiveType, Int::class.javaObjectType -> 0
            Long::class.javaPrimitiveType, Long::class.javaObjectType -> 0L
            else -> null
        }
}
