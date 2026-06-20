package com.oaalto.agent.acp

interface AcpSessionListener {
    /** Applies a normalized transcript delta to the structured view pipeline. */
    fun onStructuredUpdate(update: StructuredUpdate)

    fun onError(message: String)
}
