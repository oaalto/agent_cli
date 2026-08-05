package com.oaalto.agent.acp.transcript.view.rows

/** Live-path streaming cursor indicator for Swing text labels. */
internal object TranscriptStreamingCursor {
    /** Literal cursor glyph (U+258A, ▊) used by [AgentTextRowAdapter] for streaming blocks. */
    const val CURSOR_CHAR: String = "\u258A"
}
