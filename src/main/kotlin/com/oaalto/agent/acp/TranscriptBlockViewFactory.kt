package com.oaalto.agent.acp

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.serviceOrNull
import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.util.ui.JBUI
import com.oaalto.agent.AgentCliLog
import com.oaalto.agent.AgentCliSessionContext
import com.oaalto.agent.acp.plan.PlanPanel
import java.awt.BorderLayout
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
import javax.swing.SwingUtilities
import javax.swing.border.EmptyBorder
import javax.swing.text.StyleConstants
import javax.swing.text.StyledDocument

private const val MONO_FAMILY = "Monospaced"
private const val FONT_SIZE = 12
private const val BODY_PART_GAP = 4
private val ROW_BORDER = JBUI.Borders.empty(2, 0)
private const val LIST_LEFT_INSET = 16
private const val BLOCKQUOTE_LEFT_THICKNESS = 3
private const val BLOCKQUOTE_TEXT_INSET = 8
private const val BLOCKQUOTE_VERTICAL_PAD = 2
private const val THEMATIC_BREAK_VERTICAL_PAD = 4
private const val THEMATIC_BREAK_HORIZONTAL_INSET = 8
private const val MIN_HEADING_LEVEL = 1
private const val MAX_HEADING_LEVEL = 6
private val HEADING_SIZES = mapOf(1 to 18, 2 to 16, 3 to 14, 4 to 13, 5 to 12, 6 to 12)

/** Lazily accessed color provider for theme-aware colors */
private val colorProvider: TranscriptColorProvider
    get() = serviceOrNull<TranscriptColorProvider>() ?: DefaultTranscriptColorProvider()

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
        TextStyle.CODE -> applyInlineCodeStyle(runStyle)
        TextStyle.LINK -> {
            StyleConstants.setUnderline(runStyle, true)
            StyleConstants.setForeground(runStyle, colorProvider.getLinkForeground())
        }
        TextStyle.STRIKETHROUGH -> StyleConstants.setStrikeThrough(runStyle, true)
    }
}

private fun applyInlineCodeStyle(runStyle: javax.swing.text.Style) {
    StyleConstants.setFontFamily(runStyle, MONO_FAMILY)
    val scheme =
        ApplicationManager
            .getApplication()
            ?.let { EditorColorsManager.getInstance().globalScheme }
            ?: return
    val editorBackground =
        scheme.getColor(EditorColors.CARET_ROW_COLOR)
            ?: scheme.defaultBackground
    StyleConstants.setBackground(runStyle, editorBackground)
    StyleConstants.setForeground(runStyle, scheme.defaultForeground)
}

private data class MergedRun(
    val start: Int,
    val end: Int,
    val style: TextStyle,
    val url: String?,
)

private fun createAgentStyledTextPane(
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
            foreground = colorProvider.getTextForeground()
            alignmentX = Component.LEFT_ALIGNMENT
        }
    pane.text = text
    val validRuns = runs.filter { it.start < text.length && it.end <= text.length }
    applyStyledRuns(pane, validRuns)
    return pane
}

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
                            run.url?.let(TranscriptBlockViewFactory::tryOpenUrl)
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

/** Maps [TranscriptBlock] snapshots to Swing row components. */
internal class TranscriptBlockViewFactory(
    private val codeBlockViewFactory: TranscriptCodeBlockViewFactory,
    private val logContextProvider: () -> AgentCliSessionContext? = { null },
) {
    private val log = AgentCliLog.getInstance(TranscriptBlockViewFactory::class.java)

    fun create(
        block: TranscriptBlock,
        onToolToggle: (toolCallId: String) -> Unit,
    ): JPanel =
        when (block) {
            is TranscriptBlock.ToolCallBlock ->
                CollapsibleToolPanel(onToolToggle, codeBlockViewFactory).apply { bind(block) }
            is TranscriptBlock.PlanBlock ->
                PlanPanel().apply { bind(block) }
            else -> AgentTextRow(codeBlockViewFactory).apply { bind(block) }
        }

    fun update(
        component: JPanel,
        block: TranscriptBlock,
    ) {
        when {
            isToolCallMatch(component, block) ->
                (component as CollapsibleToolPanel).bind(
                    block as TranscriptBlock.ToolCallBlock,
                )
            isPlanMatch(component, block) ->
                (component as PlanPanel).bind(block as TranscriptBlock.PlanBlock)
            isTextRowMatch(component, block) -> (component as AgentTextRow).bind(block)
            isTypeMismatch(component, block) -> logTypeMismatch(component, block)
            else -> logTextRowMismatch(component, block)
        }
    }

    private fun isToolCallMatch(
        component: JPanel,
        block: TranscriptBlock,
    ): Boolean =
        component is CollapsibleToolPanel &&
            block is TranscriptBlock.ToolCallBlock

    private fun isPlanMatch(
        component: JPanel,
        block: TranscriptBlock,
    ): Boolean = component is PlanPanel && block is TranscriptBlock.PlanBlock

    private fun isTextRowMatch(
        component: JPanel,
        block: TranscriptBlock,
    ): Boolean =
        component is AgentTextRow &&
            block !is TranscriptBlock.ToolCallBlock &&
            block !is TranscriptBlock.PlanBlock

    private fun isTypeMismatch(
        component: JPanel,
        block: TranscriptBlock,
    ): Boolean =
        component is CollapsibleToolPanel ||
            component is PlanPanel ||
            block is TranscriptBlock.ToolCallBlock ||
            block is TranscriptBlock.PlanBlock

    private fun logTypeMismatch(
        component: JPanel,
        block: TranscriptBlock,
    ) {
        log.warn(
            "Transcript block/component type mismatch: " +
                "component=${component::class.simpleName}, block=${block::class.simpleName}",
            context = logContextProvider(),
        )
    }

    private fun logTextRowMismatch(
        component: JPanel,
        block: TranscriptBlock,
    ) {
        log.warn(
            "Transcript text row type mismatch: " +
                "component=${component::class.simpleName}, block=${block::class.simpleName}",
            context = logContextProvider(),
        )
    }

    fun disposeRow(component: JPanel) {
        when (component) {
            is CollapsibleToolPanel -> component.disposeCodeComponents()
            is AgentTextRow -> component.disposeCodeComponents()
        }
    }

    companion object {
        fun escapeHtml(text: String): String = TranscriptRenderHelpers.escapeHtml(text)

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
                val parts =
                    TranscriptContentRenderer.renderMarkdownText(
                        block.text,
                        ContentRenderOptions.AGENT_TEXT,
                    )
                if (parts.isNotEmpty() && renderedFinalText == block.text) {
                    widthAdjustment()
                    revalidate()
                    repaint()
                    return
                }
                rebuildBodyParts(parts)
                renderedFinalText = block.text
                if (disposableCodeComponents.isNotEmpty()) {
                    SwingUtilities.invokeLater {
                        widthAdjustment()
                        revalidate()
                        repaint()
                    }
                }
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
        contentColumn.components.filterIsInstance<JComponent>().forEach { child ->
            applyTranscriptColumnWidth(child, w)
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
                alignmentX = Component.LEFT_ALIGNMENT
            }
        apply(pane)
        contentColumn.add(pane)
    }

    private fun rebuildBodyParts(parts: List<TranscriptBodyPart>) {
        disposeCodeComponents()
        contentColumn.removeAll()
        val highlightedCount = IntArray(1)
        var needGap = false

        for (part in parts) {
            if (needGap) {
                contentColumn.add(Box.createVerticalStrut(JBUI.scale(BODY_PART_GAP)))
            }
            needGap = true
            contentColumn.add(renderPartToComponent(part, highlightedCount))
        }
    }

    private fun renderPartToComponent(
        part: TranscriptBodyPart,
        highlightedCount: IntArray,
    ): JComponent =
        when (part) {
            is TranscriptBodyPart.InlineText -> createAgentStyledTextPane(part.text, part.runs)
            is TranscriptBodyPart.Heading -> {
                val headingFont =
                    Font(
                        MONO_FAMILY,
                        Font.BOLD,
                        HEADING_SIZES.getValue(part.level.coerceIn(MIN_HEADING_LEVEL, MAX_HEADING_LEVEL)),
                    )
                createAgentStyledTextPane(part.text, part.runs, font = headingFont)
            }
            is TranscriptBodyPart.ListLine -> {
                val offsetRuns =
                    part.runs.map { run ->
                        StyledRun(run.start + part.marker.length, run.end + part.marker.length, run.style, run.url)
                    }
                createAgentStyledTextPane(
                    "${part.marker}${part.text}",
                    offsetRuns,
                    border = EmptyBorder(0, JBUI.scale(LIST_LEFT_INSET), 0, 0),
                )
            }
            is TranscriptBodyPart.Code -> renderCodeBlockBodyPart(part, highlightedCount)
            is TranscriptBodyPart.Html -> {
                val html =
                    TranscriptRenderHelpers.htmlDocumentStart() +
                        part.fragment +
                        TranscriptRenderHelpers.HTML_DOCUMENT_END
                createHtmlPane(html)
            }
            is TranscriptBodyPart.Image -> createImageLabelBodyPart(part)
            is TranscriptBodyPart.ThematicBreak -> createThematicBreak()
            is TranscriptBodyPart.BlockQuote -> createBlockQuoteBodyPart(part, highlightedCount)
        }

    private fun renderCodeBlockBodyPart(
        part: TranscriptBodyPart.Code,
        highlightedCount: IntArray,
    ): JComponent {
        if (highlightedCount[0] < TranscriptToolCallContentRenderer.MAX_HIGHLIGHTED_CODE_BLOCKS) {
            highlightedCount[0] += 1
            val codeComponent =
                codeBlockViewFactory
                    .createReadOnlyCodeBlock(part.languageId, part.code)
                    .also {
                        it.alignmentX = Component.LEFT_ALIGNMENT
                        val height = it.preferredSize.height.coerceAtLeast(1)
                        it.minimumSize = Dimension(0, height)
                        it.maximumSize = Dimension(Int.MAX_VALUE, height)
                    }
            disposableCodeComponents += codeComponent
            return codeComponent
        }
        return createHtmlPane(
            TranscriptRenderHelpers.htmlDocumentStart() +
                TranscriptHtmlBuilder.buildPlainPre(part.code) +
                TranscriptRenderHelpers.HTML_DOCUMENT_END,
        )
    }

    private fun createImageLabelBodyPart(part: TranscriptBodyPart.Image): JComponent {
        val displayText = "[image: ${part.altText}]"
        val url = part.url
        return JLabel(displayText).apply {
            isOpaque = false
            font = Font(MONO_FAMILY, Font.ITALIC, FONT_SIZE)
            foreground = colorProvider.getThoughtColor()
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

    private fun createBlockQuoteBodyPart(
        part: TranscriptBodyPart.BlockQuote,
        highlightedCount: IntArray,
    ): JPanel {
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
                for (inner in part.parts) {
                    if (needGap) {
                        add(Box.createVerticalStrut(JBUI.scale(BODY_PART_GAP)))
                    }
                    needGap = true
                    add(renderPartToComponent(inner, highlightedCount))
                }
            }
        val leftBorderLine =
            JLabel().apply {
                isOpaque = true
                background = colorProvider.getThoughtColor()
                preferredSize = Dimension(BLOCKQUOTE_LEFT_THICKNESS, 1)
                minimumSize = Dimension(BLOCKQUOTE_LEFT_THICKNESS, 1)
                maximumSize = Dimension(BLOCKQUOTE_LEFT_THICKNESS, Int.MAX_VALUE)
            }
        borderPanel.add(leftBorderLine, BorderLayout.WEST)
        borderPanel.add(quoteContent, BorderLayout.CENTER)
        panel.add(borderPanel)
        return panel
    }
}
