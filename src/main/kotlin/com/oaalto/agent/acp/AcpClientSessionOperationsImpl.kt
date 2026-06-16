package com.oaalto.agent.acp

import com.agentclientprotocol.common.ClientSessionOperations
import com.agentclientprotocol.model.PermissionOption
import com.agentclientprotocol.model.RequestPermissionOutcome
import com.agentclientprotocol.model.RequestPermissionResponse
import com.agentclientprotocol.model.SessionUpdate
import kotlinx.serialization.json.JsonElement

class AcpClientSessionOperationsImpl(
    private val listener: AcpSessionListener,
) : ClientSessionOperations {
    override suspend fun requestPermissions(
        toolCall: SessionUpdate.ToolCallUpdate,
        permissions: List<PermissionOption>,
        _meta: JsonElement?,
    ): RequestPermissionResponse {
        listener.onTranscriptLine(
            TranscriptRenderer.formatPermissionRequest(toolCall.title ?: toolCall.toolCallId.value),
        )
        return RequestPermissionResponse(RequestPermissionOutcome.Cancelled)
    }

    override suspend fun notify(
        notification: SessionUpdate,
        _meta: JsonElement?,
    ) {
        TranscriptRenderer.renderUpdate(notification).forEach(listener::onTranscriptLine)
    }
}
