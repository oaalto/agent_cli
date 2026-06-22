package com.oaalto.agent.acp

import com.intellij.openapi.diagnostic.Logger
import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.Desktop
import java.awt.Dimension
import java.awt.Font
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.net.URI
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JComponent
import javax.swing.JEditorPane
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JSeparator
import javax.swing.JTextPane
import javax.swing.SwingConstants
import javax.swing.border.EmptyBorder
import javax.swing.text.StyleConstants
import javax.swing.text.StyledDocument

private const val MONO_FAMILY = "Monospaced"
private const val FONT_SIZE = 12
private const val BODY_PART_GAP = 4
private val ROW_BORDER = JBUI.Borders.empty(2, 0)
private const val LIST_LEFT_INSET = 16
private const val TABLE_LEFT_INSET = 20
private const val BLOCKQUOTE_LEFT_THICKNESS = 3
private const val BLOCKQUOTE_TEXT_INSET = 8
private const val BLOCKQUOTE_VERTICAL_PAD = 2
private const val THEMATIC_BREAK_VERTICAL_PAD = 4
private const val THEMATIC_BREAK_HORIZONTAL_INSET = 8
private const val MIN_HEADING_LEVEL = 1
private const val MAX_HEADING_LEVEL = 6
private val HEADING_SIZES = mapOf(1 to 18, 2 to 16, 3 to 14, 4 to 13, 5 to 12, 6 to 12)
private const val CODE_BG_DARK_RGB = 0x2D2D2D
private const val CODE_BG_LIGHT_RGB = 0xF0F0F0
private const val CODE_TEXT_DARK_RGB = 0xCE9178
private const val CODE_TEXT_LIGHT_RGB = 0x8B4513
private const val LINK_DARK_RGB = 0x569CD6
private const val LINK_LIGHT_RGB = 0x1A70C8

private fun pickStyle(styles: List<TextStyle>): TextStyle =
    if (styles.contains(TextStyle.CODE)) {
        TextStyle.CODE
    } else if (styles.contains(TextStyle.LINK)) {
        TextStyle.LINK
    } else {
        val hasBold = styles.contains(TextStyle.BOLD)
        val hasItalic = styles.contains(TextStyle.ITALIC)
        when {
            hasBold && hasItalic -> TextStyle.BOLD_ITALIC
            hasBold -> TextStyle.BOLD
            hasItalic -> TextStyle.ITALIC
            styles.contains(TextStyle.STRIKETHROUGH) -> TextStyle.STRIKETHROUGH
            else -> TextStyle.BOLD
        }
    }

private fun createThematicBreak(): JComponent =
    JSeparator(SwingConstants.HORIZONTAL).apply {
        border =
            JBUI.Borders.empty(
                JBUI.scale(THEMATIC_BREAK_VERTICAL_PAD),
                JBUI.scale(THEMATIC_BREAK_HORIZONTAL_INSET),
            )
        isOpaque = false
        alignmentX = Component.LEFT_ALIGNMENT
        maximumSize = Dimension(Int.MAX_VALUE, preferredSize.height)
    }

private fun createHtmlPane(html: String): JEditorPane =
    JEditorPane("text/html", html).apply {
        isEditable = false
        isOpaque = false
        border = EmptyBorder(0, 0, 0, 0)
        alignmentX = Component.LEFT_ALIGNMENT
        putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true)
    }

private fun applyStyleToRun(
    style: TextStyle,
    runStyle: javax.swing.text.Style,
) {
    when (style) {
        TextStyle.BOLD -> StyleConstants.setBold(runStyle, true)
        TextStyle.ITALIC -> StyleConstants.setItalic(runStyle, true)
        TextStyle.BOLD_ITALIC -> {
            StyleConstants.setBold(runStyle, true)
            StyleConstants.setItalic(runStyle, true)
        }
        TextStyle.CODE -> {
            StyleConstants.setFontFamily(runStyle, MONO_FAMILY)
            StyleConstants.setBackground(
                runStyle,
                JBColor(Color(CODE_BG_DARK_RGB), Color(CODE_BG_LIGHT_RGB)),
            )
            StyleConstants.setForeground(
                runStyle,
                JBColor(Color(CODE_TEXT_DARK_RGB), Color(CODE_TEXT_LIGHT_RGB)),
            )
        }
        TextStyle.LINK -> {
            StyleConstants.setUnderline(runStyle, true)
            StyleConstants.setForeground(
                runStyle,
                JBColor(Color(LINK_DARK_RGB), Color(LINK_LIGHT_RGB)),
            )
        }
        TextStyle.STRIKETHROUGH -> StyleConstants.setStrikeThrough(runStyle, true)
    }
}

/** Maps [TranscriptBlock] snapshots to Swing row components. */
internal class TranscriptBlockViewFactory(
    private val codeBlockViewFactory: TranscriptCodeBlockViewFactory,
) {
    private val log = Logger.getInstance(TranscriptBlockViewFactory::class.java)

    fun create(
        block: TranscriptBlock,
        onToolToggle: (toolCallId: String) -> Unit,
    ): JPanel =
        when (block) {
            is TranscriptBlock.ToolCallBlock ->
                CollapsibleToolPanel(onToolToggle, codeBlockViewFactory).apply { bind(block) }
            else -> AgentTextRow(codeBlockViewFactory).apply { bind(block) }
        }

    fun update(
        component: JPanel,
        block: TranscriptBlock,
    ) {
        when {
            component is CollapsibleToolPanel && block is TranscriptBlock.ToolCallBlock ->
                component.bind(block)
            component is AgentTextRow && block !is TranscriptBlock.ToolCallBlock ->
                component.bind(block)
            component is CollapsibleToolPanel || block is TranscriptBlock.ToolCallBlock ->
                log.warn(
                    "Transcript block/component type mismatch: " +
                        "component=${component::class.simpleName}, block=${block::class.simpleName}",
                )
            else ->
                log.warn(
                    "Transcript text row type mismatch: " +
                        "component=${component::class.simpleName}, block=${block::class.simpleName}",
                )
        }
    }

    fun disposeRow(component: JPanel) {
        when (component) {
            is CollapsibleToolPanel -> component.disposeCodeComponents()
            is AgentTextRow -> component.disposeCodeComponents()
        }
    }

    companion object {
        fun escapeHtml(text: String): String = TranscriptRenderHelpers.escapeHtml(text)

        fun joinTableCells(
            cells: List<String>,
            alignStyles: List<String>,
            cellStyle: String,
            isHeader: Boolean,
        ): String {
            val tag = if (isHeader) "th" else "td"
            return cells
                .mapIndexed { idx, cell ->
                    val align = alignStyles.getOrElse(idx) { "text-align:left" }
                    "<$tag style='$cellStyle;$align'>$cell</$tag>"
                }.joinToString("")
        }

        fun tryOpenUrl(url: String) {
            try {
                val uri = URI(url)
                if (uri.scheme == "http" || uri.scheme == "https") {
                    Desktop.getDesktop().browse(uri)
                }
            } catch (_: Exception) {
                // silently ignore
            }
        }
    }
}

private class AgentTextRow(
    private val codeBlockViewFactory: TranscriptCodeBlockViewFactory,
) : JPanel(BorderLayout()) {
    private val contentColumn =
        JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            isOpaque = false
            alignmentX = Component.LEFT_ALIGNMENT
        }
    private val disposableCodeComponents = mutableListOf<JComponent>()
    private var renderedFinalText: String? = null

    init {
        border = ROW_BORDER
        isOpaque = false
        alignmentX = Component.LEFT_ALIGNMENT
        add(contentColumn, BorderLayout.CENTER)
        addComponentListener(
            object : ComponentAdapter() {
                override fun componentResized(event: ComponentEvent) {
                    widthAdjustment()
                }
            },
        )
    }

    override fun getMaximumSize(): Dimension {
        widthAdjustment()
        val pref = preferredSize
        return Dimension(Int.MAX_VALUE, pref.height)
    }

    fun bind(block: TranscriptBlock) {
        when (block) {
            is TranscriptBlock.FinalAgentText -> {
                val blocks = TranscriptMarkdownRenderer.parseToBlocks(block.text)
                if (!blocks.isEmpty() && renderedFinalText == block.text) {
                    widthAdjustment()
                    revalidate()
                    repaint()
                    return
                }
                rebuildMarkdownBlocks(blocks)
                renderedFinalText = block.text
            }
            is TranscriptBlock.StreamingAgentText ->
                setupSimpleTextPane { it.bindTranscriptBlock(block) }
            else ->
                setupSimpleTextPane { it.bindTranscriptBlock(block) }
        }
        widthAdjustment()
        revalidate()
        repaint()
    }

    fun disposeCodeComponents() {
        disposableCodeComponents.forEach(codeBlockViewFactory::dispose)
        disposableCodeComponents.clear()
        renderedFinalText = null
    }

    private fun widthAdjustment() {
        val w = width - insets.left - insets.right
        if (w <= 0) return
        contentColumn.components.forEach { child ->
            when (child) {
                is JTextPane -> {
                    child.setSize(w, Int.MAX_VALUE)
                    val height = child.preferredSize.height
                    child.preferredSize = Dimension(w, height)
                    child.maximumSize = Dimension(Int.MAX_VALUE, height)
                }
                is JComponent -> {
                    child.setSize(w, child.preferredSize.height)
                    child.maximumSize = Dimension(Int.MAX_VALUE, child.preferredSize.height)
                }
            }
        }
    }

    private fun setupSimpleTextPane(apply: (JTextPane) -> Unit) {
        renderedFinalText = null
        if (contentColumn.componentCount == 1 && disposableCodeComponents.isEmpty()) {
            val existing = contentColumn.getComponent(0)
            if (existing is JTextPane) {
                apply(existing)
                return
            }
        }
        disposeCodeComponents()
        contentColumn.removeAll()
        val pane =
            JTextPane().apply {
                isEditable = false
                isOpaque = false
                border = EmptyBorder(0, 0, 0, 0)
                font = Font(MONO_FAMILY, Font.PLAIN, FONT_SIZE)
            }
        apply(pane)
        contentColumn.add(pane)
    }

    private fun rebuildMarkdownBlocks(blocks: List<RenderedBlock>) {
        disposeCodeComponents()
        contentColumn.removeAll()
        val highlightedCount = IntArray(1)
        var needGap = false

        for (block in blocks) {
            if (needGap) {
                contentColumn.add(Box.createVerticalStrut(JBUI.scale(BODY_PART_GAP)))
            }
            needGap = true
            contentColumn.add(renderBlockToComponent(block, highlightedCount))
        }
    }

    private fun renderBlockToComponent(
        block: RenderedBlock,
        highlightedCount: IntArray,
    ): JComponent =
        when (block) {
            is RenderedBlock.InlineText -> renderInlineText(block)
            is RenderedBlock.CodeBlock -> renderCodeBlock(block.code, block.languageId, highlightedCount)
            is RenderedBlock.Table -> createTableHtmlPart(block)
            is RenderedBlock.Image -> createImageLabel(block)
            is RenderedBlock.ThematicBreak -> createThematicBreak()
            is RenderedBlock.BlockQuote -> createBlockQuotePanel(block)
            is RenderedBlock.CustomHtml ->
                createHtmlPane(
                    TranscriptRenderHelpers.htmlDocumentStart() +
                        block.html +
                        TranscriptRenderHelpers.HTML_DOCUMENT_END,
                )
        }

    private fun renderInlineText(block: RenderedBlock.InlineText): JComponent {
        val displayText = TranscriptTextTruncation.truncate(block.text)
        return when {
            block.headingLevel > 0 -> {
                val headingFont =
                    Font(
                        MONO_FAMILY,
                        Font.BOLD,
                        HEADING_SIZES.getValue(
                            block.headingLevel.coerceIn(MIN_HEADING_LEVEL, MAX_HEADING_LEVEL),
                        ),
                    )
                createStyledTextPane(displayText, block.runs, font = headingFont)
            }
            block.listMarker != null -> {
                val marker = block.listMarker
                val offsetRuns =
                    block.runs.map { run ->
                        StyledRun(run.start + marker.length, run.end + marker.length, run.style, run.url)
                    }
                createStyledTextPane(
                    "$marker$displayText",
                    offsetRuns,
                    border = EmptyBorder(0, JBUI.scale(LIST_LEFT_INSET), 0, 0),
                )
            }
            else -> createStyledTextPane(displayText, block.runs)
        }
    }

    private fun renderCodeBlock(
        code: String,
        languageId: String?,
        highlightedCount: IntArray,
    ): JComponent {
        val displayCode = TranscriptTextTruncation.truncate(code)
        if (highlightedCount[0] < TranscriptToolCallContentRenderer.MAX_HIGHLIGHTED_CODE_BLOCKS) {
            highlightedCount[0] += 1
            val codeComponent =
                codeBlockViewFactory
                    .createReadOnlyCodeBlock(languageId, displayCode)
                    .also {
                        it.alignmentX = Component.LEFT_ALIGNMENT
                        it.maximumSize = Dimension(Int.MAX_VALUE, it.preferredSize.height)
                    }
            disposableCodeComponents += codeComponent
            return codeComponent
        }
        return JEditorPane(
            "text/html",
            TranscriptRenderHelpers.htmlDocumentStart() +
                TranscriptHtmlBuilder.buildPlainPre(displayCode) +
                TranscriptRenderHelpers.HTML_DOCUMENT_END,
        ).apply {
            isEditable = false
            isOpaque = false
            border = EmptyBorder(0, 0, 0, 0)
            alignmentX = Component.LEFT_ALIGNMENT
            putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true)
        }
    }

    private fun createStyledTextPane(
        text: String,
        runs: List<StyledRun>,
        font: Font? = null,
        border: EmptyBorder? = null,
    ): JTextPane {
        val pane =
            JTextPane().apply {
                isEditable = false
                isOpaque = false
                this.border = border ?: EmptyBorder(0, 0, 0, 0)
                this.font =
                    font ?: Font(MONO_FAMILY, Font.PLAIN, FONT_SIZE)
                foreground =
                    JBColor(
                        Color(TranscriptPalette.AGENT_TEXT_DARK_RGB),
                        Color(TranscriptPalette.AGENT_TEXT_LIGHT_RGB),
                    )
            }
        pane.text = text
        // Filter runs to only those within truncated text bounds
        val validRuns = runs.filter { it.start < text.length && it.end <= text.length }
        applyStyledRuns(pane, validRuns)
        return pane
    }

    private fun createImageLabel(block: RenderedBlock.Image): JComponent {
        val displayText = "[image: ${block.altText}]"
        val url = block.url
        return JLabel(displayText).apply {
            isOpaque = false
            font = Font(MONO_FAMILY, Font.ITALIC, FONT_SIZE)
            foreground =
                JBColor(
                    Color(TranscriptPalette.THOUGHT_RGB),
                    Color(TranscriptPalette.THOUGHT_RGB),
                )
            border = JBUI.Borders.emptyLeft(LIST_LEFT_INSET)
            if (url.isNotEmpty()) {
                toolTipText = url
                cursor = java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR)
                addMouseListener(
                    object : MouseAdapter() {
                        override fun mouseClicked(event: MouseEvent) {
                            TranscriptBlockViewFactory.tryOpenUrl(url)
                        }
                    },
                )
            }
        }
    }

    private fun createBlockQuotePanel(blockQuote: RenderedBlock.BlockQuote): JPanel {
        val highlightedCount = IntArray(1)
        val panel =
            JPanel().apply {
                layout = BoxLayout(this, BoxLayout.Y_AXIS)
                isOpaque = false
                border =
                    JBUI.Borders.compound(
                        JBUI.Borders.empty(0, 0, 0, 0),
                        JBUI.Borders.emptyLeft(BLOCKQUOTE_LEFT_THICKNESS),
                        JBUI.Borders.empty(BLOCKQUOTE_VERTICAL_PAD, 0),
                    )
                alignmentX = Component.LEFT_ALIGNMENT
            }
        val borderPanel =
            JPanel(BorderLayout()).apply {
                isOpaque = false
                alignmentX = Component.LEFT_ALIGNMENT
            }
        val quoteContent =
            JPanel().apply {
                layout = BoxLayout(this, BoxLayout.Y_AXIS)
                isOpaque = false
                border = JBUI.Borders.emptyLeft(BLOCKQUOTE_TEXT_INSET)
                var needGap = false
                for (inner in blockQuote.blocks) {
                    if (needGap) {
                        add(Box.createVerticalStrut(JBUI.scale(BODY_PART_GAP)))
                    }
                    needGap = true
                    add(
                        when (inner) {
                            is RenderedBlock.InlineText ->
                                createStyledTextPane(TranscriptTextTruncation.truncate(inner.text), inner.runs)
                            else -> renderBlockToComponent(inner, highlightedCount)
                        },
                    )
                }
            }
        val leftBorderLine =
            JLabel().apply {
                isOpaque = true
                background =
                    JBColor(
                        Color(TranscriptPalette.THOUGHT_RGB),
                        Color(TranscriptPalette.THOUGHT_RGB),
                    )
                preferredSize = Dimension(BLOCKQUOTE_LEFT_THICKNESS, 1)
                minimumSize = Dimension(BLOCKQUOTE_LEFT_THICKNESS, 1)
                maximumSize = Dimension(BLOCKQUOTE_LEFT_THICKNESS, Int.MAX_VALUE)
            }
        borderPanel.add(leftBorderLine, BorderLayout.WEST)
        borderPanel.add(quoteContent, BorderLayout.CENTER)
        panel.add(borderPanel)
        return panel
    }

    private fun createTableHtmlPart(table: RenderedBlock.Table): JEditorPane {
        val escapedHeaders = table.headers.map { TranscriptRenderHelpers.escapeHtml(it) }
        val escapedRows = table.rows.map { row -> row.map { TranscriptRenderHelpers.escapeHtml(it) } }
        val alignStyles =
            table.alignments.map { align ->
                when (align) {
                    TableAlignment.LEFT -> "text-align:left"
                    TableAlignment.CENTER -> "text-align:center"
                    TableAlignment.RIGHT -> "text-align:right"
                }
            }
        val cellStyle = "border:1px solid #555;padding:4px"
        val headerCells =
            TranscriptBlockViewFactory.joinTableCells(
                escapedHeaders,
                alignStyles,
                cellStyle,
                isHeader = true,
            )
        val bodyCells =
            escapedRows.joinToString("\n") { row ->
                "<tr>${TranscriptBlockViewFactory.joinTableCells(row, alignStyles, cellStyle, isHeader = false)}</tr>"
            }
        val marginLeft = JBUI.scale(TABLE_LEFT_INSET)
        val html =
            TranscriptRenderHelpers.htmlDocumentStart() +
                "<table style='border-collapse:collapse;width:100%;margin-left:${marginLeft}px'>" +
                "<thead><tr>$headerCells</tr></thead>" +
                "<tbody>$bodyCells</tbody></table>" +
                TranscriptRenderHelpers.HTML_DOCUMENT_END
        return JEditorPane("text/html", html).apply {
            isEditable = false
            isOpaque = false
            border = EmptyBorder(0, 0, 0, 0)
            alignmentX = Component.LEFT_ALIGNMENT
            putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true)
        }
    }

    private data class MergedRun(
        val start: Int,
        val end: Int,
        val style: TextStyle,
        val url: String?,
    )

    private fun applyStyledRuns(
        pane: JTextPane,
        runs: List<StyledRun>,
    ) {
        val doc: StyledDocument = pane.styledDocument
        val sc =
            javax.swing.text.StyleContext
                .getDefaultStyleContext()

        val merged = mergeRuns(runs)

        for (run in merged) {
            val runStyle = sc.addStyle("run_${run.start}_${run.end}", null)
            applyStyleToRun(run.style, runStyle)
            doc.setCharacterAttributes(run.start, run.end - run.start, runStyle, false)
        }

        val linkRuns = merged.filter { it.style == TextStyle.LINK && it.url != null }
        if (linkRuns.isNotEmpty()) {
            pane.addMouseListener(
                object : MouseAdapter() {
                    override fun mouseClicked(event: MouseEvent) {
                        val pos = pane.viewToModel2D(event.point)
                        for (run in linkRuns) {
                            if (pos in run.start until run.end) {
                                TranscriptBlockViewFactory.tryOpenUrl(run.url!!)
                                break
                            }
                        }
                    }
                },
            )
        }
    }

    private fun mergeRuns(runs: List<StyledRun>): List<MergedRun> {
        if (runs.isEmpty()) return emptyList()

        val points = mutableSetOf(0, runs.maxOf { it.end })
        runs.forEach {
            points.add(it.start)
            points.add(it.end)
        }
        val sortedPoints = points.sorted()

        val segments = mutableListOf<MergedRun>()
        for (i in 0 until sortedPoints.size - 1) {
            val segStart = sortedPoints[i]
            val segEnd = sortedPoints[i + 1]
            if (segStart != segEnd) {
                val covering = runs.filter { it.start <= segStart && it.end >= segEnd }
                if (covering.isNotEmpty()) {
                    val finalStyle = pickStyle(covering.map { it.style })
                    val url = covering.firstOrNull { it.url != null }?.url
                    segments.add(MergedRun(segStart, segEnd, finalStyle, url))
                }
            }
        }

        return segments
    }
}
