package com.oaalto.agent.acp

interface AcpSessionListener {
    /** Applies a normalized transcript delta to the structured view pipeline. */
    fun onStructuredUpdate(update: StructuredUpdate)

    fun onError(message: String)

    /** Called when accumulated usage statistics are updated. */
    fun onUsageUpdate(usage: AccumulatedUsage)
}
