package com.oaalto.agent.acp

import com.agentclientprotocol.model.ToolCallStatus
import com.agentclientprotocol.model.ToolKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TranscriptTextSerializerTest {
    @Test
    fun `empty model produces empty string`() {
        assertEquals("", TranscriptTextSerializer.serialize(emptyList()))
    }

    @Test
    fun `user echo prefixed with greater than`() {
        val blocks =
            listOf(
                TranscriptBlock.UserEcho("block-1", "hello"),
            )
        assertEquals("> hello", TranscriptTextSerializer.serialize(blocks))
    }

    @Test
    fun `thought blocks included with thought prefix`() {
        val blocks =
            listOf(
                TranscriptBlock.Thought("block-1", "reasoning step"),
            )
        assertEquals("[thought] reasoning step", TranscriptTextSerializer.serialize(blocks))
    }

    @Test
    fun `agent text normalized across line endings`() {
        val blocks =
            listOf(
                TranscriptBlock.FinalAgentText("block-1", "line one\r\nline two"),
            )
        assertEquals("line one\nline two", TranscriptTextSerializer.serialize(blocks))
    }

    @Test
    fun `streaming and final agent text serialize equivalently`() {
        val streaming = listOf(TranscriptBlock.StreamingAgentText("block-1", "same text"))
        val final = listOf(TranscriptBlock.FinalAgentText("block-2", "same text"))
        assertEquals(
            TranscriptTextSerializer.serialize(streaming),
            TranscriptTextSerializer.serialize(final),
        )
    }

    @Test
    fun `plain stderr line preserved`() {
        val blocks =
            listOf(
                TranscriptBlock.PlainLine("block-1", "[stderr] warning from agent"),
            )
        assertEquals("[stderr] warning from agent", TranscriptTextSerializer.serialize(blocks))
    }

    @Test
    fun `error and auth failure lines use renderer format`() {
        val blocks =
            listOf(
                TranscriptBlock.ErrorLine("block-1", "connection lost"),
                TranscriptBlock.AuthFailureLine("block-2", "token expired"),
            )
        val text = TranscriptTextSerializer.serialize(blocks)
        assertTrue(text.contains("Error: connection lost"))
        assertTrue(text.contains("Auth failed: token expired"))
    }

    @Test
    fun `tool calls serialize as single header lines without body`() {
        val blocks =
            listOf(
                TranscriptBlock.ToolCallBlock(
                    blockId = "block-1",
                    toolCallId = "1",
                    title = "read README.md",
                    kind = ToolKind.READ,
                    status = ToolCallStatus.COMPLETED,
                    bodyParts = listOf(TranscriptBodyPart.Html("<pre>secret</pre>")),
                ),
                TranscriptBlock.ToolCallBlock(
                    blockId = "block-2",
                    toolCallId = "2",
                    title = "run tests",
                    kind = ToolKind.EXECUTE,
                    status = ToolCallStatus.FAILED,
                    bodyParts = emptyList(),
                ),
            )
        val text = TranscriptTextSerializer.serialize(blocks)
        assertEquals(
            "[tool: read README.md completed]\n[tool: run tests failed]",
            text,
        )
        assertFalse(text.contains("secret"))
    }

    @Test
    fun `plan blocks omitted from serialization`() {
        val blocks =
            listOf(
                TranscriptBlock.UserEcho("block-1", "go"),
                TranscriptBlock.PlanBlock(
                    blockId = "block-2",
                    planId = "plan-1",
                    entries =
                        listOf(
                            PlanEntry("step", PlanEntryStatus.PENDING, PlanEntryPriority.HIGH),
                        ),
                ),
                TranscriptBlock.FinalAgentText("block-3", "done"),
            )
        assertEquals("> go\ndone", TranscriptTextSerializer.serialize(blocks))
    }

    @Test
    fun `multiple tools preserve order`() {
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
                status = ToolCallStatus.COMPLETED,
            ),
        )
        val text = TranscriptTextSerializer.serialize(model.blocks())
        assertEquals(
            "[tool: first in progress]\n[tool: second completed]",
            text,
        )
    }
}
