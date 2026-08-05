package com.oaalto.agent.acp.transcript.render

import com.intellij.openapi.components.serviceOrNull
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.oaalto.agent.acp.transcript.model.TranscriptBodyPart
import com.oaalto.agent.acp.transcript.theme.DefaultTranscriptColorProvider
import com.oaalto.agent.acp.transcript.theme.TranscriptColorProvider
import java.util.Locale

internal object TranscriptHtmlBuilder {
    private val fallbackProvider: TranscriptColorProvider by lazy { DefaultTranscriptColorProvider() }

    private fun getProvider(): TranscriptColorProvider = serviceOrNull<TranscriptColorProvider>() ?: fallbackProvider

    /**
     * Returns theme-aware CSS style for inline code elements.
     *
     * Uses the IDE's editor color scheme when available, falling back to
     * the TranscriptColorProvider colors. This ensures code blocks respect
     * the current editor theme and update dynamically when the theme changes.
     */
    private fun codeBlockStyle(): String {
        val provider = getProvider()
        val scheme =
            try {
                EditorColorsManager.getInstance().globalScheme
            } catch (_: Exception) {
                null
            }

        val backgroundColor =
            if (scheme != null) {
                scheme.getColor(EditorColors.CARET_ROW_COLOR)
                    ?: scheme.defaultBackground
            } else {
                provider.getPanelBackground()
            }
        val foregroundColor =
            if (scheme != null) {
                scheme.getAttributes(DefaultLanguageHighlighterColors.STRING)?.foregroundColor
                    ?: scheme.defaultForeground
            } else {
                provider.getTextForeground()
            }

        return String.format(
            Locale.US,
            "background:%s;padding:1px 4px;border-radius:2px;color:%s",
            provider.toHtml(backgroundColor),
            provider.toHtml(foregroundColor),
        )
    }

    private const val PRE_STYLE =
        "margin-left:20px;color:#999999;border-left:2px solid #444444;" +
            "padding-left:8px;font-family:monospace;font-size:12px;white-space:pre-wrap"
    private const val MUTED_REFERENCE_STYLE =
        "margin-left:20px;color:#999999;font-family:monospace;font-size:12px"

    fun buildPlainPre(
        text: String,
        maxCharacters: Int = TranscriptContentRenderer.MAX_TEXT_CHARACTERS,
    ): String {
        val openTag = "<pre style=\"$PRE_STYLE\">"
        val closeTag = "</pre>"
        val maxInnerChars =
            (
                maxCharacters -
                    openTag.length -
                    closeTag.length
            ).coerceAtLeast(0)
        val displayText = TranscriptTextTruncation.truncate(text, maxInnerChars)
        return openTag + TranscriptRenderHelpers.escapeHtml(displayText) + closeTag
    }

    fun buildMutedSpan(text: String): String =
        "<span style=\"$MUTED_REFERENCE_STYLE;white-space:pre-wrap\">${TranscriptRenderHelpers.escapeHtml(text)}</span>"

    fun buildStyledSpan(
        text: String,
        runs: List<StyledRun>,
        isHeading: Boolean,
    ): String {
        if (runs.isEmpty()) {
            return buildMutedSpan(text)
        }
        val segments = computeStyledSegments(text, runs)
        val htmlContent = buildStyledHtml(segments)
        val weight = if (isHeading) "font-weight:bold;" else ""
        return "<span style=\"$MUTED_REFERENCE_STYLE;${weight}white-space:pre-wrap\">$htmlContent</span>"
    }

    private fun computeStyledSegments(
        text: String,
        runs: List<StyledRun>,
    ): List<Pair<String, TextStyle?>> {
        val points = mutableSetOf(0, text.length)
        runs.forEach {
            points.add(it.start)
            points.add(it.end)
        }
        val sortedPoints = points.sorted()

        return sortedPoints
            .zipWithNext()
            .mapNotNull { (segStart, segEnd) ->
                if (segStart == segEnd) return@mapNotNull null
                val covering = runs.filter { it.start <= segStart && it.end >= segEnd }
                val segmentText = text.substring(segStart, segEnd)
                segmentText to covering.firstOrNull()?.style
            }
    }

    private fun buildStyledHtml(segments: List<Pair<String, TextStyle?>>): String =
        segments.joinToString("") { (segmentText, style) ->
            renderStyleTag(TranscriptRenderHelpers.escapeHtml(segmentText), style)
        }

    private fun renderStyleTag(
        escaped: String,
        style: TextStyle?,
    ): String =
        when (style) {
            TextStyle.BOLD -> "<strong>$escaped</strong>"
            TextStyle.ITALIC -> "<em>$escaped</em>"
            TextStyle.BOLD_ITALIC -> "<strong><em>$escaped</em></strong>"
            TextStyle.CODE -> "<code style=\"${codeBlockStyle()}\">$escaped</code>"
            TextStyle.LINK -> "<span style='color:#569CD6;text-decoration:underline'>$escaped</span>"
            TextStyle.STRIKETHROUGH -> "<s>$escaped</s>"
            null -> escaped
        }

    fun bodyPartToHtmlFragment(part: TranscriptBodyPart): String =
        when (part) {
            is TranscriptBodyPart.Html -> part.fragment
            is TranscriptBodyPart.Code -> buildPlainPre(part.code)
            is TranscriptBodyPart.Heading -> buildStyledSpan(part.text, part.runs, isHeading = true)
            is TranscriptBodyPart.ListLine -> {
                val offsetRuns =
                    part.runs.map { run ->
                        StyledRun(
                            run.start + part.marker.length,
                            run.end + part.marker.length,
                            run.style,
                            run.url,
                        )
                    }
                buildStyledSpan("${part.marker}${part.text}", offsetRuns, isHeading = false)
            }
            is TranscriptBodyPart.InlineText -> buildStyledSpan(part.text, part.runs, isHeading = false)
            is TranscriptBodyPart.BlockQuote -> {
                val inner = part.parts.joinToString("") { bodyPartToHtmlFragment(it) }
                "<div style='margin-left:20px;border-left:2px solid #444;padding-left:8px'>$inner</div>"
            }
            is TranscriptBodyPart.ThematicBreak ->
                "<hr style='border:none;border-top:1px solid #444;margin:4px 8px'/>"
            is TranscriptBodyPart.Image -> buildMutedSpan("[image: ${part.altText}]")
        }
}
