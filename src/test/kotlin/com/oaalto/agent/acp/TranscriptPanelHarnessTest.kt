package com.oaalto.agent.acp

import com.agentclientprotocol.model.ContentBlock
import com.agentclientprotocol.model.SessionUpdate
import com.agentclientprotocol.model.ToolCallContent
import com.agentclientprotocol.model.ToolCallId
import com.agentclientprotocol.model.ToolCallStatus
import com.agentclientprotocol.model.ToolKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Start here for ACP transcript panel UI regressions.
 *
 * Golden scenarios exercise the mounted [TranscriptPanel] through [TranscriptPanelTestHarness].
 * Row-adapter unit tests remain the fast seam for adapter dispatch.
 */
class TranscriptPanelHarnessTest {
    @Test
    fun `streaming finalize and code block rows have non zero height`() {
        val harness = TranscriptPanelTestHarness(columnWidth = 400)
        try {
            harness.apply(
                StructuredUpdate.AppendAgentText("Here is code:\n```kotlin\nfun main("),
                StructuredUpdate.AppendAgentText(") {}\n```"),
                StructuredUpdate.FinalizeAgentStream,
            )

            val codeHeights = harness.codeBlockPreferredHeights()
            assertTrue(codeHeights.isNotEmpty(), "expected at least one code block row after finalize")
            assertTrue(
                codeHeights.all { it > 0 },
                "code block rows must not collapse to zero height",
            )
        } finally {
            harness.dispose()
        }
    }

    @Test
    fun `apply keeps viewport at bottom when already following`() {
        val harness = TranscriptPanelTestHarness(columnWidth = 300, viewportHeight = 80)
        try {
            harness.apply(manyPlainLineUpdates(20))
            harness.scrollViewportToBottom()

            harness.apply(
                StructuredUpdate.AppendPlainLine("new line at the end"),
            )

            harness.assertViewportShowsBottom()
        } finally {
            harness.dispose()
        }
    }

    @Test
    fun `apply does not scroll when viewport is not at bottom`() {
        val harness = TranscriptPanelTestHarness(columnWidth = 300, viewportHeight = 80)
        try {
            harness.apply(manyPlainLineUpdates(20))
            harness.scrollViewportToTop()

            val scrollBefore = harness.verticalScrollBarValue()
            harness.apply(
                StructuredUpdate.AppendPlainLine("new line at the end"),
            )

            assertEquals(
                scrollBefore,
                harness.verticalScrollBarValue(),
                "scroll position changed while reading history",
            )
        } finally {
            harness.dispose()
        }
    }

    @Test
    fun `mounted panel owns a single transcript scroll pane`() {
        val harness = TranscriptPanelTestHarness()
        try {
            harness.apply(StructuredUpdate.AppendPlainLine("hello"))
            harness.assertSingleTranscriptScrollPane()
            harness.assertNoHorizontalScrollbar()
        } finally {
            harness.dispose()
        }
    }

    @Test
    fun `column resize reflow keeps prose and code rows visible`() {
        val harness = TranscriptPanelTestHarness(columnWidth = 400)
        try {
            harness.apply(
                StructuredUpdate.AppendAgentText("Prose before code.\n```kotlin\nfun main() {\n"),
                StructuredUpdate.AppendAgentText("  println(\"hi\")\n}\n```\nTrailing prose."),
                StructuredUpdate.FinalizeAgentStream,
            )

            harness.setColumnWidth(250)

            val heights = harness.rowPreferredHeights()
            assertTrue(heights.isNotEmpty())
            assertTrue(heights.all { it > 0 }, "rows collapsed after column resize: $heights")
            val codeHeights = harness.codeBlockPreferredHeights()
            assertTrue(codeHeights.isNotEmpty(), "expected code block after resize")
            assertTrue(codeHeights.all { it > 0 }, "code blocks must remain visible after resize")
        } finally {
            harness.dispose()
        }
    }

    @Test
    fun `mixed tool and agent rows keep non zero code block heights`() {
        val harness = TranscriptPanelTestHarness(columnWidth = 500)
        try {
            harness.apply(
                StructuredUpdate.StartOrUpdateToolCall(
                    toolCallId = "t1",
                    title = "execute_code",
                    kind = ToolKind.EDIT,
                    status = ToolCallStatus.COMPLETED,
                    bodyParts = listOf(TranscriptBodyPart.Code("python", "print('tool')")),
                ),
                StructuredUpdate.AppendAgentText("```kotlin\nfun agent()\n```"),
                StructuredUpdate.FinalizeAgentStream,
            )

            harness.assertRowCount(2)
            harness.assertNoHorizontalScrollbar()

            val toolCodeHeights = harness.codeBlockPreferredHeights()
            assertTrue(toolCodeHeights.isNotEmpty(), "expected tool code block")
            assertTrue(toolCodeHeights.all { it > 0 }, "tool code block collapsed")

            val agentHeights = harness.rowPreferredHeights()
            assertEquals(2, agentHeights.size)
            assertTrue(agentHeights.all { it > 0 }, "agent row collapsed alongside tool row")
        } finally {
            harness.dispose()
        }
    }

    @Test
    fun `ingest finalizes agent stream before mapping tool call update`() {
        val harness = TranscriptPanelTestHarness()
        try {
            harness.applyIngest(
                SessionUpdate.AgentMessageChunk(content = ContentBlock.Text("Thinking")),
            )
            harness.applyIngest(
                SessionUpdate.ToolCallUpdate(
                    toolCallId = ToolCallId("1"),
                    title = "read",
                    kind = ToolKind.READ,
                    status = ToolCallStatus.COMPLETED,
                    content =
                        listOf(
                            ToolCallContent.Content(ContentBlock.Text("# README")),
                        ),
                ),
            )

            harness.assertRowCount(2)
            val blocks = harness.blocks()
            val agentIndex = blocks.indexOfFirst { it is TranscriptBlock.FinalAgentText }
            val toolIndex = blocks.indexOfFirst { it is TranscriptBlock.ToolCallBlock }
            assertTrue(agentIndex >= 0, "expected finalized agent text block")
            assertTrue(toolIndex >= 0, "expected tool call block")
            assertTrue(
                agentIndex < toolIndex,
                "agent stream must finalize before tool row is appended",
            )
        } finally {
            harness.dispose()
        }
    }

    private fun manyPlainLineUpdates(count: Int): List<StructuredUpdate.AppendPlainLine> =
        (1..count).map { index ->
            StructuredUpdate.AppendPlainLine(
                "line $index with enough text to wrap in a narrow viewport",
            )
        }
}
