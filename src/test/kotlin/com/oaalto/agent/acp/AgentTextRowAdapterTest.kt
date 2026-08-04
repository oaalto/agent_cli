package com.oaalto.agent.acp

import java.awt.Dimension
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTextArea
import javax.swing.JTextPane
import javax.swing.SwingUtilities
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RecordingCodeBlockViewFactory : TranscriptCodeBlockViewFactory {
    var createdCount = 0
        private set
    var disposedCount = 0
        private set
    val codes = mutableListOf<String>()
    val languageIds = mutableListOf<String?>()

    override fun createReadOnlyCodeBlock(
        languageId: String?,
        code: String,
    ): JPanel {
        createdCount += 1
        codes += code
        languageIds += languageId
        return JPanel().apply {
            preferredSize = Dimension(100, 48)
        }
    }

    override fun dispose(component: JComponent) {
        disposedCount += 1
    }
}

class AgentTextRowAdapterTest {
    private val recFactory = RecordingCodeBlockViewFactory()
    private val context = createTestRowContext(recFactory)
    private val adapter = AgentTextRowAdapter()

    @Test
    fun `matches returns true for agent text block types`() {
        assertTrue(adapter.matches(TranscriptBlock.StreamingAgentText("1", "stream")))
        assertTrue(adapter.matches(TranscriptBlock.FinalAgentText("1", "final")))
    }

    @Test
    fun `matches returns false for non-agent block types`() {
        assertFalse(adapter.matches(TranscriptBlock.UserEcho("1", "echo")))
        assertFalse(adapter.matches(TranscriptBlock.PlainLine("1", "plain")))
        assertFalse(adapter.matches(TranscriptBlock.ErrorLine("1", "err")))
        assertFalse(
            adapter.matches(
                TranscriptBlock.ToolCallBlock(
                    blockId = "1",
                    toolCallId = "t1",
                    title = "t",
                    kind = null,
                    status = null,
                    bodyParts = emptyList(),
                ),
            ),
        )
        assertFalse(
            adapter.matches(
                TranscriptBlock.PlanBlock(
                    blockId = "1",
                    planId = "p1",
                    entries = emptyList(),
                ),
            ),
        )
    }

    @Test
    fun `main kt inline fence transcript creates two code block views`() =
        runOnEdt {
            val dollar = "$"
            val row =
                adapter.create(
                    context,
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

            assertEquals(2, recFactory.createdCount)
            assertTrue(recFactory.codes.all { it.contains('\n') }, recFactory.codes.toString())
        }

    @Test
    fun `plain monospace code block is selectable`() =
        runOnEdt {
            val monoCtx = createTestRowContext(PlainMonospaceTranscriptCodeBlockViewFactory)
            val adapter = AgentTextRowAdapter()
            val row =
                adapter.create(
                    monoCtx,
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
    fun `kotlin sample transcript creates code block views`() =
        runOnEdt {
            val dollar = "$"
            val row =
                adapter.create(
                    context,
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

            assertEquals(1, recFactory.createdCount)
            assertTrue(
                recFactory.codes
                    .single()
                    .contains("fun main"),
            )
        }

    @Test
    fun `malformed concat transcript creates code block views`() =
        runOnEdt {
            val dollar = "$"
            val row =
                adapter.create(
                    context,
                    TranscriptBlock.FinalAgentText(
                        blockId = "1",
                        text =
                            """```kotlinfun concat(a: String, b: String): String = a + b```Or with string templates:

```kotlinfun concat(a: String, b: String): String = ${dollar}a${dollar}b
""",
                    ),
                    onToolToggle = {},
                )

            assertEquals(2, recFactory.createdCount)
            assertTrue(recFactory.codes.all { it.isNotBlank() }, recFactory.codes.toString())
        }

    @Test
    fun `person transcript creates two code block views`() =
        runOnEdt {
            val dollar = "$"
            val row =
                adapter.create(
                    context,
                    TranscriptBlock.FinalAgentText(
                        blockId = "1",
                        text =
                            """Added `Person` in `src/main/kotlin/Person.kt`:```kotlinclass Person(
 val name: String,
 val age: Int,
) {
 fun greet(): String = "Hello, my name is ${dollar}name and I am ${dollar}age years old." fun isAdult(): Boolean = age >=18}
```Usage:

```kotlinval person = Person("Ada",36)
println(person.greet())println(person.isAdult())```""",
                    ),
                    onToolToggle = {},
                )

            assertEquals(2, recFactory.createdCount)
            assertTrue(recFactory.codes[0].contains("class Person"))
            assertTrue(recFactory.codes[1].contains("val person"))
        }

    @Test
    fun `citation fence transcript creates kotlin code block view`() =
        runOnEdt {
            val dollar = "$"
            val row =
                adapter.create(
                    context,
                    TranscriptBlock.FinalAgentText(
                        blockId = "1",
                        text =
                            """Here it is:

```3:10:src/main/kotlin/Person.ktclass Person(
 val name: String,
 val age: Int,
) {
 fun greet(): String = "Hello, my name is ${dollar}name and I am ${dollar}age years old." fun isAdult(): Boolean = age >=18}
```""",
                    ),
                    onToolToggle = {},
                )

            assertEquals(1, recFactory.createdCount)
            assertEquals("kotlin", recFactory.languageIds.single())
            assertTrue(
                recFactory.codes
                    .single()
                    .contains("class Person"),
            )
        }

    @Test
    fun `final agent text row does not stretch inline text panes vertically`() =
        runOnEdt {
            val monoCtx = createTestRowContext(PlainMonospaceTranscriptCodeBlockViewFactory)
            val adapter = AgentTextRowAdapter()
            val row =
                adapter.create(
                    monoCtx,
                    TranscriptBlock.FinalAgentText(
                        blockId = "1",
                        text =
                            """Added in `file`:```kotlin
fun main()
```Usage:

```kotlin
println()
```""",
                    ),
                    onToolToggle = {},
                )

            row.setSize(600, 600)
            row.doLayout()
            row.maximumSize

            val contentColumn = row.getComponent(0) as JPanel
            contentColumn.components.filterIsInstance<JTextPane>().forEach { pane ->
                assertTrue(pane.height < 80, "inline pane stretched to height=${pane.height}")
            }
            val codeAreas =
                contentColumn.components
                    .filterIsInstance<JTextArea>()
                    .filter { isTranscriptCodeBlock(it) }
            assertEquals(2, codeAreas.size)
            assertTrue(codeAreas.all { it.preferredSize.height > 10 })
        }

    @Test
    fun `dispose releases code block components`() =
        runOnEdt {
            val row =
                adapter.create(
                    context,
                    TranscriptBlock.FinalAgentText(
                        blockId = "1",
                        text = "```kotlin\nfun main()\n```",
                    ),
                    onToolToggle = {},
                )

            assertEquals(1, recFactory.createdCount)

            adapter.dispose(context, row)

            assertEquals(1, recFactory.disposedCount)
        }

    @Test
    fun `streaming agent text updates in place without code block factory`() =
        runOnEdt {
            val row =
                adapter.create(
                    context,
                    TranscriptBlock.StreamingAgentText(blockId = "1", text = "Hel"),
                    onToolToggle = {},
                )

            adapter.update(context, row, TranscriptBlock.StreamingAgentText(blockId = "1", text = "Hello"))

            assertEquals(0, recFactory.createdCount)
        }

    @Test
    fun `code block width adjustment preserves monospace text content`() =
        runOnEdt {
            val monoCtx = createTestRowContext(PlainMonospaceTranscriptCodeBlockViewFactory)
            val adapter = AgentTextRowAdapter()
            val row =
                adapter.create(
                    monoCtx,
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

    @Test
    fun `transcript row width adjustment updates preferred size for all children`() =
        runOnEdt {
            val row =
                adapter.create(
                    context,
                    TranscriptBlock.FinalAgentText(
                        blockId = "1",
                        text = "```kotlin\nfun main()\n```",
                    ),
                    onToolToggle = {},
                )

            row.setSize(800, 200)
            row.doLayout()
            row.setSize(400, 200)
            row.doLayout()
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
    fun `update returns false on type mismatch`() =
        runOnEdt {
            val row =
                adapter.create(
                    context,
                    TranscriptBlock.FinalAgentText("1", "agent"),
                    onToolToggle = {},
                )

            val result = adapter.update(context, row, TranscriptBlock.PlainLine("2", "plain"))
            assertEquals(false, result)
        }

    private fun findCodeBlockTextArea(row: JPanel): JTextArea {
        val contentColumn = row.getComponent(0) as JPanel
        return contentColumn.components
            .filterIsInstance<JTextArea>()
            .first { isTranscriptCodeBlock(it) }
    }

    private fun createTestRowContext(factory: RecordingCodeBlockViewFactory): RowContext =
        RowContext(
            columnWidth = 600,
            codeBlockViewFactory = factory,
            colorProvider = DefaultTranscriptColorProvider(),
            logContextProvider = { null },
        )

    private fun createTestRowContext(factory: TranscriptCodeBlockViewFactory): RowContext =
        RowContext(
            columnWidth = 600,
            codeBlockViewFactory = factory,
            colorProvider = DefaultTranscriptColorProvider(),
            logContextProvider = { null },
        )

    private fun runOnEdt(action: () -> Unit) {
        if (SwingUtilities.isEventDispatchThread()) {
            action()
        } else {
            SwingUtilities.invokeAndWait(action)
        }
    }
}
