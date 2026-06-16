package com.oaalto.agent.acp

import com.agentclientprotocol.agent.AgentInfo
import com.agentclientprotocol.client.Client
import com.agentclientprotocol.client.ClientInfo
import com.agentclientprotocol.client.ClientOperationsFactory
import com.agentclientprotocol.client.ClientSession
import com.agentclientprotocol.common.Event
import com.agentclientprotocol.common.SessionCreationParameters
import com.agentclientprotocol.model.ContentBlock
import com.agentclientprotocol.model.Implementation
import com.agentclientprotocol.model.SessionId
import com.agentclientprotocol.model.SessionUpdate
import com.agentclientprotocol.protocol.Protocol
import com.agentclientprotocol.transport.StdioTransport
import com.intellij.openapi.diagnostic.Logger
import com.oaalto.agent.acp.auth.AuthFlowCoordinator
import com.oaalto.agent.worktree.resume.SessionSummary
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.util.concurrent.TimeUnit

class AcpSessionControllerImpl(
    private val listener: AcpSessionListener,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) : AcpSessionController {
    private var process: Process? = null
    private var protocol: Protocol? = null
    private var client: Client? = null
    private var session: ClientSession? = null
    private var launchPlan: AcpLaunchPlan? = null
    private var editorContext: AcpEditorContext? = null
    private var agentInfo: AgentInfo? = null
    private var promptJob: Job? = null
    private var stderrJob: Job? = null
    private var exitJob: Job? = null
    private var sessionReady: CompletableDeferred<Unit> = CompletableDeferred()

    @Suppress("DEPRECATION", "OPT_IN_USAGE")
    override suspend fun connect(
        launchPlan: AcpLaunchPlan,
        editorContext: AcpEditorContext,
    ) {
        disposeTransportOnly()
        this.launchPlan = launchPlan
        this.editorContext = editorContext
        sessionReady = CompletableDeferred()

        val startedProcess =
            ProcessBuilder(launchPlan.command)
                .directory(Path.of(launchPlan.processWorkingDirectory).toFile())
                .redirectInput(ProcessBuilder.Redirect.PIPE)
                .redirectOutput(ProcessBuilder.Redirect.PIPE)
                .redirectError(ProcessBuilder.Redirect.PIPE)
                .start()

        val transport =
            StdioTransport(
                parentScope = scope,
                ioDispatcher = Dispatchers.IO,
                input = startedProcess.inputStream.asSource().buffered(),
                output = startedProcess.outputStream.asSink().buffered(),
                name = "acp-agent-stdio",
            )
        val protocolInstance = Protocol(scope, transport)
        val clientInstance = Client(protocolInstance)

        try {
            protocolInstance.start()
            val capabilities =
                AcpClientCapabilities.build(AcpClientCapabilities.fullSupport)
            val info =
                clientInstance.initialize(
                    ClientInfo(
                        capabilities = capabilities,
                        implementation = Implementation(name = "agent-cli-plugin", version = "3.0"),
                    ),
                )
            agentInfo = info
            val authCoordinator =
                AuthFlowCoordinator(
                    client = clientInstance,
                    agentInfo = info,
                    shellPaneHost = editorContext.shellPaneHost,
                    authPromptUi = editorContext.authPromptUi,
                    listener = listener,
                )
            authCoordinator.authenticateIfRequired().getOrElse { throwable ->
                sessionReady.completeExceptionally(throwable)
                disposeTransportOnly()
                throw throwable
            }
        } catch (throwable: Throwable) {
            sessionReady.completeExceptionally(throwable)
            disposeTransportOnly()
            throw throwable
        }

        process = startedProcess
        protocol = protocolInstance
        client = clientInstance

        stderrJob =
            scope.launch {
                monitorStderr(startedProcess)
            }
        exitJob =
            scope.launch {
                val exitCode = startedProcess.waitFor()
                if (scope.isActive) {
                    listener.onError("Agent process exited with code $exitCode.")
                }
            }
    }

    override suspend fun newSession() {
        openSession { activeClient, context, cwd, operationsFactory ->
            activeClient.newSession(
                SessionCreationParameters(cwd = cwd, mcpServers = emptyList()),
                operationsFactory,
            )
        }
    }

    override suspend fun loadSession(sessionId: String) {
        val normalizedId = sessionId.trim()
        if (normalizedId.isBlank()) error("ACP session id is blank")
        openSession { activeClient, _, cwd, operationsFactory ->
            activeClient.loadSession(
                SessionId(normalizedId),
                SessionCreationParameters(cwd = cwd, mcpServers = emptyList()),
                operationsFactory,
            )
        }
    }

    @OptIn(com.agentclientprotocol.annotations.UnstableApi::class)
    override suspend fun listSessions(cwd: String?): List<SessionSummary> {
        val activeClient = client ?: error("ACP client is not connected")
        return activeClient
            .listSessions(cwd = cwd?.trim()?.takeIf { it.isNotBlank() })
            .toList()
            .map { info ->
                SessionSummary(
                    sessionId = info.sessionId.value,
                    cwd = info.cwd,
                    title = info.title,
                )
            }
    }

    override fun currentSessionId(): String? = session?.sessionId?.value

    private suspend fun openSession(
        open: suspend (
            activeClient: Client,
            context: AcpEditorContext,
            cwd: String,
            operationsFactory: ClientOperationsFactory,
        ) -> ClientSession,
    ) {
        val activeClient = client ?: error("ACP client is not connected")
        val context = editorContext ?: error("ACP editor context is missing")
        val cwd = launchPlan?.sessionWorkingDirectory ?: error("ACP launch plan is missing")
        val operationsFactory =
            ClientOperationsFactory { _, _ ->
                AcpClientSessionOperationsImpl.create(context)
            }
        try {
            session =
                open(activeClient, context, cwd, operationsFactory)
            sessionReady.complete(Unit)
        } catch (throwable: Throwable) {
            sessionReady.completeExceptionally(throwable)
            throw throwable
        }
    }

    override suspend fun prompt(text: String) {
        awaitOpenSession()
        val activeSession = session ?: error("ACP session is not open")
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return

        promptJob?.cancel()
        promptJob =
            scope.launch {
                runCatching {
                    activeSession.prompt(listOf(ContentBlock.Text(trimmed))).collect { event ->
                        handlePromptEvent(event)
                    }
                }.onFailure { throwable ->
                    logger.warn("ACP prompt failed", throwable)
                    listener.onError(throwable.message ?: throwable.javaClass.simpleName)
                }
            }
        promptJob?.join()
    }

    override suspend fun cancelPrompt() {
        promptJob?.cancel()
        session?.cancel()
    }

    override fun dispose() {
        promptJob?.cancel()
        editorContext?.terminalSessionRegistry?.clear()
        editorContext?.shellPaneHost?.clear()
        runBlocking {
            withContext(Dispatchers.IO) {
                runCatching { session?.cancel() }
            }
        }
        disposeTransportOnly()
        scope.cancel()
    }

    private suspend fun handlePromptEvent(event: Event) {
        when (event) {
            is Event.SessionUpdateEvent -> {
                when (val update = event.update) {
                    is SessionUpdate.AgentMessageChunk -> {
                        TranscriptRenderer.renderEventText(update)?.let(listener::onTranscriptAppend)
                    }
                    else -> {
                        TranscriptRenderer.renderUpdate(update).forEach(listener::onTranscriptLine)
                    }
                }
            }
            is Event.PromptResponseEvent -> Unit
        }
    }

    private fun monitorStderr(process: Process) {
        BufferedReader(InputStreamReader(process.errorStream, StandardCharsets.UTF_8)).use { reader ->
            var line = reader.readLine()
            while (line != null) {
                if (line.isNotBlank()) {
                    listener.onTranscriptLine("[stderr] $line")
                }
                line = reader.readLine()
            }
        }
    }

    private suspend fun awaitOpenSession() {
        if (session != null) return
        try {
            sessionReady.await()
        } catch (_: CancellationException) {
            error("ACP session is not open")
        } catch (throwable: Throwable) {
            val cause = throwable.cause ?: throwable
            error(cause.message ?: "ACP session failed to open")
        }
    }

    private fun disposeTransportOnly() {
        if (!sessionReady.isCompleted) {
            sessionReady.completeExceptionally(CancellationException("ACP session disposed"))
        }
        stderrJob?.cancel()
        exitJob?.cancel()
        runBlocking(Dispatchers.IO) {
            runCatching { protocol?.close() }
        }
        process?.let { activeProcess ->
            if (activeProcess.isAlive) {
                activeProcess.destroy()
                if (!activeProcess.waitFor(PROCESS_DESTROY_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
                    activeProcess.destroyForcibly()
                }
            }
        }
        process = null
        protocol = null
        client = null
        session = null
        launchPlan = null
        editorContext = null
        agentInfo = null
        promptJob = null
        stderrJob = null
        exitJob = null
    }

    companion object {
        private val logger = Logger.getInstance(AcpSessionControllerImpl::class.java)
        private const val PROCESS_DESTROY_TIMEOUT_MS = 3000L
    }
}
