package com.oaalto.agent.acp

import com.oaalto.agent.acp.plan.PlanPanel
import com.oaalto.agent.acp.transcript.theme.DefaultTranscriptColorProvider
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.SwingUtilities
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlanRowAdapterTest {
    @Test
    fun `matches returns true for PlanBlock`() {
        val adapter = PlanRowAdapter()
        assertTrue(
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
    fun `matches returns false for non-plan block types`() {
        val adapter = PlanRowAdapter()
        assertFalse(adapter.matches(TranscriptBlock.UserEcho("1", "echo")))
        assertFalse(adapter.matches(TranscriptBlock.PlainLine("1", "plain")))
        assertFalse(adapter.matches(TranscriptBlock.FinalAgentText("1", "text")))
        assertFalse(
            adapter.matches(
                TranscriptBlock.ToolCallBlock(
                    blockId = "1",
                    toolCallId = "t1",
                    title = "tool",
                    kind = null,
                    status = null,
                    bodyParts = emptyList(),
                ),
            ),
        )
    }

    @Test
    fun `plan row renders with correct entry count`() =
        runOnEdt {
            val context = createRowContext()
            val adapter = PlanRowAdapter()
            val entries =
                listOf(
                    PlanEntry("First task", PlanEntryStatus.PENDING, PlanEntryPriority.HIGH),
                    PlanEntry("Second task", PlanEntryStatus.IN_PROGRESS, PlanEntryPriority.MEDIUM),
                    PlanEntry("Third task", PlanEntryStatus.COMPLETED, PlanEntryPriority.LOW),
                )
            val block = TranscriptBlock.PlanBlock("1", "p1", entries)
            val row = adapter.create(context, block, {})

            assertTrue(isPlanRow(row))
            assertTrue(row is PlanPanel)
            val labels = findLabels(row)
            val entryLabels = labels.filter { it.text.orEmpty().matches(Regex("^\\d+\\..*")) }
            assertEquals(3, entryLabels.size, "should have 3 entry labels")
        }

    @Test
    fun `plan row renders status icons`() =
        runOnEdt {
            val context = createRowContext()
            val adapter = PlanRowAdapter()
            val entries =
                listOf(
                    PlanEntry("Pending", PlanEntryStatus.PENDING, PlanEntryPriority.MEDIUM),
                    PlanEntry("In progress", PlanEntryStatus.IN_PROGRESS, PlanEntryPriority.MEDIUM),
                    PlanEntry("Done", PlanEntryStatus.COMPLETED, PlanEntryPriority.MEDIUM),
                )
            val block = TranscriptBlock.PlanBlock("1", "p1", entries)
            val row = adapter.create(context, block, {})

            val labels = findLabels(row)
            val iconLabels =
                labels.filter { lbl ->
                    val t = lbl.text.orEmpty()
                    t.contains("[ ]") || t.contains("[→]") || t.contains("[✓]")
                }
            assertEquals(3, iconLabels.size, "should have 3 status icons")
        }

    @Test
    fun `update modifies existing plan row in place`() =
        runOnEdt {
            val context = createRowContext()
            val adapter = PlanRowAdapter()
            val initialEntries =
                listOf(
                    PlanEntry("Task one", PlanEntryStatus.PENDING, PlanEntryPriority.MEDIUM),
                )
            val block1 = TranscriptBlock.PlanBlock("1", "p1", initialEntries)
            val row = adapter.create(context, block1, {})

            assertEquals(1, findEntryLabels(row).size, "initial entry count")

            val updatedEntries =
                listOf(
                    PlanEntry("Task one", PlanEntryStatus.COMPLETED, PlanEntryPriority.MEDIUM),
                    PlanEntry("Task two", PlanEntryStatus.PENDING, PlanEntryPriority.MEDIUM),
                )
            val block2 = TranscriptBlock.PlanBlock("1", "p1", updatedEntries)
            assertTrue(adapter.update(context, row, block2))

            val updatedLabels = findEntryLabels(row)
            assertEquals(2, updatedLabels.size, "updated entry count")
        }

    @Test
    fun `update returns false on type mismatch`() =
        runOnEdt {
            val context = createRowContext()
            val adapter = PlanRowAdapter()
            val row =
                adapter.create(
                    context,
                    TranscriptBlock.PlanBlock("1", "p1", emptyList()),
                    {},
                )

            assertFalse(adapter.update(context, row, TranscriptBlock.PlainLine("2", "plain")))
        }

    @Test
    fun `dispose does not throw`() =
        runOnEdt {
            val context = createRowContext()
            val adapter = PlanRowAdapter()
            val row =
                adapter.create(
                    context,
                    TranscriptBlock.PlanBlock("1", "p1", emptyList()),
                    {},
                )

            adapter.dispose(context, row)
        }

    @Test
    fun `factory creates plan row via adapter`() =
        runOnEdt {
            val factory = TranscriptBlockViewFactory(PlainMonospaceTranscriptCodeBlockViewFactory)
            val entries =
                listOf(
                    PlanEntry("Step 1", PlanEntryStatus.PENDING, PlanEntryPriority.HIGH),
                )
            val block = TranscriptBlock.PlanBlock("1", "p1", entries)
            val row = factory.create(block, {})

            assertTrue(isPlanRow(row))
            assertTrue(row is PlanPanel)
        }

    @Test
    fun `factory updates plan row via adapter`() =
        runOnEdt {
            val factory = TranscriptBlockViewFactory(PlainMonospaceTranscriptCodeBlockViewFactory)
            val initialEntries =
                listOf(
                    PlanEntry("Original", PlanEntryStatus.PENDING, PlanEntryPriority.MEDIUM),
                )
            val block1 = TranscriptBlock.PlanBlock("1", "p1", initialEntries)
            val row = factory.create(block1, {})

            assertEquals(1, findEntryLabels(row).size)

            val updatedEntries =
                listOf(
                    PlanEntry("Original", PlanEntryStatus.COMPLETED, PlanEntryPriority.MEDIUM),
                    PlanEntry("Added", PlanEntryStatus.PENDING, PlanEntryPriority.MEDIUM),
                )
            val block2 = TranscriptBlock.PlanBlock("1", "p1", updatedEntries)
            factory.update(row, block2)

            assertEquals(2, findEntryLabels(row).size)
        }

    @Test
    fun `factory disposeRow handles plan row`() =
        runOnEdt {
            val factory = TranscriptBlockViewFactory(PlainMonospaceTranscriptCodeBlockViewFactory)
            val block = TranscriptBlock.PlanBlock("1", "p1", emptyList())
            val row = factory.create(block, {})

            assertTrue(isPlanRow(row))
            factory.disposeRow(row)
        }

    @Test
    fun `plan row has reasonable preferred height`() =
        runOnEdt {
            val context = createRowContext()
            val adapter = PlanRowAdapter()
            val entries =
                listOf(
                    PlanEntry("Task", PlanEntryStatus.PENDING, PlanEntryPriority.MEDIUM),
                )
            val block = TranscriptBlock.PlanBlock("1", "p1", entries)
            val row = adapter.create(context, block, {})
            row.setSize(600, 0)
            row.doLayout()

            val height = row.preferredSize.height
            assertTrue(height > 0 && height < 200, "expected reasonable height, got $height")
        }

    @Test
    fun `empty plan row renders header only`() =
        runOnEdt {
            val context = createRowContext()
            val adapter = PlanRowAdapter()
            val block = TranscriptBlock.PlanBlock("1", "p1", emptyList())
            val row = adapter.create(context, block, {})

            val entryLabels = findEntryLabels(row)
            assertEquals(0, entryLabels.size, "empty plan has no entries")
            val labels = findLabels(row)
            val headerLabel = labels.firstOrNull { it.text.orEmpty().contains("Plan") }
            assertTrue(headerLabel != null, "should have Plan header")
        }

    private fun createRowContext(): RowContext =
        RowContext(
            columnWidth = 600,
            codeBlockViewFactory = PlainMonospaceTranscriptCodeBlockViewFactory,
            colorProvider = DefaultTranscriptColorProvider(),
            logContextProvider = { null },
        )

    private fun findLabels(row: javax.swing.JPanel): List<JLabel> {
        val labels = mutableListOf<JLabel>()

        fun traverse(component: JComponent) {
            if (component is JLabel) labels.add(component)
            component.components.filterIsInstance<JComponent>().forEach(::traverse)
        }
        traverse(row)
        return labels
    }

    private fun findEntryLabels(row: javax.swing.JPanel): List<JLabel> =
        findLabels(row).filter { it.text.orEmpty().matches(Regex("^\\d+\\..*")) }

    private fun runOnEdt(action: () -> Unit) {
        if (SwingUtilities.isEventDispatchThread()) {
            action()
        } else {
            SwingUtilities.invokeAndWait(action)
        }
    }
}
