package com.oaalto.agent.acp

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.util.ui.JBUI
import java.awt.Component
import java.awt.Desktop
import java.awt.Dimension
import java.awt.Font
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

/** Render profile that controls profile-specific body-part fallbacks. */
internal enum class BodyPartRenderProfile {
    /** Agent text row: rich widgets for all part kinds, hyperlinks, clickable images. */
    AGENT,

    /** Tool card body: shared parts only, unknown parts fall back to HTML. */
    TOOL,
}

private const val MONO_FAMILY = "Monospaced"
private const val FONT_SIZE = 12
private const val BODY_PART_GAP = 4
private const val LIST_LEFT_INSET = 16
private const val BLOCKQUOTE_LEFT_THICKNESS = 3
private const val BLOCKQUOTE_TEXT_INSET = 8
private const val BLOCKQUOTE_VERTICAL_PAD = 2
private const val THEMATIC_BREAK_VERTICAL_PAD = 4
private const val THEMATIC_BREAK_HORIZONTAL_INSET = 8
private const val MIN_HEADING_LEVEL = 1
private const val MAX_HEADING_LEVEL = 6
private val HEADING_SIZES = mapOf(1 to 18, 2 to 16, 3 to 14, 4 to 13, 5 to 12, 6 to 12)

/**
 * Maps [TranscriptBodyPart] lists to Swing widgets shared by agent-text rows and tool-card bodies.
 *
 * [profile] controls profile-specific fallbacks:
 * - [BodyPartRenderProfile.AGENT] renders all part kinds with rich widgets and hyperlink support.
 * - [BodyPartRenderProfile.TOOL] falls back to HTML for agent-only part kinds.
 */
internal class TranscriptBodyPartWidgetMapper(
    private val codeBlockViewFactory: TranscriptCodeBlockViewFactory,
    internal val colorProvider: TranscriptColorProvider,
    private val profile: BodyPartRenderProfile,
) {
    /**
     * Build a column of widgets for [parts].
     *
     * Returns a pair: (column panel, list of disposable code components).
     * The caller adds the column to its row and tracks the disposable components.
     */
    fun buildBodyColumn(parts: List<TranscriptBodyPart>): Pair<JPanel, MutableList<JComponent>> {
        val column =
            JPanel().apply {
                layout = BoxLayout(this, BoxLayout.Y_AXIS)
                isOpaque = false
                alignmentX = Component.LEFT_ALIGNMENT
            }
        val disposables = mutableListOf<JComponent>()
        val highlightedCount = IntArray(1)
        var needGap = false

        for (part in parts) {
            if (needGap) {
                column.add(Box.createVerticalStrut(JBUI.scale(BODY_PART_GAP)))
            }
            needGap = true
            val widgets = renderPart(part, highlightedCount, disposables)
            widgets.forEach(column::add)
        }

        return column to disposables
    }

    private fun renderPart(
        part: TranscriptBodyPart,
        highlightedCount: IntArray,
        disposables: MutableList<JComponent>,
    ): List<JComponent> {
        val agentWidgets = renderAgentPart(part)
        if (agentWidgets.isNotEmpty()) return agentWidgets
        return renderSharedPart(part, highlightedCount, disposables)
    }

    private fun renderAgentPart(part: TranscriptBodyPart): List<JComponent> =
        if (profile != BodyPartRenderProfile.AGENT) {
            emptyList()
        } else {
            when (part) {
                is TranscriptBodyPart.InlineText ->
                    listOf(createStyledTextPane(part.text, part.runs))
                is TranscriptBodyPart.Heading -> {
                    val headingFont =
                        Font(
                            MONO_FAMILY,
                            Font.BOLD,
                            HEADING_SIZES.getValue(part.level.coerceIn(MIN_HEADING_LEVEL, MAX_HEADING_LEVEL)),
                        )
                    listOf(createStyledTextPane(part.text, part.runs, font = headingFont))
                }
                is TranscriptBodyPart.ListLine -> {
                    val offsetRuns =
                        part.runs.map { run ->
                            StyledRun(run.start + part.marker.length, run.end + part.marker.length, run.style, run.url)
                        }
                    listOf(
                        createStyledTextPane(
                            "${part.marker}${part.text}",
                            offsetRuns,
                            border = EmptyBorder(0, JBUI.scale(LIST_LEFT_INSET), 0, 0),
                        ),
                    )
                }
                is TranscriptBodyPart.Image ->
                    listOf(createImageLabel(part))
                else -> emptyList()
            }
        }

    private fun renderSharedPart(
        part: TranscriptBodyPart,
        highlightedCount: IntArray,
        disposables: MutableList<JComponent>,
    ): List<JComponent> =
        when (part) {
            is TranscriptBodyPart.Code ->
                listOf(renderCodeBlock(part, highlightedCount, disposables))
            is TranscriptBodyPart.Html ->
                listOf(createHtmlPane(part.fragment))
            is TranscriptBodyPart.ThematicBreak ->
                listOf(createThematicBreak())
            is TranscriptBodyPart.BlockQuote ->
                listOf(createBlockQuote(part, highlightedCount, disposables))
            else ->
                listOf(createHtmlPane(TranscriptHtmlBuilder.bodyPartToHtmlFragment(part)))
        }

    private fun renderCodeBlock(
        part: TranscriptBodyPart.Code,
        highlightedCount: IntArray,
        disposables: MutableList<JComponent>,
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
            disposables += codeComponent
            return codeComponent
        }
        return createHtmlPane(TranscriptHtmlBuilder.buildPlainPre(part.code))
    }

    private fun createBlockQuote(
        part: TranscriptBodyPart.BlockQuote,
        highlightedCount: IntArray,
        disposables: MutableList<JComponent>,
    ): JComponent {
        if (profile == BodyPartRenderProfile.AGENT) {
            return createAgentBlockQuote(part, highlightedCount, disposables)
        }
        // Tool profile: flatten inner parts into a simple container
        val container =
            JPanel().apply {
                layout = BoxLayout(this, BoxLayout.Y_AXIS)
                isOpaque = false
                alignmentX = Component.LEFT_ALIGNMENT
            }
        var needGap = false
        for (inner in part.parts) {
            if (needGap) {
                container.add(Box.createVerticalStrut(JBUI.scale(BODY_PART_GAP)))
            }
            needGap = true
            val widgets = renderPart(inner, highlightedCount, disposables)
            widgets.forEach(container::add)
        }
        return container
    }

    private fun createAgentBlockQuote(
        part: TranscriptBodyPart.BlockQuote,
        highlightedCount: IntArray,
        disposables: MutableList<JComponent>,
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
            JPanel(java.awt.BorderLayout()).apply {
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
                    val widgets = renderPart(inner, highlightedCount, disposables)
                    widgets.forEach(::add)
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
        borderPanel.add(leftBorderLine, java.awt.BorderLayout.WEST)
        borderPanel.add(quoteContent, java.awt.BorderLayout.CENTER)
        panel.add(borderPanel)
        return panel
    }

    private fun createImageLabel(part: TranscriptBodyPart.Image): JComponent {
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
                            tryOpenUrl(url)
                        }
                    },
                )
            }
        }
    }

    companion object {
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

// Shared styling helpers used by TranscriptBodyPartWidgetMapper

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

private fun applyStyleToRun(
    style: TextStyle,
    runStyle: javax.swing.text.Style,
    colorProvider: TranscriptColorProvider,
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

private fun TranscriptBodyPartWidgetMapper.createStyledTextPane(
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
    applyStyledRuns(pane, validRuns, colorProvider)
    return pane
}

private fun applyStyledRuns(
    pane: JTextPane,
    runs: List<StyledRun>,
    colorProvider: TranscriptColorProvider,
) {
    val doc: StyledDocument = pane.styledDocument
    val sc =
        javax.swing.text.StyleContext
            .getDefaultStyleContext()

    val merged = mergeRuns(runs)

    for (run in merged) {
        val runStyle = sc.addStyle("run_${run.start}_${run.end}", null)
        applyStyleToRun(run.style, runStyle, colorProvider)
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
                            run.url?.let(TranscriptBodyPartWidgetMapper::tryOpenUrl)
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

private fun TranscriptBodyPartWidgetMapper.createHtmlPane(html: String): JEditorPane =
    JEditorPane("text/html", html).apply {
        isEditable = false
        isOpaque = false
        border = EmptyBorder(0, 0, 0, 0)
        alignmentX = Component.LEFT_ALIGNMENT
        putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true)
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
