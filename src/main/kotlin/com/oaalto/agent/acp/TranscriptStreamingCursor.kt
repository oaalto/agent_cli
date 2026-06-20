package com.oaalto.agent.acp

/**
 * Pure HTML operations for the in-progress agent message streaming cursor.
 *
 * Chunk text passed into these helpers must already be HTML-escaped.
 * Avoid custom element attributes here — Swing's HTML editor drops unknown attrs.
 *
 * The cursor is rendered as an HTML entity (`&#9612;`) rather than a literal
 * unicode character so that JEditorPane does not strip it during round-trips.
 */
internal object TranscriptStreamingCursor {
    /** Literal cursor glyph for Swing labels. */
    const val CURSOR_CHAR: String = "\u258A"

    /** HTML entity for U+258A (▊) — survives JEditorPane HTML round-trip. */
    const val CURSOR_HTML: String = "&#9612;"

    private const val AGENT_SPAN_OPEN: String =
        "<span style=\"color:#d4d4d4;font-family:monospace;font-size:12px\">"

    /** Builds an agent stream block with cursor at the live edge. */
    fun streamBlockHtml(
        escapedText: String,
        lineSeparator: String,
    ): String = lineSeparator + AGENT_SPAN_OPEN + escapedText + CURSOR_HTML + "</span>"

    /** Builds a finalized agent block without the cursor. */
    fun finalizedBlockHtml(
        escapedText: String,
        lineSeparator: String,
    ): String = lineSeparator + AGENT_SPAN_OPEN + escapedText + "</span>"

    /** Strips the cursor entity from [html], leaving agent text intact. */
    fun stripCursor(html: String): String = html.replace(CURSOR_HTML, "")

    /** Detects cursor in any form: decimal entity, hex entity, or literal character. */
    fun hasCursor(html: String): Boolean =
        html.contains(CURSOR_HTML) ||
            html.contains("&#x258a;", ignoreCase = true) ||
            html.contains("\u258a")
}
