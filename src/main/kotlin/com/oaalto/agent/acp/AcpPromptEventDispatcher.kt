package com.oaalto.agent.acp

import com.agentclientprotocol.model.SessionUpdate

/**
 * Routes ACP prompt [SessionUpdate] events to structured transcript updates,
 * finalizing any active agent stream before non-chunk updates.
 */
internal object AcpPromptEventDispatcher {
    fun dispatchSessionUpdate(
        update: SessionUpdate,
        listener: AcpSessionListener,
    ) {
        when (update) {
            is SessionUpdate.AgentMessageChunk -> {
                TranscriptSessionUpdateMapper.mapAgentChunk(update)?.let(listener::onStructuredUpdate)
            }
            else -> {
                listener.onStructuredUpdate(StructuredUpdate.FinalizeAgentStream)
                TranscriptSessionUpdateMapper.mapUpdate(update).forEach(listener::onStructuredUpdate)
            }
        }
    }

    fun dispatchPromptCompleted(listener: AcpSessionListener) {
        listener.onStructuredUpdate(StructuredUpdate.FinalizeAgentStream)
    }
}
