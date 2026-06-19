package com.oaalto.agent.acp

import com.agentclientprotocol.model.SessionUpdate

/**
 * Routes ACP prompt [SessionUpdate] events to transcript listener callbacks,
 * finalizing any active agent stream before non-chunk updates.
 */
internal object AcpPromptEventDispatcher {
    fun dispatchSessionUpdate(
        update: SessionUpdate,
        listener: AcpSessionListener,
    ) {
        when (update) {
            is SessionUpdate.AgentMessageChunk -> {
                TranscriptRenderer.renderEventText(update)?.let(listener::onTranscriptAppend)
            }
            else -> {
                listener.onFinalizeAgentStream()
                TranscriptRenderer.renderUpdate(update).forEach(listener::onTranscriptHtml)
            }
        }
    }

    fun dispatchPromptCompleted(listener: AcpSessionListener) {
        listener.onFinalizeAgentStream()
    }
}
