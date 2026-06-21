package com.oaalto.agent.acp

/** One renderable unit in a tool card body or legacy HTML transcript line. */
sealed class TranscriptBodyPart {
    data class Html(
        val fragment: String,
    ) : TranscriptBodyPart()

    data class Code(
        val languageId: String?,
        val code: String,
    ) : TranscriptBodyPart()
}
