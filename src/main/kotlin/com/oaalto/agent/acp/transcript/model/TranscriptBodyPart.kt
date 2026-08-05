package com.oaalto.agent.acp.transcript.model

import com.oaalto.agent.acp.StyledRun

/**
 * One renderable unit in a tool card body or agent-text row.
 *
 * Variants cover the full markdown grammar handled by [TranscriptMarkdownRenderer.parseToBlocks]:
 * inline text with styled runs, headings, list lines, fenced code, tables (HTML),
 * block quotes, thematic breaks, images, and raw HTML fragments.
 */
sealed class TranscriptBodyPart {
    /** Raw HTML fragment, self-contained (no wrapping `<html>` document). */
    data class Html(
        val fragment: String,
    ) : TranscriptBodyPart()

    /** Highlighted fenced code block; [languageId] may be null. */
    data class Code(
        val languageId: String?,
        val code: String,
    ) : TranscriptBodyPart()

    /** A heading line (level 1–6) with inline styled text. */
    data class Heading(
        val level: Int,
        val text: String,
        val runs: List<StyledRun>,
    ) : TranscriptBodyPart()

    /** A list line with its marker (bullet or "n. ") and inline styled text. */
    data class ListLine(
        val marker: String,
        val text: String,
        val runs: List<StyledRun>,
    ) : TranscriptBodyPart()

    /** A paragraph / prose line with inline styled text (no heading/list decoration). */
    data class InlineText(
        val text: String,
        val runs: List<StyledRun>,
    ) : TranscriptBodyPart()

    /** A block quote wrapping a list of inner body parts. */
    data class BlockQuote(
        val parts: List<TranscriptBodyPart>,
    ) : TranscriptBodyPart()

    /** A horizontal rule (thematic break). */
    data object ThematicBreak : TranscriptBodyPart()

    /** An image placeholder — rendered as muted text with optional URL tooltip. */
    data class Image(
        val altText: String,
        val url: String,
    ) : TranscriptBodyPart()
}
