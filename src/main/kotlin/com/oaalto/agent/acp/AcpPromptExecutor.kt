package com.oaalto.agent.acp

import com.agentclientprotocol.client.ClientSession
import com.agentclientprotocol.common.Event
import com.agentclientprotocol.model.ContentBlock
import com.oaalto.agent.AgentCliLog
import com.oaalto.agent.AgentCliSessionContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class AcpPromptExecutor(
    private val scope: CoroutineScope,
    private val listener: AcpSessionListener,
    private val sessionLogContext: () -> AgentCliSessionContext,
) {
    private var promptJob: Job? = null

    suspend fun prompt(
        awaitOpenSession: suspend () -> Unit,
        sessionProvider: () -> ClientSession?,
        text: String,
    ) {
        awaitOpenSession()
        val activeSession = sessionProvider() ?: error("ACP session is not open")
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return

        promptJob?.cancel()
        listener.onStructuredUpdate(StructuredUpdate.FinalizeAgentStream)
        promptJob =
            scope.launch {
                runCatching {
                    activeSession.prompt(listOf(ContentBlock.Text(trimmed))).collect { event ->
                        dispatchPromptEvent(event)
                    }
                }.onFailure { throwable ->
                    log.warn("ACP prompt failed", throwable, sessionLogContext())
                    listener.onStructuredUpdate(StructuredUpdate.FinalizeAgentStream)
                    listener.onError(throwable.message ?: throwable.javaClass.simpleName)
                }
            }
        promptJob?.join()
    }

    suspend fun cancelPrompt(activeSession: ClientSession?) {
        listener.onStructuredUpdate(StructuredUpdate.FinalizeAgentStream)
        promptJob?.cancel()
        activeSession?.cancel()
    }

    fun disposePromptWork() {
        listener.onStructuredUpdate(StructuredUpdate.FinalizeAgentStream)
        promptJob?.cancel()
        promptJob = null
    }

    private fun dispatchPromptEvent(event: Event) {
        when (event) {
            is Event.SessionUpdateEvent ->
                TranscriptEventIngestion.ingest(event.update).forEach(listener::onStructuredUpdate)
            is Event.PromptResponseEvent ->
                TranscriptEventIngestion.ingestPromptCompleted().forEach(listener::onStructuredUpdate)
        }
    }

    companion object {
        private val log = AgentCliLog.getInstance(AcpPromptExecutor::class.java)
    }
}
