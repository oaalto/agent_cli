@file:Suppress("OPT_IN_USAGE")

package com.oaalto.agent.acp

import com.agentclientprotocol.model.ContentBlock
import com.agentclientprotocol.model.SessionUpdate
import com.agentclientprotocol.model.ToolCallId
import com.agentclientprotocol.model.ToolCallStatus
import com.agentclientprotocol.model.ToolKind
import kotlin.test.Test
import kotlin.test.assertEquals

class TranscriptRendererTest {
    @Test
    fun `renders agent text chunks as incremental transcript text`() {
        val text =
            TranscriptRenderer.renderEventText(
                SessionUpdate.AgentMessageChunk(content = ContentBlock.Text("Hello")),
            )

        assertEquals("Hello", text)
    }

    @Test
    fun `renders tool call status lines in chronological update order`() {
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
                ),
                SessionUpdate.AgentMessageChunk(content = ContentBlock.Text("Done")),
            )

        val lines = updates.flatMap(TranscriptRenderer::renderUpdate)

        assertEquals(
            listOf(
                "Thinking",
                "[read] read README.md (in progress)",
                "[read] read README.md (completed)",
                "Done",
            ),
            lines,
        )
    }
}
