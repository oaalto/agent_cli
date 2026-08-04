package com.oaalto.agent.acp

import com.agentclientprotocol.model.ToolCallStatus
import com.agentclientprotocol.model.ToolKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TranscriptBlockViewFactoryTest {
    @Test
    fun `adapter order is specific-before-generic`() {
        val toolBlock =
            TranscriptBlock.ToolCallBlock(
                blockId = "1",
                toolCallId = "t1",
                title = "t",
                kind = null,
                status = null,
                bodyParts = emptyList(),
            )
        val planBlock = TranscriptBlock.PlanBlock("1", "p1", emptyList())
        val agentBlock = TranscriptBlock.FinalAgentText("1", "agent")
        val simpleBlock = TranscriptBlock.UserEcho("1", "echo")

        val factory = TranscriptBlockViewFactory(PlainMonospaceTranscriptCodeBlockViewFactory)

        val toolRow = factory.create(toolBlock, {})
        assertTrue(isToolCallRow(toolRow))

        val planRow = factory.create(planBlock, {})
        assertTrue(isPlanRow(planRow))

        val agentRow = factory.create(agentBlock, {})
        assertTrue(isAgentTextRow(agentRow))

        val simpleRow = factory.create(simpleBlock, {})
        assertTrue(isSimpleTextRow(simpleRow))
    }

    @Test
    fun `create dispatches to correct adapter for each block family`() {
        val factory = TranscriptBlockViewFactory(PlainMonospaceTranscriptCodeBlockViewFactory)

        val toolRow =
            factory.create(
                TranscriptBlock.ToolCallBlock(
                    blockId = "1",
                    toolCallId = "t1",
                    title = "t",
                    kind = ToolKind.READ,
                    status = ToolCallStatus.IN_PROGRESS,
                    bodyParts = emptyList(),
                ),
                onToolToggle = {},
            )
        assertTrue(toolRow is CollapsibleToolPanel)

        val planRow =
            factory.create(
                TranscriptBlock.PlanBlock(
                    "1",
                    "p1",
                    listOf(PlanEntry("e", PlanEntryStatus.PENDING, PlanEntryPriority.MEDIUM)),
                ),
                {},
            )
        assertTrue(planRow is com.oaalto.agent.acp.plan.PlanPanel)

        val streamRow = factory.create(TranscriptBlock.StreamingAgentText("1", "streaming"), {})
        assertTrue(isAgentTextRow(streamRow))

        assertTrue(isSimpleTextRow(factory.create(TranscriptBlock.UserEcho("1", "echo"), {})))
        assertTrue(isSimpleTextRow(factory.create(TranscriptBlock.Thought("1", "thinking"), {})))
        assertTrue(isSimpleTextRow(factory.create(TranscriptBlock.PlainLine("1", "plain"), {})))
        assertTrue(isSimpleTextRow(factory.create(TranscriptBlock.ErrorLine("1", "err"), {})))
        assertTrue(isSimpleTextRow(factory.create(TranscriptBlock.AuthFailureLine("1", "auth"), {})))
    }

    @Test
    fun `update dispatches to correct adapter`() {
        val factory = TranscriptBlockViewFactory(PlainMonospaceTranscriptCodeBlockViewFactory)

        val toolRow =
            factory.create(
                TranscriptBlock.ToolCallBlock(
                    blockId = "1",
                    toolCallId = "t1",
                    title = "before",
                    kind = ToolKind.READ,
                    status = ToolCallStatus.IN_PROGRESS,
                    bodyParts = emptyList(),
                ),
                {},
            )
        factory.update(
            toolRow,
            TranscriptBlock.ToolCallBlock(
                blockId = "1",
                toolCallId = "t1",
                title = "after",
                kind = ToolKind.READ,
                status = ToolCallStatus.COMPLETED,
                bodyParts = emptyList(),
            ),
        )

        val planRow =
            factory.create(
                TranscriptBlock.PlanBlock(
                    "1",
                    "p1",
                    listOf(PlanEntry("e", PlanEntryStatus.PENDING, PlanEntryPriority.MEDIUM)),
                ),
                {},
            )
        factory.update(
            planRow,
            TranscriptBlock.PlanBlock(
                "1",
                "p1",
                listOf(PlanEntry("e", PlanEntryStatus.COMPLETED, PlanEntryPriority.MEDIUM)),
            ),
        )

        val agentRow = factory.create(TranscriptBlock.StreamingAgentText("1", "Hel"), {})
        factory.update(agentRow, TranscriptBlock.StreamingAgentText("1", "Hello"))

        val simpleRow = factory.create(TranscriptBlock.PlainLine("1", "before"), {})
        factory.update(simpleRow, TranscriptBlock.PlainLine("1", "after"))
    }

    @Test
    fun `disposeRow dispatches to correct adapter`() {
        val recFactory = RecordingCodeBlockViewFactory()
        val factory = TranscriptBlockViewFactory(recFactory)

        val agentRow =
            factory.create(
                TranscriptBlock.FinalAgentText("1", "```kotlin\nfun main()\n```"),
                {},
            )
        factory.disposeRow(agentRow)
        assertEquals(1, recFactory.disposedCount)

        val simpleRow = factory.create(TranscriptBlock.PlainLine("1", "text"), {})
        factory.disposeRow(simpleRow)

        val toolRow =
            factory.create(
                TranscriptBlock.ToolCallBlock(
                    blockId = "1",
                    toolCallId = "t1",
                    title = "t",
                    kind = null,
                    status = null,
                    bodyParts = emptyList(),
                ),
                {},
            )
        factory.disposeRow(toolRow)

        val planRow = factory.create(TranscriptBlock.PlanBlock("1", "p1", emptyList()), {})
        factory.disposeRow(planRow)
    }

    @Test
    fun `update with type mismatch logs warning and preserves row`() {
        val factory = TranscriptBlockViewFactory(PlainMonospaceTranscriptCodeBlockViewFactory)
        val simpleRow = factory.create(TranscriptBlock.PlainLine("1", "text"), {})

        factory.update(simpleRow, TranscriptBlock.FinalAgentText("1", "agent text"))
        assertTrue(isSimpleTextRow(simpleRow))
    }

    @Test
    fun `companion object delegates helper functions`() {
        val escaped = TranscriptBlockViewFactory.escapeHtml("<script>alert(1)</script>")
        assertTrue(escaped.contains("&lt;") || escaped.contains("&amp;"))
    }
}
