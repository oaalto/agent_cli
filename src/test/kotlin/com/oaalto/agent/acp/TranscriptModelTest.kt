@file:Suppress("OPT_IN_USAGE")

package com.oaalto.agent.acp

import com.agentclientprotocol.model.ToolCallStatus
import com.agentclientprotocol.model.ToolKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class TranscriptModelTest {
    @Test
    fun `tool call and update merge into one block`() {
        val model = TranscriptModel()

        model.apply(
            StructuredUpdate.StartOrUpdateToolCall(
                toolCallId = "1",
                title = "read README.md",
                kind = ToolKind.READ,
                status = ToolCallStatus.IN_PROGRESS,
            ),
        )
        model.apply(
            StructuredUpdate.StartOrUpdateToolCall(
                toolCallId = "1",
                title = "read README.md",
                kind = ToolKind.READ,
                status = ToolCallStatus.COMPLETED,
                contentFragments = listOf("<pre>content</pre>"),
            ),
        )

        val blocks = model.blocks()
        assertEquals(1, blocks.size)
        val tool = assertIs<TranscriptBlock.ToolCallBlock>(blocks.single())
        assertEquals("1", tool.toolCallId)
        assertEquals(ToolCallStatus.COMPLETED, tool.status)
        assertEquals(listOf("<pre>content</pre>"), tool.contentFragments)
        assertFalse(tool.expanded)
    }

    @Test
    fun `two tool ids produce two blocks in order`() {
        val model = TranscriptModel()

        model.apply(
            StructuredUpdate.StartOrUpdateToolCall(
                toolCallId = "a",
                title = "first",
                kind = ToolKind.READ,
                status = ToolCallStatus.IN_PROGRESS,
            ),
        )
        model.apply(
            StructuredUpdate.StartOrUpdateToolCall(
                toolCallId = "b",
                title = "second",
                kind = ToolKind.EDIT,
                status = ToolCallStatus.IN_PROGRESS,
            ),
        )

        val tools = model.blocks().filterIsInstance<TranscriptBlock.ToolCallBlock>()
        assertEquals(listOf("a", "b"), tools.map { it.toolCallId })
    }

    @Test
    fun `agent stream finalizes before tool block`() {
        val model = TranscriptModel()

        model.apply(StructuredUpdate.AppendAgentText("Thinking"))
        model.apply(StructuredUpdate.FinalizeAgentStream)
        model.apply(
            StructuredUpdate.StartOrUpdateToolCall(
                toolCallId = "1",
                title = "read",
                kind = ToolKind.READ,
                status = ToolCallStatus.IN_PROGRESS,
            ),
        )

        val blocks = model.blocks()
        assertEquals(2, blocks.size)
        assertIs<TranscriptBlock.FinalAgentText>(blocks[0])
        assertIs<TranscriptBlock.ToolCallBlock>(blocks[1])
    }

    @Test
    fun `agent chunks append to active streaming block`() {
        val model = TranscriptModel()

        model.apply(StructuredUpdate.AppendAgentText("Hel"))
        model.apply(StructuredUpdate.AppendAgentText("lo"))

        val block = assertIs<TranscriptBlock.StreamingAgentText>(model.blocks().single())
        assertEquals("Hello", block.text)
    }

    @Test
    fun `finalize converts streaming block to final agent text`() {
        val model = TranscriptModel()

        model.apply(StructuredUpdate.AppendAgentText("Done"))
        model.apply(StructuredUpdate.FinalizeAgentStream)

        val block = assertIs<TranscriptBlock.FinalAgentText>(model.blocks().single())
        assertEquals("Done", block.text)
    }

    @Test
    fun `expansion state survives tool status update`() {
        val model = TranscriptModel()

        model.apply(
            StructuredUpdate.StartOrUpdateToolCall(
                toolCallId = "1",
                title = "read",
                kind = ToolKind.READ,
                status = ToolCallStatus.COMPLETED,
                contentFragments = listOf("<pre>body</pre>"),
            ),
        )
        model.toggleToolExpansion("1")
        model.apply(
            StructuredUpdate.StartOrUpdateToolCall(
                toolCallId = "1",
                title = "read",
                kind = ToolKind.READ,
                status = ToolCallStatus.FAILED,
                contentFragments = listOf("<pre>error</pre>"),
            ),
        )

        val tool = assertIs<TranscriptBlock.ToolCallBlock>(model.blocks().single())
        assertTrue(tool.expanded)
        assertEquals(ToolCallStatus.FAILED, tool.status)
        assertEquals(2, tool.contentFragments.size)
    }

    @Test
    fun `auth failure uses dedicated block type`() {
        val model = TranscriptModel()

        model.apply(StructuredUpdate.AppendAuthFailure("token expired"))

        val block = assertIs<TranscriptBlock.AuthFailureLine>(model.blocks().single())
        assertEquals("token expired", block.message)
    }

    @Test
    fun `toggle ignored when tool has no body content`() {
        val model = TranscriptModel()

        model.apply(
            StructuredUpdate.StartOrUpdateToolCall(
                toolCallId = "1",
                title = "read",
                kind = ToolKind.READ,
                status = ToolCallStatus.IN_PROGRESS,
            ),
        )
        model.toggleToolExpansion("1")

        val tool = assertIs<TranscriptBlock.ToolCallBlock>(model.blocks().single())
        assertFalse(tool.expanded)
    }
}
