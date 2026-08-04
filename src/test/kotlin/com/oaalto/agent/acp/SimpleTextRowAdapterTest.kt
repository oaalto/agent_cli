package com.oaalto.agent.acp

import com.intellij.ui.JBColor
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTextPane
import javax.swing.SwingUtilities
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SimpleTextRowAdapterTest {
    @Test
    fun `matches returns true for simple text block types`() {
        val adapter = SimpleTextRowAdapter()
        assertTrue(adapter.matches(TranscriptBlock.UserEcho("1", "hello")))
        assertTrue(adapter.matches(TranscriptBlock.Thought("1", "thinking")))
        assertTrue(adapter.matches(TranscriptBlock.PlainLine("1", "plain")))
        assertTrue(adapter.matches(TranscriptBlock.ErrorLine("1", "error")))
        assertTrue(adapter.matches(TranscriptBlock.AuthFailureLine("1", "auth")))
    }

    @Test
    fun `matches returns false for non-simple block types`() {
        val adapter = SimpleTextRowAdapter()
        assertFalse(adapter.matches(TranscriptBlock.StreamingAgentText("1", "streaming")))
        assertFalse(adapter.matches(TranscriptBlock.FinalAgentText("1", "final")))
        assertFalse(
            adapter.matches(
                TranscriptBlock.ToolCallBlock(
                    blockId = "1",
                    toolCallId = "t1",
                    title = "t",
                    kind = null,
                    status = null,
                    bodyParts = emptyList(),
                ),
            ),
        )
        assertFalse(
            adapter.matches(
                TranscriptBlock.PlanBlock(
                    blockId = "1",
                    planId = "p1",
                    entries = emptyList(),
                ),
            ),
        )
    }

    @Test
    fun `UserEcho row renders with correct text and color`() =
        runOnEdt {
            val context = createRowContext()
            val adapter = SimpleTextRowAdapter()
            val row = adapter.create(context, TranscriptBlock.UserEcho("1", "hello world"), {})

            assertTrue(isSimpleTextRow(row))
            val textPane = findTextPane(row)
            assertEquals("> hello world", textPane.text)
            assertEquals(JBColor.BLUE, textPane.foreground)
        }

    @Test
    fun `Thought row renders with correct text and color`() =
        runOnEdt {
            val context = createRowContext()
            val adapter = SimpleTextRowAdapter()
            val row = adapter.create(context, TranscriptBlock.Thought("1", "considering options"), {})

            assertTrue(isSimpleTextRow(row))
            val textPane = findTextPane(row)
            assertEquals("[thought] considering options", textPane.text)
            // Thought color should be muted (not white/black)
            val thoughtColor = textPane.foreground
            assertNotNull(thoughtColor)
            assertTrue(thoughtColor.red > 0 && thoughtColor.red < 255, "thought color should be muted")
        }

    @Test
    fun `PlainLine row renders with correct text`() =
        runOnEdt {
            val context = createRowContext()
            val adapter = SimpleTextRowAdapter()
            val row = adapter.create(context, TranscriptBlock.PlainLine("1", "just a line"), {})

            assertTrue(isSimpleTextRow(row))
            val textPane = findTextPane(row)
            assertEquals("just a line", textPane.text)
        }

    @Test
    fun `PlainLine user prompt renders with user echo color`() =
        runOnEdt {
            val context = createRowContext()
            val adapter = SimpleTextRowAdapter()
            val row =
                adapter.create(
                    context,
                    TranscriptBlock.PlainLine("1", "user prompt", isUserPrompt = true),
                    {},
                )

            assertTrue(isSimpleTextRow(row))
            val textPane = findTextPane(row)
            assertEquals(JBColor.BLUE, textPane.foreground)
        }

    @Test
    fun `ErrorLine row renders with error color and formatted text`() =
        runOnEdt {
            val context = createRowContext()
            val adapter = SimpleTextRowAdapter()
            val row = adapter.create(context, TranscriptBlock.ErrorLine("1", "something went wrong"), {})

            assertTrue(isSimpleTextRow(row))
            val textPane = findTextPane(row)
            // Error foreground should be red-tinted
            val errorColor = textPane.foreground
            assertNotNull(errorColor)
            assertTrue(
                errorColor.red > errorColor.green || errorColor.red > errorColor.blue,
                "error color should be reddish",
            )
            assertTrue(textPane.text.contains("Error:"))
            assertTrue(textPane.text.contains("something went wrong"))
        }

    @Test
    fun `AuthFailureLine row renders with error color and formatted text`() =
        runOnEdt {
            val context = createRowContext()
            val adapter = SimpleTextRowAdapter()
            val row =
                adapter.create(
                    context,
                    TranscriptBlock.AuthFailureLine("1", "invalid token"),
                    {},
                )

            assertTrue(isSimpleTextRow(row))
            val textPane = findTextPane(row)
            // Auth failure foreground should be red-tinted (same as error)
            val authColor = textPane.foreground
            assertNotNull(authColor)
            assertTrue(
                authColor.red > authColor.green || authColor.red > authColor.blue,
                "auth failure color should be reddish",
            )
            assertTrue(textPane.text.contains("Auth failed:"))
            assertTrue(textPane.text.contains("invalid token"))
        }

    @Test
    fun `row has reasonable preferred height`() =
        runOnEdt {
            val context = createRowContext()
            val adapter = SimpleTextRowAdapter()
            val row = adapter.create(context, TranscriptBlock.PlainLine("1", "some text"), {})
            row.setSize(600, 0)
            row.doLayout()

            val height = row.preferredSize.height
            assertTrue(height > 0 && height < 50, "expected small height, got $height")
        }

    @Test
    fun `row maximum size allows unlimited width`() =
        runOnEdt {
            val context = createRowContext()
            val adapter = SimpleTextRowAdapter()
            val row = adapter.create(context, TranscriptBlock.PlainLine("1", "text"), {})

            val max = row.maximumSize
            assertEquals(Int.MAX_VALUE, max.width)
        }

    @Test
    fun `update modifies existing row in place`() =
        runOnEdt {
            val context = createRowContext()
            val adapter = SimpleTextRowAdapter()
            val row = adapter.create(context, TranscriptBlock.PlainLine("1", "initial"), {})

            assertTrue(adapter.update(context, row, TranscriptBlock.PlainLine("1", "updated")))
            val textPane = findTextPane(row)
            assertEquals("updated", textPane.text)
        }

    @Test
    fun `update returns false on type mismatch`() =
        runOnEdt {
            val context = createRowContext()
            val adapter = SimpleTextRowAdapter()
            val row = adapter.create(context, TranscriptBlock.PlainLine("1", "text"), {})

            // AgentTextRow created by factory for FinalAgentText
            val factory = TranscriptBlockViewFactory(PlainMonospaceTranscriptCodeBlockViewFactory)
            val agentRow = factory.create(TranscriptBlock.FinalAgentText("2", "agent"), {})

            assertFalse(adapter.update(context, agentRow, TranscriptBlock.PlainLine("3", "plain")))
        }

    @Test
    fun `dispose does not throw`() =
        runOnEdt {
            val context = createRowContext()
            val adapter = SimpleTextRowAdapter()
            val row = adapter.create(context, TranscriptBlock.PlainLine("1", "text"), {})

            adapter.dispose(context, row)
            // No exception = success
        }

    @Test
    fun `factory creates SimpleTextRow via adapter for UserEcho`() =
        runOnEdt {
            val factory = TranscriptBlockViewFactory(PlainMonospaceTranscriptCodeBlockViewFactory)
            val row = factory.create(TranscriptBlock.UserEcho("1", "echo"), {})

            assertTrue(isSimpleTextRow(row))
            val textPane = findTextPane(row)
            assertEquals("> echo", textPane.text)
        }

    @Test
    fun `factory creates SimpleTextRow via adapter for ErrorLine`() =
        runOnEdt {
            val factory = TranscriptBlockViewFactory(PlainMonospaceTranscriptCodeBlockViewFactory)
            val row = factory.create(TranscriptBlock.ErrorLine("1", "error msg"), {})

            assertTrue(isSimpleTextRow(row))
            val textPane = findTextPane(row)
            assertTrue(textPane.text.contains("error msg"))
        }

    @Test
    fun `factory disposeRow handles SimpleTextRow`() =
        runOnEdt {
            val factory = TranscriptBlockViewFactory(PlainMonospaceTranscriptCodeBlockViewFactory)
            val row = factory.create(TranscriptBlock.PlainLine("1", "text"), {})

            assertTrue(isSimpleTextRow(row))
            factory.disposeRow(row)
            // No exception = success
        }

    private fun createRowContext(): RowContext =
        RowContext(
            columnWidth = 600,
            codeBlockViewFactory = PlainMonospaceTranscriptCodeBlockViewFactory,
            colorProvider = DefaultTranscriptColorProvider(),
            logContextProvider = { null },
        )

    private fun findTextPane(row: JPanel): JTextPane {
        // SimpleTextRow adds textPane directly; find it by traversing
        val found = row.components.filterIsInstance<JTextPane>().firstOrNull()
        if (found != null) return found
        // Check children of first child (BorderLayout wrapper)
        row.components.forEach { parent ->
            if (parent is JComponent) {
                val child = parent.components.filterIsInstance<JTextPane>().firstOrNull()
                if (child != null) return child
            }
        }
        throw AssertionError("No JTextPane found in SimpleTextRow")
    }

    private fun runOnEdt(action: () -> Unit) {
        if (SwingUtilities.isEventDispatchThread()) {
            action()
        } else {
            SwingUtilities.invokeAndWait(action)
        }
    }
}
