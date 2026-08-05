package com.oaalto.agent.acp.transcript.model

import com.agentclientprotocol.model.ContentBlock
import com.agentclientprotocol.model.SessionUpdate
import com.agentclientprotocol.model.ToolCallId
import com.agentclientprotocol.model.ToolCallStatus
import com.agentclientprotocol.model.ToolKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class TranscriptFinalizePolicyTest {
    @Test
    fun `table chunk chunk tool update finalizes before tool mapping`() {
        val events =
            listOf(
                PolicyEvent.Ingest(SessionUpdate.AgentMessageChunk(content = ContentBlock.Text("a"))),
                PolicyEvent.Ingest(SessionUpdate.AgentMessageChunk(content = ContentBlock.Text("b"))),
                PolicyEvent.Ingest(
                    SessionUpdate.ToolCall(
                        toolCallId = ToolCallId("1"),
                        title = "read",
                        kind = ToolKind.READ,
                        status = ToolCallStatus.IN_PROGRESS,
                    ),
                ),
            )

        val updates = applyPolicyEvents(events)
        val finalizeIndices = updates.finalizeIndices()

        assertEquals(1, finalizeIndices.size)
        assertTrue(finalizeIndices.single() < updates.indexOfFirst { it is StructuredUpdate.StartOrUpdateToolCall })
    }

    @Test
    fun `table chunk prompt complete finalizes on response hook`() {
        val events =
            listOf(
                PolicyEvent.Ingest(SessionUpdate.AgentMessageChunk(content = ContentBlock.Text("reply"))),
                PolicyEvent.PromptResponse,
            )

        val updates = applyPolicyEvents(events)
        val finalizeIndices = updates.finalizeIndices()

        assertEquals(1, finalizeIndices.size)
        assertTrue(finalizeIndices.single() > updates.indexOfFirst { it is StructuredUpdate.AppendAgentText })
    }

    @Test
    fun `table chunk error finalizes on prompt failed hook`() {
        val events =
            listOf(
                PolicyEvent.Ingest(SessionUpdate.AgentMessageChunk(content = ContentBlock.Text("partial"))),
                PolicyEvent.PromptFailed,
            )

        val updates = applyPolicyEvents(events)
        val finalizeIndices = updates.finalizeIndices()

        assertEquals(1, finalizeIndices.size)
        assertTrue(finalizeIndices.single() > updates.indexOfFirst { it is StructuredUpdate.AppendAgentText })
    }

    @Test
    fun `blank non chunk still finalizes with empty mapped updates`() {
        val updates =
            TranscriptEventIngestion.ingest(
                SessionUpdate.AgentThoughtChunk(content = ContentBlock.Text("   ")),
            )

        assertEquals(1, updates.size)
        assertIs<StructuredUpdate.FinalizeAgentStream>(updates.single())
    }

    @Test
    fun `redundant finalize is idempotent via model`() {
        val model = TranscriptModel()
        model.apply(StructuredUpdate.AppendAgentText("streaming"))
        TranscriptFinalizePolicy.onPromptResponse().forEach(model::apply)
        TranscriptFinalizePolicy.onPromptFlowCompleted().forEach(model::apply)

        val blocks = model.blocks()
        assertEquals(1, blocks.size)
        assertIs<TranscriptBlock.FinalAgentText>(blocks.single())
    }

    @Test
    fun `finalize prelude skips agent message chunks`() {
        val prelude =
            TranscriptFinalizePolicy.finalizePrelude(
                SessionUpdate.AgentMessageChunk(content = ContentBlock.Text("chunk")),
            )

        assertTrue(prelude.isEmpty())
    }

    @Test
    fun `prompt lifecycle hooks each emit single finalize`() {
        assertEquals(1, TranscriptFinalizePolicy.onPromptStarting().size)
        assertEquals(1, TranscriptFinalizePolicy.onPromptResponse().size)
        assertEquals(1, TranscriptFinalizePolicy.onPromptFlowCompleted().size)
        assertEquals(1, TranscriptFinalizePolicy.onPromptFailed().size)
        assertEquals(1, TranscriptFinalizePolicy.onPromptInterrupted().size)
        assertIs<StructuredUpdate.FinalizeAgentStream>(TranscriptFinalizePolicy.onPromptStarting().single())
    }

    private sealed interface PolicyEvent {
        data class Ingest(
            val update: SessionUpdate,
        ) : PolicyEvent

        data object PromptResponse : PolicyEvent

        data object PromptFailed : PolicyEvent
    }

    private fun applyPolicyEvents(events: List<PolicyEvent>): List<StructuredUpdate> =
        events.flatMap { event ->
            when (event) {
                is PolicyEvent.Ingest -> TranscriptEventIngestion.ingest(event.update)
                PolicyEvent.PromptResponse -> TranscriptFinalizePolicy.onPromptResponse()
                PolicyEvent.PromptFailed -> TranscriptFinalizePolicy.onPromptFailed()
            }
        }

    private fun List<StructuredUpdate>.finalizeIndices(): List<Int> =
        mapIndexedNotNull { index, update ->
            if (update is StructuredUpdate.FinalizeAgentStream) index else null
        }
}
