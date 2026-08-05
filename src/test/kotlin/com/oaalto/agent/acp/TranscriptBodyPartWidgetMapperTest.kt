package com.oaalto.agent.acp

import com.oaalto.agent.acp.transcript.theme.DefaultTranscriptColorProvider
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JEditorPane
import javax.swing.JPanel
import javax.swing.JTextPane
import javax.swing.SwingUtilities
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TranscriptBodyPartWidgetMapperTest {
    private val agentRecFactory = RecordingCodeBlockViewFactory()
    private val colorProvider = DefaultTranscriptColorProvider()

    private fun createAgentMapper(): TranscriptBodyPartWidgetMapper =
        TranscriptBodyPartWidgetMapper(
            codeBlockViewFactory = agentRecFactory,
            colorProvider = colorProvider,
            profile = BodyPartRenderProfile.AGENT,
        )

    private fun createToolMapper(toolRecFactory: RecordingCodeBlockViewFactory): TranscriptBodyPartWidgetMapper =
        TranscriptBodyPartWidgetMapper(
            codeBlockViewFactory = toolRecFactory,
            colorProvider = colorProvider,
            profile = BodyPartRenderProfile.TOOL,
        )

    @Test
    fun `agent mapper renders code block with syntax highlighting`() =
        runOnEdt {
            val recFactory = RecordingCodeBlockViewFactory()
            val mapper =
                TranscriptBodyPartWidgetMapper(
                    codeBlockViewFactory = recFactory,
                    colorProvider = colorProvider,
                    profile = BodyPartRenderProfile.AGENT,
                )
            val parts = listOf(TranscriptBodyPart.Code("kotlin", "fun main()"))

            val (column, disposables) = mapper.buildBodyColumn(parts)

            assertEquals(1, disposables.size)
            assertEquals(1, recFactory.createdCount)
            assertEquals("kotlin", recFactory.languageIds.single())
            assertEquals("fun main()", recFactory.codes.single())
            assertEquals(1, column.componentCount)
        }

    @Test
    fun `tool mapper renders code block with syntax highlighting`() =
        runOnEdt {
            val recFactory = RecordingCodeBlockViewFactory()
            val mapper =
                TranscriptBodyPartWidgetMapper(
                    codeBlockViewFactory = recFactory,
                    colorProvider = colorProvider,
                    profile = BodyPartRenderProfile.TOOL,
                )
            val parts = listOf(TranscriptBodyPart.Code("python", "print('hello')"))

            val (column, disposables) = mapper.buildBodyColumn(parts)

            assertEquals(1, disposables.size)
            assertEquals(1, recFactory.createdCount)
            assertEquals(1, column.componentCount)
        }

    @Test
    fun `code block beyond highlight budget falls back to HTML`() =
        runOnEdt {
            val recFactory = RecordingCodeBlockViewFactory()
            val mapper =
                TranscriptBodyPartWidgetMapper(
                    codeBlockViewFactory = recFactory,
                    colorProvider = colorProvider,
                    profile = BodyPartRenderProfile.AGENT,
                )
            val parts =
                List(TranscriptToolCallContentRenderer.MAX_HIGHLIGHTED_CODE_BLOCKS + 1) {
                    TranscriptBodyPart.Code("kotlin", "fun f$it()")
                }

            val (column, disposables) = mapper.buildBodyColumn(parts)

            assertEquals(TranscriptToolCallContentRenderer.MAX_HIGHLIGHTED_CODE_BLOCKS, recFactory.createdCount)
            assertEquals(TranscriptToolCallContentRenderer.MAX_HIGHLIGHTED_CODE_BLOCKS, disposables.size)
            // Last code block should be rendered as HTML (no code block created)
            val lastComponent = column.getComponent(column.componentCount - 1)
            assertTrue(lastComponent is JEditorPane, "overflow code should render as HTML")
        }

    @Test
    fun `html part renders as JEditorPane`() =
        runOnEdt {
            val mapper = createAgentMapper()
            val parts = listOf(TranscriptBodyPart.Html("<p>Hello</p>"))

            val (column, disposables) = mapper.buildBodyColumn(parts)

            assertEquals(0, disposables.size)
            assertEquals(1, column.componentCount)
            val component = column.getComponent(0)
            assertTrue(component is JEditorPane)
        }

    @Test
    fun `blockquote renders for agent profile`() =
        runOnEdt {
            val mapper = createAgentMapper()
            val parts =
                listOf(TranscriptBodyPart.BlockQuote(listOf(TranscriptBodyPart.InlineText("quoted", emptyList()))))

            val (column, disposables) = mapper.buildBodyColumn(parts)

            assertEquals(0, disposables.size)
            assertEquals(1, column.componentCount)
            val component = column.getComponent(0)
            assertTrue(component is JPanel)
        }

    @Test
    fun `blockquote renders for tool profile`() =
        runOnEdt {
            val recFactory = RecordingCodeBlockViewFactory()
            val mapper = createToolMapper(recFactory)
            val parts =
                listOf(TranscriptBodyPart.BlockQuote(listOf(TranscriptBodyPart.InlineText("quoted", emptyList()))))

            val (column, disposables) = mapper.buildBodyColumn(parts)

            assertEquals(0, disposables.size)
            assertEquals(1, column.componentCount)
            val component = column.getComponent(0)
            assertTrue(component is JPanel)
        }

    @Test
    fun `thematic break renders as separator`() =
        runOnEdt {
            val mapper = createAgentMapper()
            val parts = listOf(TranscriptBodyPart.ThematicBreak)

            val (column, disposables) = mapper.buildBodyColumn(parts)

            assertEquals(0, disposables.size)
            assertEquals(1, column.componentCount)
        }

    @Test
    fun `agent mapper renders inline text as styled text pane`() =
        runOnEdt {
            val mapper = createAgentMapper()
            val parts = listOf(TranscriptBodyPart.InlineText("hello world", emptyList()))

            val (column, disposables) = mapper.buildBodyColumn(parts)

            assertEquals(0, disposables.size)
            assertEquals(1, column.componentCount)
            val component = column.getComponent(0)
            assertTrue(component is JTextPane)
        }

    @Test
    fun `agent and tool mappers produce same widgets for shared part kinds`() =
        runOnEdt {
            val agentRecFactory = RecordingCodeBlockViewFactory()
            val agentMapper =
                TranscriptBodyPartWidgetMapper(
                    codeBlockViewFactory = agentRecFactory,
                    colorProvider = colorProvider,
                    profile = BodyPartRenderProfile.AGENT,
                )
            val toolRecFactory = RecordingCodeBlockViewFactory()
            val toolMapper =
                TranscriptBodyPartWidgetMapper(
                    codeBlockViewFactory = toolRecFactory,
                    colorProvider = colorProvider,
                    profile = BodyPartRenderProfile.TOOL,
                )
            val sharedParts =
                listOf(
                    TranscriptBodyPart.Code("kotlin", "fun main()"),
                    TranscriptBodyPart.Html("<p>html</p>"),
                    TranscriptBodyPart.ThematicBreak,
                )

            val (agentColumn, agentDisposables) = agentMapper.buildBodyColumn(sharedParts)
            val (toolColumn, toolDisposables) = toolMapper.buildBodyColumn(sharedParts)

            // Both should create 1 code block each
            assertEquals(1, agentRecFactory.createdCount)
            assertEquals(1, toolRecFactory.createdCount)
            assertEquals(1, agentDisposables.size)
            assertEquals(1, toolDisposables.size)
            // Same widget types in same order
            assertEquals(agentColumn.componentCount, toolColumn.componentCount)
            for (i in 0 until agentColumn.componentCount) {
                assertEquals(
                    agentColumn.getComponent(i)::class.simpleName,
                    toolColumn.getComponent(i)::class.simpleName,
                    "widget type mismatch at index $i",
                )
            }
        }

    @Test
    fun `gap spacing inserted between parts`() =
        runOnEdt {
            val mapper = createAgentMapper()
            val parts =
                listOf(
                    TranscriptBodyPart.InlineText("first", emptyList()),
                    TranscriptBodyPart.InlineText("second", emptyList()),
                    TranscriptBodyPart.InlineText("third", emptyList()),
                )

            val (column, _) = mapper.buildBodyColumn(parts)

            // 3 text parts + 2 gap spacers
            assertEquals(5, column.componentCount)
            // Verify gaps are vertical struts
            column.components.forEachIndexed { index, comp ->
                if (index % 2 == 1) {
                    assertTrue(comp is Box.Filler, "expected gap at index $index")
                }
            }
        }

    @Test
    fun `dispose cleans up code components`() =
        runOnEdt {
            val recFactory = RecordingCodeBlockViewFactory()
            val mapper =
                TranscriptBodyPartWidgetMapper(
                    codeBlockViewFactory = recFactory,
                    colorProvider = colorProvider,
                    profile = BodyPartRenderProfile.AGENT,
                )
            val parts = listOf(TranscriptBodyPart.Code("kotlin", "fun main()"))

            val (_, disposables) = mapper.buildBodyColumn(parts)
            assertEquals(1, disposables.size)

            disposables.forEach(recFactory::dispose)
            assertEquals(1, recFactory.disposedCount)
        }

    @Test
    fun `heading renders for agent profile`() =
        runOnEdt {
            val mapper = createAgentMapper()
            val parts = listOf(TranscriptBodyPart.Heading(2, "Title", emptyList()))

            val (column, _) = mapper.buildBodyColumn(parts)

            assertEquals(1, column.componentCount)
            val component = column.getComponent(0)
            assertTrue(component is JTextPane)
        }

    @Test
    fun `image label renders for agent profile`() =
        runOnEdt {
            val mapper = createAgentMapper()
            val parts = listOf(TranscriptBodyPart.Image("test image", ""))

            val (column, _) = mapper.buildBodyColumn(parts)

            assertEquals(1, column.componentCount)
        }

    @Test
    fun `list line renders for agent profile`() =
        runOnEdt {
            val mapper = createAgentMapper()
            val parts = listOf(TranscriptBodyPart.ListLine("- item", "item", emptyList()))

            val (column, _) = mapper.buildBodyColumn(parts)

            assertEquals(1, column.componentCount)
            val component = column.getComponent(0)
            assertTrue(component is JTextPane)
        }

    @Test
    fun `column has correct layout and alignment`() =
        runOnEdt {
            val mapper = createAgentMapper()
            val parts = listOf(TranscriptBodyPart.InlineText("text", emptyList()))

            val (column, _) = mapper.buildBodyColumn(parts)

            assertTrue(column.layout is BoxLayout)
            assertFalse(column.isOpaque)
        }

    private fun runOnEdt(action: () -> Unit) {
        if (SwingUtilities.isEventDispatchThread()) {
            action()
        } else {
            SwingUtilities.invokeAndWait(action)
        }
    }
}
