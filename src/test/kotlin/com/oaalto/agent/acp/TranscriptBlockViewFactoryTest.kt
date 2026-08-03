package com.oaalto.agent.acp

import com.agentclientprotocol.model.ToolCallStatus
import com.agentclientprotocol.model.ToolKind
import java.awt.Dimension
import java.awt.event.ComponentEvent
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTextArea
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
    fun `main kt inline fence transcript creates two code block views`() {
        val dollar = "$"
        val factory = RecordingCodeBlockViewFactory()
        val viewFactory = TranscriptBlockViewFactory(factory)
        viewFactory.create(
            TranscriptBlock.FinalAgentText(
                blockId = "1",
                text =
                    """Here's a concise Kotlin example in the same style as your `Main.kt`:```kotlinfun sum(numbers: List<Int>): Int = numbers.sum()fun main() {
 val numbers = listOf(1,2,3,4,5)
 println(sum(numbers)) //15}
```A slightly richer version with data classes and null safety:

```kotlindata class User(val name: String, val age: Int?)fun greet(user: User): String {
 val ageText = user.age?.let { "${dollar}it years old" } ?: "age unknown"
 return "Hello, $dollar{user.name} (${dollar}ageText)"}

fun main() {
 val users = listOf(
 User("Ada",36), User("Grace", null)
 )

 users .map(::greet)
 .forEach(::println)
}
```Your project already has the first style in `src/main/kotlin/Main.kt`.""",
            ),
            onToolToggle = {},
        )

        assertEquals(2, factory.createdCount)
        assertTrue(factory.codes.all { it.contains('\n') }, factory.codes.toString())
    }

    @Test
    fun `plain monospace code block is selectable`() {
        val viewFactory = TranscriptBlockViewFactory(PlainMonospaceTranscriptCodeBlockViewFactory)
        val row =
            viewFactory.create(
                TranscriptBlock.FinalAgentText(
                    blockId = "1",
                    text = "```kotlin\nfun main()\n```",
                ),
                onToolToggle = {},
            )

        val codeArea = findCodeBlockTextArea(row)
        assertTrue(codeArea.isEnabled)
        assertTrue(!codeArea.isEditable)
    }

    @Test
    fun `kotlin sample transcript creates code block views`() {
        val dollar = "$"
        val factory = RecordingCodeBlockViewFactory()
        val viewFactory = TranscriptBlockViewFactory(factory)
        viewFactory.create(
            TranscriptBlock.FinalAgentText(
                blockId = "1",
                text =
                    """Here's a small Kotlin sample:

```kotlinfun main() {
 val name = "Kotlin"
 val numbers = listOf(1,2,3,4,5)

 val doubled = numbers.map { it *2 }
 println("Hello, ${dollar}name!") println("Doubled: ${dollar}doubled") greet("Ada")}

fun greet(person: String) {
 println("Nice to meet you, ${dollar}person.")}
```Want something more specific (coroutines, Android, data classes, etc.)?""",
            ),
            onToolToggle = {},
        )

        assertEquals(1, factory.createdCount)
        assertTrue(factory.codes.single().contains("fun main"))
    }

    @Test
    fun `malformed concat transcript creates code block views`() {
        val dollar = "$"
        val factory = RecordingCodeBlockViewFactory()
        val viewFactory = TranscriptBlockViewFactory(factory)
        viewFactory.create(
            TranscriptBlock.FinalAgentText(
                blockId = "1",
                text =
                    """```kotlinfun concat(a: String, b: String): String = a + b```Or with string templates:

```kotlinfun concat(a: String, b: String): String = ${dollar}a${dollar}b
""",
            ),
            onToolToggle = {},
        )

        assertEquals(2, factory.createdCount)
        assertTrue(factory.codes.all { it.isNotBlank() }, factory.codes.toString())
    }

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

    @Test
    fun `code block width adjustment preserves monospace text content`() {
        val viewFactory = TranscriptBlockViewFactory(PlainMonospaceTranscriptCodeBlockViewFactory)
        val row =
            viewFactory.create(
                TranscriptBlock.FinalAgentText(
                    blockId = "1",
                    text = "```kotlin\nfun main()\n```",
                ),
                onToolToggle = {},
            )

        row.setSize(800, 200)
        row.doLayout()
        row.setSize(200, 200)
        row.maximumSize

        val codeArea = findCodeBlockTextArea(row)
        assertEquals("fun main()", codeArea.text.trim())
        val availableWidth = row.width - row.insets.left - row.insets.right
        assertEquals(availableWidth, codeArea.preferredSize.width)
    }

    private fun findCodeBlockTextArea(row: JPanel): JTextArea {
        val contentColumn = row.getComponent(0) as JPanel
        return contentColumn.components
            .filterIsInstance<JTextArea>()
            .first { isTranscriptCodeBlock(it) }
    }

    @Test
    fun `transcript row width adjustment updates preferred size for all children`() {
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

        row.setSize(800, 200)
        row.doLayout()
        row.setSize(400, 200)
        row.maximumSize

        val availableWidth = row.width - row.insets.left - row.insets.right
        val contentColumn = row.getComponent(0) as JPanel
        contentColumn.components.filterIsInstance<JComponent>().forEach { child ->
            assertEquals(
                availableWidth,
                child.preferredSize.width,
                "child ${child::class.simpleName} preferred width",
            )
        }
    }

    @Test
    fun `expanded tool card body adjustment updates preferred size for all children`() {
        val factory = RecordingCodeBlockViewFactory()
        val viewFactory = TranscriptBlockViewFactory(factory)
        val row =
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
            ) as CollapsibleToolPanel

        row.setSize(800, 400)
        row.dispatchEvent(ComponentEvent(row, ComponentEvent.COMPONENT_RESIZED))
        row.setSize(400, 400)
        row.dispatchEvent(ComponentEvent(row, ComponentEvent.COMPONENT_RESIZED))

        val availableWidth = row.width - row.insets.left - row.insets.right
        val bodyContainer = row.components.filterIsInstance<JPanel>().last { it.layout is BoxLayout }
        bodyContainer.components.filterIsInstance<JComponent>().filterNot { it is Box.Filler }.forEach { child ->
            assertEquals(
                availableWidth,
                child.preferredSize.width,
                "child ${child::class.simpleName} preferred width",
            )
        }
    }

    private class RecordingCodeBlockViewFactory : TranscriptCodeBlockViewFactory {
        var createdCount = 0
            private set
        var disposedCount = 0
            private set
        val codes = mutableListOf<String>()

        override fun createReadOnlyCodeBlock(
            languageId: String?,
            code: String,
        ): JPanel {
            createdCount += 1
            codes += code
            return JPanel().apply {
                preferredSize = Dimension(100, 48)
            }
        }

        override fun dispose(component: javax.swing.JComponent) {
            disposedCount += 1
        }
    }
}
