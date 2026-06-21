@file:Suppress("OPT_IN_USAGE")

package com.oaalto.agent.acp

import com.agentclientprotocol.model.ToolCallStatus
import com.agentclientprotocol.model.ToolKind
import javax.swing.JPanel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TranscriptTextTruncationTest {
    @Test
    fun `truncate leaves short text unchanged`() {
        assertEquals("hello", TranscriptTextTruncation.truncate("hello"))
    }

    @Test
    fun `truncate appends total character suffix`() {
        val longText = "x".repeat(TranscriptToolCallContentRenderer.MAX_TEXT_CHARACTERS + 100)
        val truncated = TranscriptTextTruncation.truncate(longText)

        assertTrue(truncated.contains("… (truncated, ${longText.length} characters total)"))
        assertTrue(truncated.length < longText.length)
    }
}

class TranscriptBlockViewFactoryTest {
    @Test
    fun `disposeRow releases code block components`() {
        val factory = RecordingCodeBlockViewFactory()
        val viewFactory = TranscriptBlockViewFactory(factory)
        val row =
            viewFactory.create(
                TranscriptBlock.FinalAgentText(
                    blockId = "1",
                    text = "```kotlin\nfun main()\n```",
                ),
                onToolToggle = {},
            )

        assertEquals(1, factory.createdCount)

        viewFactory.disposeRow(row)

        assertEquals(1, factory.disposedCount)
    }

    @Test
    fun `collapsed tool card does not create code block components`() {
        val factory = RecordingCodeBlockViewFactory()
        val viewFactory = TranscriptBlockViewFactory(factory)
        viewFactory.create(
            TranscriptBlock.ToolCallBlock(
                blockId = "1",
                toolCallId = "tool-1",
                title = "read file",
                kind = ToolKind.READ,
                status = ToolCallStatus.COMPLETED,
                bodyParts =
                    listOf(
                        TranscriptBodyPart.Code("kotlin", "fun main()"),
                    ),
                expanded = false,
            ),
            onToolToggle = {},
        )

        assertEquals(0, factory.createdCount)
    }

    @Test
    fun `expanded tool card creates code block components`() {
        val factory = RecordingCodeBlockViewFactory()
        val viewFactory = TranscriptBlockViewFactory(factory)
        viewFactory.create(
            TranscriptBlock.ToolCallBlock(
                blockId = "1",
                toolCallId = "tool-1",
                title = "read file",
                kind = ToolKind.READ,
                status = ToolCallStatus.COMPLETED,
                bodyParts =
                    listOf(
                        TranscriptBodyPart.Code("kotlin", "fun main()"),
                    ),
                expanded = true,
            ),
            onToolToggle = {},
        )

        assertEquals(1, factory.createdCount)
    }

    @Test
    fun `streaming agent text updates in place without code block factory`() {
        val factory = RecordingCodeBlockViewFactory()
        val viewFactory = TranscriptBlockViewFactory(factory)
        val row =
            viewFactory.create(
                TranscriptBlock.StreamingAgentText(blockId = "1", text = "Hel"),
                onToolToggle = {},
            )

        viewFactory.update(row, TranscriptBlock.StreamingAgentText(blockId = "1", text = "Hello"))

        assertEquals(0, factory.createdCount)
    }

    @Test
    fun `expanded tool card skips body rebuild when only header changes`() {
        val factory = RecordingCodeBlockViewFactory()
        val viewFactory = TranscriptBlockViewFactory(factory)
        val bodyParts = listOf(TranscriptBodyPart.Code("kotlin", "fun main()"))
        val row =
            viewFactory.create(
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

        assertEquals(1, factory.createdCount)

        viewFactory.update(
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

        assertEquals(1, factory.createdCount)
    }

    private class RecordingCodeBlockViewFactory : TranscriptCodeBlockViewFactory {
        var createdCount = 0
            private set
        var disposedCount = 0
            private set

        override fun createReadOnlyCodeBlock(
            languageId: String?,
            code: String,
        ): JPanel {
            createdCount += 1
            return JPanel()
        }

        override fun dispose(component: javax.swing.JComponent) {
            disposedCount += 1
        }
    }
}
