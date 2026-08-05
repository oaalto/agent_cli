package com.oaalto.agent.acp

import com.agentclientprotocol.model.ToolCallStatus
import com.agentclientprotocol.model.ToolKind
import com.oaalto.agent.acp.transcript.model.TranscriptBlock
import com.oaalto.agent.acp.transcript.model.TranscriptBodyPart
import com.oaalto.agent.acp.transcript.theme.DefaultTranscriptColorProvider
import javax.swing.SwingUtilities
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ToolCallRowAdapterTest {
    @Test
    fun `matches returns true for ToolCallBlock`() {
        val adapter = ToolCallRowAdapter()
        assertTrue(
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
    fun `matches returns false for non-tool block types`() {
        val adapter = ToolCallRowAdapter()
        assertFalse(adapter.matches(TranscriptBlock.UserEcho("1", "echo")))
        assertFalse(adapter.matches(TranscriptBlock.PlainLine("1", "plain")))
        assertFalse(adapter.matches(TranscriptBlock.FinalAgentText("1", "text")))
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
    fun `collapsed tool card renders default collapsed state`() =
        runOnEdt {
            val context = createRowContext()
            val adapter = ToolCallRowAdapter()
            val block =
                TranscriptBlock.ToolCallBlock(
                    blockId = "1",
                    toolCallId = "t1",
                    title = "read_file",
                    kind = ToolKind.READ,
                    status = ToolCallStatus.IN_PROGRESS,
                    bodyParts = emptyList(),
                )
            val row = adapter.create(context, block, {})

            assertTrue(isToolCallRow(row))
            assertNotNull(row.getClientProperty(TOOL_CALL_ROW_MARKER))
        }

    @Test
    fun `expanded tool card renders body with code editors`() =
        runOnEdt {
            val context = createRowContext()
            val adapter = ToolCallRowAdapter()
            val block =
                TranscriptBlock.ToolCallBlock(
                    blockId = "1",
                    toolCallId = "t1",
                    title = "execute_code",
                    kind = ToolKind.EDIT,
                    status = ToolCallStatus.COMPLETED,
                    expanded = true,
                    bodyParts = listOf(TranscriptBodyPart.Code("python", "print('hello')")),
                )
            val row = adapter.create(context, block, {})
            row.setSize(600, 0)
            row.doLayout()

            assertTrue(isToolCallRow(row))
            val codeComponents = (row as CollapsibleToolPanel).disposableCodeComponents
            assertEquals(1, codeComponents.size)
            assertTrue(row.preferredSize.height > 0)
        }

    @Test
    fun `child count stable across update`() =
        runOnEdt {
            val context = createRowContext()
            val adapter = ToolCallRowAdapter()
            val block1 =
                TranscriptBlock.ToolCallBlock(
                    blockId = "1",
                    toolCallId = "t1",
                    title = "read_file",
                    kind = ToolKind.READ,
                    status = ToolCallStatus.IN_PROGRESS,
                    bodyParts = emptyList(),
                )
            val block2 =
                TranscriptBlock.ToolCallBlock(
                    blockId = "1",
                    toolCallId = "t1",
                    title = "read_file",
                    kind = ToolKind.READ,
                    status = ToolCallStatus.COMPLETED,
                    bodyParts = emptyList(),
                )
            val row = adapter.create(context, block1, {})
            val initialChildCount = row.componentCount

            assertTrue(adapter.update(context, row, block2))
            assertEquals(initialChildCount, row.componentCount, "child count should not change on update")
        }

    @Test
    fun `update returns false on type mismatch`() =
        runOnEdt {
            val context = createRowContext()
            val adapter = ToolCallRowAdapter()
            val row =
                adapter.create(
                    context,
                    TranscriptBlock.ToolCallBlock(
                        blockId = "1",
                        toolCallId = "t1",
                        title = "tool",
                        kind = null,
                        status = null,
                        bodyParts = emptyList(),
                    ),
                    {},
                )

            assertFalse(adapter.update(context, row, TranscriptBlock.PlainLine("2", "plain")))
        }

    @Test
    fun `dispose does not throw`() =
        runOnEdt {
            val context = createRowContext()
            val adapter = ToolCallRowAdapter()
            val row =
                adapter.create(
                    context,
                    TranscriptBlock.ToolCallBlock(
                        blockId = "1",
                        toolCallId = "t1",
                        title = "tool",
                        kind = null,
                        status = null,
                        bodyParts = emptyList(),
                    ),
                    {},
                )

            adapter.dispose(context, row)
        }

    @Test
    fun `dispose clears code editors for expanded tool card`() =
        runOnEdt {
            val context = createRowContext()
            val adapter = ToolCallRowAdapter()
            val block =
                TranscriptBlock.ToolCallBlock(
                    blockId = "1",
                    toolCallId = "t1",
                    title = "execute_code",
                    kind = ToolKind.EDIT,
                    status = ToolCallStatus.COMPLETED,
                    expanded = true,
                    bodyParts =
                        listOf(
                            TranscriptBodyPart.Code("python", "print('hello')"),
                            TranscriptBodyPart.Code("javascript", "console.log('world')"),
                        ),
                )
            val row = adapter.create(context, block, {})
            assertEquals(2, (row as CollapsibleToolPanel).disposableCodeComponents.size)

            adapter.dispose(context, row)
            assertEquals(0, row.disposableCodeComponents.size)
        }

    @Test
    fun `expanded tool card skips body rebuild when only header changes`() =
        runOnEdt {
            val recFactory = RecordingCodeBlockViewFactory()
            val ctx =
                RowContext(
                    columnWidth = 600,
                    codeBlockViewFactory = recFactory,
                    colorProvider = DefaultTranscriptColorProvider(),
                    logContextProvider = { null },
                )
            val adapter = ToolCallRowAdapter()
            val bodyParts = listOf(TranscriptBodyPart.Code("kotlin", "fun main()"))
            val row =
                adapter.create(
                    ctx,
                    TranscriptBlock.ToolCallBlock(
                        blockId = "1",
                        toolCallId = "tool-1",
                        title = "read file",
                        kind = ToolKind.READ,
                        status = ToolCallStatus.COMPLETED,
                        bodyParts = bodyParts,
                        expanded = true,
                    ),
                    onToolToggle = {},
                )

            assertEquals(1, recFactory.createdCount)

            adapter.update(
                ctx,
                row,
                TranscriptBlock.ToolCallBlock(
                    blockId = "1",
                    toolCallId = "tool-1",
                    title = "read README.md",
                    kind = ToolKind.READ,
                    status = ToolCallStatus.COMPLETED,
                    bodyParts = bodyParts,
                    expanded = true,
                ),
            )

            assertEquals(1, recFactory.createdCount)
        }

    @Test
    fun `expanded tool card body remeasures children on resize`() =
        runOnEdt {
            val recFactory = RecordingCodeBlockViewFactory()
            val ctx =
                RowContext(
                    columnWidth = 600,
                    codeBlockViewFactory = recFactory,
                    colorProvider = DefaultTranscriptColorProvider(),
                    logContextProvider = { null },
                )
            val adapter = ToolCallRowAdapter()
            val block =
                TranscriptBlock.ToolCallBlock(
                    blockId = "1",
                    toolCallId = "t1",
                    title = "execute",
                    kind = ToolKind.EDIT,
                    status = ToolCallStatus.COMPLETED,
                    expanded = true,
                    bodyParts = listOf(TranscriptBodyPart.Code("kotlin", "fun main()")),
                )
            val row = adapter.create(ctx, block, {})

            val initialHeight = row.preferredSize.height

            // Resize row wider
            row.setSize(800, 0)
            row.doLayout()

            val widerHeight = row.preferredSize.height
            assertTrue(
                widerHeight <= initialHeight,
                "wider column should not increase height for single-line code block",
            )

            // Code block width should update
            val codeComponent = (row as CollapsibleToolPanel).disposableCodeComponents.first()
            assertTrue(codeComponent.preferredSize.width > 0)
        }

    @Test
    fun `collapsed tool card skips code-block component creation`() =
        runOnEdt {
            val recFactory = RecordingCodeBlockViewFactory()
            val ctx =
                RowContext(
                    columnWidth = 600,
                    codeBlockViewFactory = recFactory,
                    colorProvider = DefaultTranscriptColorProvider(),
                    logContextProvider = { null },
                )
            val adapter = ToolCallRowAdapter()
            val block =
                TranscriptBlock.ToolCallBlock(
                    blockId = "1",
                    toolCallId = "t1",
                    title = "execute",
                    kind = ToolKind.EDIT,
                    status = ToolCallStatus.COMPLETED,
                    expanded = false,
                    bodyParts = listOf(TranscriptBodyPart.Code("kotlin", "fun main()")),
                )
            val row = adapter.create(ctx, block, {})

            // Collapsed card should not create code components
            assertEquals(0, recFactory.createdCount)
            assertEquals(0, (row as CollapsibleToolPanel).disposableCodeComponents.size)
        }

    @Test
    fun `factory creates tool row via adapter`() =
        runOnEdt {
            val factory = TranscriptBlockViewFactory(PlainMonospaceTranscriptCodeBlockViewFactory)
            val block =
                TranscriptBlock.ToolCallBlock(
                    blockId = "1",
                    toolCallId = "t1",
                    title = "test tool",
                    kind = ToolKind.READ,
                    status = ToolCallStatus.COMPLETED,
                    bodyParts = emptyList(),
                )
            val row = factory.create(block, {})

            assertTrue(isToolCallRow(row))
            assertTrue(row is CollapsibleToolPanel)
        }

    @Test
    fun `factory disposes tool row code components`() =
        runOnEdt {
            val factory = TranscriptBlockViewFactory(PlainMonospaceTranscriptCodeBlockViewFactory)
            val block =
                TranscriptBlock.ToolCallBlock(
                    blockId = "1",
                    toolCallId = "t1",
                    title = "execute_code",
                    kind = ToolKind.EDIT,
                    status = ToolCallStatus.COMPLETED,
                    expanded = true,
                    bodyParts = listOf(TranscriptBodyPart.Code("python", "print('hello')")),
                )
            val row = factory.create(block, {})
            assertEquals(1, (row as CollapsibleToolPanel).disposableCodeComponents.size)
            factory.disposeRow(row)
            assertEquals(0, row.disposableCodeComponents.size)
        }

    private fun createRowContext(): RowContext =
        RowContext(
            columnWidth = 600,
            codeBlockViewFactory = PlainMonospaceTranscriptCodeBlockViewFactory,
            colorProvider = DefaultTranscriptColorProvider(),
            logContextProvider = { null },
        )

    private fun runOnEdt(action: () -> Unit) {
        if (SwingUtilities.isEventDispatchThread()) {
            action()
        } else {
            SwingUtilities.invokeAndWait(action)
        }
    }
}
