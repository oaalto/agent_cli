package com.oaalto.agent.acp

interface AcpSessionListener {
    /** Appends plain text that must be HTML-escaped before display (e.g. streaming chunks). */
    fun onTranscriptAppend(text: String)

    /** Removes the streaming cursor from the active agent message block, if any. */
    fun onFinalizeAgentStream()

    /** Appends a pre-rendered HTML fragment (trusted, no escaping). */
    fun onTranscriptHtml(fragment: String)

    /** Appends a plain-text line that must be HTML-escaped before display (e.g. status messages). */
    fun onTranscriptPlainLine(line: String)

    fun onError(message: String)
}
