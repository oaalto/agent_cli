@file:Suppress("OPT_IN_USAGE")

package com.oaalto.agent.acp

import com.agentclientprotocol.model.ContentBlock
import com.agentclientprotocol.model.SessionUpdate
import com.agentclientprotocol.model.ToolCallContent
import com.agentclientprotocol.model.ToolCallId
import com.agentclientprotocol.model.ToolCallStatus
import com.agentclientprotocol.model.ToolKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class TranscriptSessionUpdateMapperTest {
    @Test
    fun `maps agent chunk to append text`() {
        val update =
            TranscriptSessionUpdateMapper.mapAgentChunk(
                SessionUpdate.AgentMessageChunk(content = ContentBlock.Text("Hello")),
            )

        assertEquals(StructuredUpdate.AppendAgentText("Hello"), update)
    }

    @Test
    fun `maps tool lifecycle to single start or update`() {
        val updates =
            TranscriptSessionUpdateMapper.mapUpdate(
                SessionUpdate.ToolCallUpdate(
                    toolCallId = ToolCallId("1"),
                    title = "read README.md",
                    kind = ToolKind.READ,
                    status = ToolCallStatus.COMPLETED,
                    content = listOf(ToolCallContent.Content(ContentBlock.Text("# README"))),
                ),
            )

        assertEquals(1, updates.size)
        val mapped = assertIs<StructuredUpdate.StartOrUpdateToolCall>(updates.single())
        assertEquals("1", mapped.toolCallId)
        assertEquals(ToolCallStatus.COMPLETED, mapped.status)
        assertTrue(
            mapped.bodyParts.any {
                it is TranscriptBodyPart.Html && it.fragment.contains("README")
            },
        )
    }

    @Test
    fun `maps in progress tool update without body fragments`() {
        val updates =
            TranscriptSessionUpdateMapper.mapUpdate(
                SessionUpdate.ToolCallUpdate(
                    toolCallId = ToolCallId("1"),
                    title = "read",
                    kind = ToolKind.READ,
                    status = ToolCallStatus.IN_PROGRESS,
                    content = listOf(ToolCallContent.Content(ContentBlock.Text("early"))),
                ),
            )

        val mapped = assertIs<StructuredUpdate.StartOrUpdateToolCall>(updates.single())
        assertTrue(mapped.bodyParts.isEmpty())
    }

    @Test
    fun `chronological session sequence deduplicates in model`() {
        val model = TranscriptModel()
        val updates =
            listOf(
                SessionUpdate.AgentMessageChunk(content = ContentBlock.Text("Thinking")),
                SessionUpdate.ToolCall(
                    toolCallId = ToolCallId("1"),
                    title = "read README.md",
                    kind = ToolKind.READ,
                    status = ToolCallStatus.IN_PROGRESS,
                ),
                SessionUpdate.ToolCallUpdate(
                    toolCallId = ToolCallId("1"),
                    title = "read README.md",
                    kind = ToolKind.READ,
                    status = ToolCallStatus.COMPLETED,
                    content = listOf(ToolCallContent.Content(ContentBlock.Text("# README\ncontent"))),
                ),
                SessionUpdate.AgentMessageChunk(content = ContentBlock.Text("Done")),
            )

        updates.forEach { update ->
            when (update) {
                is SessionUpdate.AgentMessageChunk -> {
                    TranscriptSessionUpdateMapper.mapAgentChunk(update)?.let(model::apply)
                }
                else -> {
                    model.apply(StructuredUpdate.FinalizeAgentStream)
                    TranscriptSessionUpdateMapper.mapUpdate(update).forEach(model::apply)
                }
            }
        }
        model.apply(StructuredUpdate.FinalizeAgentStream)

        val blocks = model.blocks()
        assertEquals(3, blocks.size)
        assertIs<TranscriptBlock.FinalAgentText>(blocks[0])
        val tool = assertIs<TranscriptBlock.ToolCallBlock>(blocks[1])
        assertEquals("1", tool.toolCallId)
        assertEquals(ToolCallStatus.COMPLETED, tool.status)
        assertTrue(
            tool.bodyParts.any {
                it is TranscriptBodyPart.Html && it.fragment.contains("README")
            },
        )
        assertIs<TranscriptBlock.FinalAgentText>(blocks[2])
    }
}
