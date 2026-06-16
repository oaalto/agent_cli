package com.oaalto.agent.acp

interface AcpSessionListener {
    fun onTranscriptAppend(text: String)

    fun onTranscriptLine(line: String)

    fun onError(message: String)
}
