package com.oaalto.agent.acp

import com.oaalto.agent.acp.transcript.model.StructuredUpdate

interface AcpSessionListener {
    /** Applies a normalized transcript delta to the structured view pipeline. */
    fun onStructuredUpdate(update: StructuredUpdate)

    fun onError(message: String)

    /** Called when accumulated usage statistics are updated. */
    fun onUsageUpdate(usage: AccumulatedUsage)
}
