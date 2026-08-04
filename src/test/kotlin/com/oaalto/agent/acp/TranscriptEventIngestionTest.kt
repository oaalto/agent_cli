package com.oaalto.agent.acp

import com.agentclientprotocol.model.AvailableCommand
import com.agentclientprotocol.model.AvailableCommandInput
import com.agentclientprotocol.model.ContentBlock
import com.agentclientprotocol.model.Cost
import com.agentclientprotocol.model.SessionUpdate
import com.agentclientprotocol.model.ToolCallContent
import com.agentclientprotocol.model.ToolCallId
import com.agentclientprotocol.model.ToolCallStatus
import com.agentclientprotocol.model.ToolKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TranscriptEventIngestionTest {
    @Test
    fun `ingest agent chunk returns single append text without finalize`() {
        val updates =
            TranscriptEventIngestion.ingest(
                SessionUpdate.AgentMessageChunk(content = ContentBlock.Text("Hello")),
            )

        assertEquals(1, updates.size)
        assertEquals(StructuredUpdate.AppendAgentText("Hello"), updates.single())
    }

    @Test
    fun `ingest non chunk includes finalize before mapping`() {
        val updates =
            TranscriptEventIngestion.ingest(
                SessionUpdate.ToolCallUpdate(
                    toolCallId = ToolCallId("1"),
                    title = "read README.md",
                    kind = ToolKind.READ,
                    status = ToolCallStatus.COMPLETED,
                    content = listOf(ToolCallContent.Content(ContentBlock.Text("# README"))),
                ),
            )

        assertTrue(updates.size >= 2)
        assertIs<StructuredUpdate.FinalizeAgentStream>(updates[0])
        val mapped = assertIs<StructuredUpdate.StartOrUpdateToolCall>(updates[1])
        assertEquals("1", mapped.toolCallId)
        assertEquals(ToolCallStatus.COMPLETED, mapped.status)
    }

    @Test
    fun `ingest prompt completed returns single finalize`() {
        val updates = TranscriptEventIngestion.ingestPromptCompleted()

        assertEquals(1, updates.size)
        assertIs<StructuredUpdate.FinalizeAgentStream>(updates.single())
    }

    @Test
    fun `maps tool lifecycle to single start or update`() {
        val updates =
            TranscriptEventIngestion.mapUpdate(
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
            TranscriptEventIngestion.mapUpdate(
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
    fun `chronological session sequence finalizes before non chunk via ingest`() {
        val model = TranscriptModel()
        val updates =
            listOf<SessionUpdate>(
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
            TranscriptEventIngestion.ingest(update).forEach(model::apply)
        }
        TranscriptEventIngestion.ingestPromptCompleted().forEach(model::apply)

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

    @Test
    fun `finalize before agent thought chunk`() {
        val model = TranscriptModel()
        TranscriptEventIngestion
            .ingest(
                SessionUpdate.AgentMessageChunk(content = ContentBlock.Text("streaming")),
            ).forEach(model::apply)
        TranscriptEventIngestion
            .ingest(
                SessionUpdate.AgentThoughtChunk(content = ContentBlock.Text("reasoning")),
            ).forEach(model::apply)

        val blocks = model.blocks()
        // The streaming text should be finalized before the thought appears
        val finalizeIdx =
            blocks.indexOfFirst { it is TranscriptBlock.FinalAgentText }
        val thoughtIdx =
            blocks.indexOfFirst { it is TranscriptBlock.Thought }
        assertTrue(finalizeIdx >= 0, "expected FinalAgentText in blocks: $blocks")
        assertTrue(thoughtIdx >= 0, "expected ThoughtBlock in blocks: $blocks")
        assertTrue(
            finalizeIdx < thoughtIdx,
            "finalize must precede thought; finalize=$finalizeIdx thought=$thoughtIdx",
        )
    }

    @Test
    fun `finalize before user echo chunk`() {
        val model = TranscriptModel()
        TranscriptEventIngestion
            .ingest(
                SessionUpdate.AgentMessageChunk(content = ContentBlock.Text("streaming")),
            ).forEach(model::apply)
        TranscriptEventIngestion
            .ingest(
                SessionUpdate.UserMessageChunk(content = ContentBlock.Text("user said hi")),
            ).forEach(model::apply)

        val blocks = model.blocks()
        val finalizeIdx =
            blocks.indexOfFirst { it is TranscriptBlock.FinalAgentText }
        val echoIdx =
            blocks.indexOfFirst { it is TranscriptBlock.UserEcho }
        assertTrue(finalizeIdx >= 0, "expected FinalAgentText in blocks: $blocks")
        assertTrue(echoIdx >= 0, "expected UserEcho in blocks: $blocks")
        assertTrue(
            finalizeIdx < echoIdx,
            "finalize must precede user echo; finalize=$finalizeIdx echo=$echoIdx",
        )
    }

    @Test
    fun `maps usage update to structured usage`() {
        val updates =
            TranscriptEventIngestion.mapUpdate(
                SessionUpdate.UsageUpdate(
                    used = 1234,
                    size = 128000,
                    cost = null,
                ),
            )

        assertEquals(1, updates.size)
        val mapped = assertIs<StructuredUpdate.Usage>(updates.single())
        assertEquals(1234, mapped.used)
        assertEquals(128000, mapped.size)
        assertNull(mapped.cost)
    }

    @Test
    fun `maps usage update with cost to structured usage with cost`() {
        val updates =
            TranscriptEventIngestion.mapUpdate(
                SessionUpdate.UsageUpdate(
                    used = 5000,
                    size = 128000,
                    cost = Cost(amount = 0.0234, currency = "USD"),
                ),
            )

        assertEquals(1, updates.size)
        val mapped = assertIs<StructuredUpdate.Usage>(updates.single())
        assertEquals(5000, mapped.used)
        assertEquals(128000, mapped.size)
        assertEquals(0.0234, mapped.cost?.amount)
        assertEquals("USD", mapped.cost?.currency)
    }

    @Test
    fun `maps available commands update to structured commands`() {
        val updates =
            TranscriptEventIngestion.mapUpdate(
                SessionUpdate.AvailableCommandsUpdate(
                    availableCommands =
                        listOf(
                            AvailableCommand(
                                name = "web",
                                description = "Search the web",
                                input = AvailableCommandInput.Unstructured(hint = "query"),
                            ),
                            AvailableCommand(
                                name = "plan",
                                description = "Create a plan",
                            ),
                        ),
                ),
            )

        assertEquals(1, updates.size)
        val mapped = assertIs<StructuredUpdate.AvailableCommands>(updates.single())
        assertEquals(2, mapped.commands.size)
        assertEquals("web", mapped.commands[0].name)
        assertEquals("query", mapped.commands[0].inputHint)
        assertEquals("plan", mapped.commands[1].name)
        assertNull(mapped.commands[1].inputHint)
    }

    @Test
    fun `maps tool call update with null title without falling back to call id`() {
        val callId = "call-d6183ca2-ef5c-4b21-87f9-74b76b7e5958-0fc_dfe19c84-ba19-93ae-97e6-a9b096edd045_0"
        val updates =
            TranscriptEventIngestion.mapUpdate(
                SessionUpdate.ToolCallUpdate(
                    toolCallId = ToolCallId(callId),
                    title = null,
                    kind = ToolKind.SEARCH,
                    status = ToolCallStatus.COMPLETED,
                    content = emptyList(),
                ),
            )

        val mapped = assertIs<StructuredUpdate.StartOrUpdateToolCall>(updates.single())
        assertEquals("", mapped.title)
    }

    @Test
    fun `empty agent chunk produces no updates`() {
        val updates =
            TranscriptEventIngestion.ingest(
                SessionUpdate.AgentMessageChunk(content = ContentBlock.Text("   ")),
            )

        assertTrue(updates.isEmpty(), "blank agent chunks are filtered: $updates")
    }
}
