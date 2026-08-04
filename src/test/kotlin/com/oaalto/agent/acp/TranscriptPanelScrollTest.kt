package com.oaalto.agent.acp

import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBScrollPane
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import javax.swing.SwingUtilities
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TranscriptPanelScrollTest {
    @Test
    fun `sync keeps viewport at bottom when already following`() {
        lateinit var panel: TranscriptPanel
        lateinit var scrollPane: JBScrollPane

        runOnEdt {
            panel = createPanel()
            scrollPane = panel.component as JBScrollPane
            scrollPane.setSize(300, 80)
            scrollPane.doLayout()
        }

        val initialBlocks =
            (1..20).map { index ->
                TranscriptBlock.PlainLine(
                    blockId = "block-$index",
                    text = "line $index with enough text to wrap in a narrow viewport",
                )
            }

        runOnEdt { panel.sync(initialBlocks) }
        pumpPendingEdtTasks()

        runOnEdt {
            val bar = scrollPane.verticalScrollBar
            bar.value = bar.maximum
        }
        pumpPendingEdtTasks()

        val moreBlocks =
            initialBlocks +
                TranscriptBlock.PlainLine(
                    blockId = "block-new",
                    text = "new line at the end",
                )

        runOnEdt { panel.sync(moreBlocks) }
        pumpPendingEdtTasks()

        runOnEdt {
            val bar = scrollPane.verticalScrollBar
            assertTrue(bar.value + bar.visibleAmount >= bar.maximum - 4)
        }
    }

    @Test
    fun `sync does not scroll when viewport is not at bottom`() {
        lateinit var panel: TranscriptPanel
        lateinit var scrollPane: JBScrollPane

        runOnEdt {
            panel = createPanel()
            scrollPane = panel.component as JBScrollPane
            scrollPane.setSize(300, 80)
            scrollPane.doLayout()
        }

        val initialBlocks =
            (1..20).map { index ->
                TranscriptBlock.PlainLine(
                    blockId = "block-$index",
                    text = "line $index with enough text to wrap in a narrow viewport",
                )
            }

        runOnEdt { panel.sync(initialBlocks) }
        pumpPendingEdtTasks()

        var scrollBefore = 0
        runOnEdt {
            val bar = scrollPane.verticalScrollBar
            bar.value = 0
            scrollBefore = bar.value
        }

        val moreBlocks =
            initialBlocks +
                TranscriptBlock.PlainLine(
                    blockId = "block-new",
                    text = "new line at the end",
                )

        runOnEdt { panel.sync(moreBlocks) }
        pumpPendingEdtTasks()

        runOnEdt {
            val bar = scrollPane.verticalScrollBar
            assertEquals(scrollBefore, bar.value, "scroll position changed while reading history")
        }
    }

    private fun createPanel(): TranscriptPanel {
        val project =
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
        return TranscriptPanel.create(
            project = project,
            onToolToggle = {},
            codeBlockViewFactory = PlainMonospaceTranscriptCodeBlockViewFactory,
        )
    }

    private fun defaultValue(returnType: Class<*>): Any? =
        when (returnType) {
            Boolean::class.javaPrimitiveType, Boolean::class.javaObjectType -> false
            Int::class.javaPrimitiveType, Int::class.javaObjectType -> 0
            Long::class.javaPrimitiveType, Long::class.javaObjectType -> 0L
            else -> null
        }

    private fun runOnEdt(action: () -> Unit) {
        if (SwingUtilities.isEventDispatchThread()) {
            action()
        } else {
            SwingUtilities.invokeAndWait(action)
        }
    }

    private fun pumpPendingEdtTasks() {
        repeat(3) {
            val latch = CountDownLatch(1)
            SwingUtilities.invokeLater { latch.countDown() }
            check(latch.await(5, TimeUnit.SECONDS)) { "timed out waiting for EDT" }
        }
    }
}
