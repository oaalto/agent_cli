package com.oaalto.agent.acp

/** Shared transcript text length limits for prose and code blocks. */
internal object TranscriptTextTruncation {
    fun truncate(
        text: String,
        maxCharacters: Int = TranscriptToolCallContentRenderer.MAX_TEXT_CHARACTERS,
    ): String {
        if (text.length <= maxCharacters) {
            return text
        }
        val suffix = "\n… (truncated, ${text.length} characters total)"
        val keepLength = (maxCharacters - suffix.length).coerceAtLeast(0)
        return text.take(keepLength) + suffix
    }
}
