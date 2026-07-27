package com.oaalto.agent.acp

import com.oaalto.agent.acp.transport.AcpProcessTransport
import com.oaalto.agent.acp.transport.ProcessStdioTransport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class AcpSessionControllerImpl(
    private val listener: AcpSessionListener,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
    private val sessionOperationsFactory: AcpClientSessionOperationsFactory,
    private val transport: AcpProcessTransport = ProcessStdioTransport(scope),
) : AcpSessionController {
    private val connectionBootstrap = AcpConnectionBootstrap(listener)
    private val sessionLifecycle = AcpSessionLifecycle(sessionOperationsFactory)
    private val promptExecutor =
        AcpPromptExecutor(scope, listener) { sessionLifecycle.sessionLogContext() }
    private var editorContext: AcpEditorContext? = null

    override suspend fun start(request: AcpSessionStartRequest): AcpSessionStartResult {
        disposeTransportOnly()
        editorContext = request.editorContext

        // Connect and bootstrap
        runCatching {
            sessionLifecycle.prepareForConnect(request.launchPlan, request.editorContext)
            val connected =
                transport.connect(
                    launchPlan = request.launchPlan,
                    listener = listener,
                    sessionLogContext = { sessionLifecycle.sessionLogContext() },
                )
            connectionBootstrap.initialize(connected.protocol, connected.client, request.editorContext)
            sessionLifecycle.bindClient(connected.client)
        }.onFailure { throwable ->
            sessionLifecycle.failSessionReady(throwable)
            disposeTransportOnly()
            throw throwable
        }

        // Resume orchestration
        val resumeResult =
            sessionLifecycle.startSession(request.resumePlan, request.sessionPicker)

        return when (resumeResult) {
            is com.oaalto.agent.worktree.resume.AcpSessionOpenResult.Success -> {
                val status =
                    if (resumeResult.pickerShown) {
                        "Resumed session ${resumeResult.sessionId}."
                    } else {
                        ""
                    }
                AcpSessionStartResult(
                    sessionId = resumeResult.sessionId,
                    statusMessage = status,
                    restoreTranscript = true,
                )
            }
            is com.oaalto.agent.worktree.resume.AcpSessionOpenResult.Fallback ->
                AcpSessionStartResult(
                    sessionId = sessionLifecycle.currentSessionId(),
                    statusMessage = resumeResult.reason,
                )
            com.oaalto.agent.worktree.resume.AcpSessionOpenResult.StartFresh ->
                AcpSessionStartResult(
                    sessionId = sessionLifecycle.currentSessionId(),
                    statusMessage = "Started a new ACP session.",
                )
        }
    }

    override suspend fun prompt(text: String) {
        promptExecutor.prompt(
            awaitOpenSession = { sessionLifecycle.awaitOpenSession() },
            sessionProvider = { sessionLifecycle.activeSession() },
            text = text,
        )
    }

    override suspend fun cancelPrompt() {
        promptExecutor.cancelPrompt(sessionLifecycle.activeSession())
    }

    override fun dispose() {
        promptExecutor.disposePromptWork()
        editorContext?.terminalSessionRegistry?.clear()
        editorContext?.shellPaneHost?.clear()
        runBlocking {
            withContext(Dispatchers.IO) {
                runCatching { sessionLifecycle.activeSession()?.cancel() }
            }
        }
        disposeTransportOnly()
        scope.cancel()
    }

    private fun disposeTransportOnly() {
        transport.dispose()
        sessionLifecycle.resetOnDispose()
        promptExecutor.disposePromptWork()
        editorContext = null
    }
}
