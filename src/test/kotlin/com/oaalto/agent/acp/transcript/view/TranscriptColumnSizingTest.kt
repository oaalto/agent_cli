package com.oaalto.agent.acp.transcript.view

import javax.swing.JTextPane
import kotlin.test.Test
import kotlin.test.assertTrue

class TranscriptColumnSizingTest {
    @Test
    fun `applyTranscriptColumnWidth caps inline text pane height`() {
        val pane =
            JTextPane().apply {
                text = "Added `Person` in `src/main/kotlin/Person.kt`:"
            }

        applyTranscriptColumnWidth(pane, 400)

        assertTrue(pane.maximumSize.height < 10_000, "max height was ${pane.maximumSize.height}")
        assertTrue(pane.preferredSize.height > 0)
        assertTrue(pane.preferredSize.width == 400)
    }

    @Test
    fun `plain monospace code block sizing stays bounded`() {
        val code =
            PlainMonospaceTranscriptCodeBlockViewFactory.createReadOnlyCodeBlock(
                "kotlin",
                "class Person(\n val name: String,\n)",
            )

        applyTranscriptCodeBlockWidth(code, 400)

        assertTrue(code.maximumSize.height < 10_000)
        assertTrue(code.preferredSize.height > 10)
        assertTrue(code.preferredSize.width == 400)
    }

    @Test
    fun `plain monospace code block height accounts for soft wrapped lines`() {
        val longLine = "fun greet(): String = " + "\"x\" ".repeat(80)
        val code =
            PlainMonospaceTranscriptCodeBlockViewFactory.createReadOnlyCodeBlock(
                "kotlin",
                "class Person {\n$longLine\n}",
            )

        applyTranscriptCodeBlockWidth(code, 200)

        assertTrue(code.preferredSize.height > 40, "height was ${code.preferredSize.height}")
    }
}
