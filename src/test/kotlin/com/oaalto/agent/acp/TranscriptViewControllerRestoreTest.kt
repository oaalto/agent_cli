package com.oaalto.agent.acp

import com.oaalto.agent.acp.transcript.model.StructuredUpdate
import com.oaalto.agent.acp.transcript.model.TranscriptBlock
import com.oaalto.agent.acp.transcript.model.TranscriptModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class TranscriptViewControllerRestoreTest {
    @Test
    fun `plain line replay maps user prompt prefix to isUserPrompt`() {
        val model = TranscriptModel()
        val content = "> prior prompt\nagent reply\n[tool: read file completed]"
        content.lineSequence().forEach { line ->
            val isUserPrompt = line.startsWith("> ")
            model.apply(StructuredUpdate.AppendPlainLine(line, isUserPrompt))
        }

        val blocks = model.blocks()
        assertEquals(3, blocks.size)
        val first = assertIs<TranscriptBlock.PlainLine>(blocks[0])
        assertEquals("> prior prompt", first.text)
        assertEquals(true, first.isUserPrompt)
        val second = assertIs<TranscriptBlock.PlainLine>(blocks[1])
        assertEquals("agent reply", second.text)
        assertEquals(false, second.isUserPrompt)
        val third = assertIs<TranscriptBlock.PlainLine>(blocks[2])
        assertEquals("[tool: read file completed]", third.text)
    }

    @Test
    fun `decorated error message stored on error line block`() {
        val model = TranscriptModel()
        val displayMessage = "boom [agent-cli:abcd]"
        model.apply(StructuredUpdate.AppendError(displayMessage))

        val error = assertIs<TranscriptBlock.ErrorLine>(model.blocks().single())
        assertEquals("boom [agent-cli:abcd]", error.message)
    }
}
