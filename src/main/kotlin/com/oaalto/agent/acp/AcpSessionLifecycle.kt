package com.oaalto.agent.acp

import com.agentclientprotocol.client.Client
import com.agentclientprotocol.client.ClientOperationsFactory
import com.agentclientprotocol.client.ClientSession
import com.agentclientprotocol.common.SessionCreationParameters
import com.agentclientprotocol.model.McpServer
import com.agentclientprotocol.model.SessionId
import com.oaalto.agent.AgentCliLog
import com.oaalto.agent.AgentCliSessionContext
import com.oaalto.agent.settings.LaunchMode
import com.oaalto.agent.worktree.resume.AcpSessionOpenResult
import com.oaalto.agent.worktree.resume.AcpSessionOperations
import com.oaalto.agent.worktree.resume.AcpSessionResumeOrchestrator
import com.oaalto.agent.worktree.resume.LaunchResumePlan
import com.oaalto.agent.worktree.resume.SessionPicker
import com.oaalto.agent.worktree.resume.SessionSummary
import com.oaalto.agent.worktree.resume.WorktreeSessionBinder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.toList

class AcpSessionLifecycle(
    private val sessionOperationsFactory: AcpClientSessionOperationsFactory,
) {
    private var client: Client? = null
    private var session: ClientSession? = null
    private var launchPlan: AcpLaunchPlan? = null
    private var editorContext: AcpEditorContext? = null
    var sessionReady: CompletableDeferred<Unit> = CompletableDeferred()
        private set

    fun prepareForConnect(
        launchPlan: AcpLaunchPlan,
        editorContext: AcpEditorContext,
    ) {
        this.launchPlan = launchPlan
        this.editorContext = editorContext
        sessionReady = CompletableDeferred()
        client = null
        session = null
    }

    fun bindClient(activeClient: Client) {
        client = activeClient
    }

    suspend fun newSession() {
        openSession { activeClient, context, cwd, operationsFactory, mcpServers ->
            activeClient.newSession(
                SessionCreationParameters(cwd = cwd, mcpServers = mcpServers),
                operationsFactory,
            )
        }
    }

    suspend fun loadSession(sessionId: String) {
        val normalizedId = sessionId.trim()
        if (normalizedId.isBlank()) error("ACP session id is blank")
        openSession { activeClient, _, cwd, operationsFactory, mcpServers ->
            activeClient.loadSession(
                SessionId(normalizedId),
                SessionCreationParameters(cwd = cwd, mcpServers = mcpServers),
                operationsFactory,
            )
        }
    }

    suspend fun listSessions(cwd: String?): List<SessionSummary> {
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

    fun currentSessionId(): String? = session?.sessionId?.value

    fun activeSession(): ClientSession? = session

    val sessionWorkingDirectory: String
        get() =
            launchPlan?.sessionWorkingDirectory
                ?: error("ACP session working directory is missing - did you call prepareForConnect?")

    suspend fun startSession(
        resumePlan: LaunchResumePlan,
        picker: SessionPicker,
    ): AcpSessionOpenResult =
        AcpSessionResumeOrchestrator(
            LifecycleSessionOperations(this),
            NoOpWorktreeSessionBinder,
            picker,
        ).openSession(
            plan = resumePlan,
            sessionWorkingDirectory = sessionWorkingDirectory,
            worktreeRecordId = null,
        )

    suspend fun awaitOpenSession() {
        if (session != null) return
        runCatching {
            sessionReady.await()
        }.getOrElse { throwable ->
            when (throwable) {
                is CancellationException -> error("ACP session is not open")
                else -> {
                    val cause = throwable.cause ?: throwable
                    error(cause.message ?: "ACP session failed to open")
                }
            }
        }
    }

    fun failSessionReady(throwable: Throwable) {
        if (!sessionReady.isCompleted) {
            sessionReady.completeExceptionally(throwable)
        }
    }

    fun resetOnDispose() {
        if (!sessionReady.isCompleted) {
            sessionReady.completeExceptionally(CancellationException("ACP session disposed"))
        }
        client = null
        session = null
        launchPlan = null
        editorContext = null
    }

    fun sessionLogContext(sessionId: String? = currentSessionId()): AgentCliSessionContext =
        AgentCliSessionContext(
            configId = editorContext?.configurationId,
            sessionId = sessionId,
            launchMode = LaunchMode.ACP_CLIENT,
            worktreePath = editorContext?.launchContext?.workingDirectoryOverride,
        )

    private suspend fun openSession(
        open: suspend (
            activeClient: Client,
            context: AcpEditorContext,
            cwd: String,
            operationsFactory: ClientOperationsFactory,
            mcpServers: List<McpServer>,
        ) -> ClientSession,
    ) {
        val activeClient = client ?: error("ACP client is not connected")
        val context = editorContext ?: error("ACP editor context is missing")
        val cwd = launchPlan?.sessionWorkingDirectory ?: error("ACP launch plan is missing")
        val activeLaunchPlan = launchPlan ?: error("ACP launch plan is missing")
        val mcpServers = activeLaunchPlan.sessionMcpServers()
        val operationsFactory =
            ClientOperationsFactory { _, _ -> sessionOperationsFactory.create(context) }
        runCatching {
            session = open(activeClient, context, cwd, operationsFactory, mcpServers)
            sessionReady.complete(Unit)
            log.info(
                { "ACP session opened: ${session?.sessionId?.value}" },
                sessionLogContext(session?.sessionId?.value),
            )
        }.onFailure { throwable -> sessionReady.completeExceptionally(throwable) }.getOrThrow()
    }

    companion object {
        private val log = AgentCliLog.getInstance(AcpSessionLifecycle::class.java)
    }
}

private class LifecycleSessionOperations(
    private val lifecycle: AcpSessionLifecycle,
) : AcpSessionOperations {
    override suspend fun newSession() = lifecycle.newSession()

    override suspend fun loadSession(sessionId: String) = lifecycle.loadSession(sessionId)

    override suspend fun listSessions(cwd: String?): List<SessionSummary> = lifecycle.listSessions(cwd)

    override fun currentSessionId(): String? = lifecycle.currentSessionId()
}

private object NoOpWorktreeSessionBinder : WorktreeSessionBinder {
    override fun persistSessionId(
        recordId: String,
        sessionId: String,
    ): Boolean = true
}
