package com.oaalto.agent.acp

import com.agentclientprotocol.common.ClientSessionOperations
import com.agentclientprotocol.model.PermissionOption
import com.agentclientprotocol.model.RequestPermissionResponse
import com.agentclientprotocol.model.SessionUpdate
import com.intellij.openapi.Disposable
import com.oaalto.agent.AgentLaunchContext
import com.oaalto.agent.acp.auth.AuthPromptResult
import com.oaalto.agent.acp.auth.AuthPromptUi
import com.oaalto.agent.acp.permission.PermissionMemoryStore
import com.oaalto.agent.acp.permission.PermissionPromptUi
import com.oaalto.agent.acp.transcript.model.StructuredUpdate
import com.oaalto.agent.acp.transcript.model.TranscriptEventIngestion
import com.oaalto.agent.acp.transcript.view.fakeTranscriptProject
import com.oaalto.agent.acp.transport.InMemoryAcpTransport
import com.oaalto.agent.acp.ui.ShellPaneHost
import com.oaalto.agent.settings.AgentSettingsState
import com.oaalto.agent.worktree.resume.LaunchResumePlan
import com.oaalto.agent.worktree.resume.SessionPicker
import com.oaalto.agent.worktree.resume.SessionSummary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.JsonElement
import java.nio.file.Path

/**
 * Headless harness for [AcpSessionControllerImpl] session-loop integration tests.
 *
 * Drives the controller through [InMemoryAcpTransport] with a [ScriptedAcpAgent]
 * and records listener output. Each `start()` exercises:
 *
 * 1. **connect** — in-memory stdio transport wires client and scripted agent
 * 2. **bootstrap** — `initialize` negotiates capabilities (no auth)
 * 3. **bind** — lifecycle binds the connected client
 * 4. **startSession** — resume orchestrator opens the session (`session/new` for new-session plan, `session/load` for `AcpLoad`)
 */
class AcpSessionLoopTestHarness(
    scriptedSessionId: String = "scripted-session-1",
    loadSessionId: String = scriptedSessionId,
    private val sessionWorkingDirectory: String = ".",
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {
    val listener = RecordingAcpSessionListener()
    private val disposable = Disposable { }

    lateinit var scriptedAgent: ScriptedAcpAgent
        private set

    private val transport =
        InMemoryAcpTransport(
            scope = scope,
            agentConfigurer = { protocol ->
                ScriptedAcpAgent(
                    protocol,
                    newSessionId = scriptedSessionId,
                    loadSessionId = loadSessionId,
                ).also { scriptedAgent = it }
            },
        )

    val controller: AcpSessionController =
        AcpSessionControllerImpl(
            listener = listener,
            scope = scope,
            sessionOperationsFactory = HarnessClientSessionOperationsFactory(listener),
            transport = transport,
        )

    private val launchPlan =
        AcpLaunchPlan(
            command = listOf("scripted-agent"),
            processWorkingDirectory = sessionWorkingDirectory,
            sessionWorkingDirectory = sessionWorkingDirectory,
        )

    private val editorContext = minimalEditorContext(listener, disposable, sessionWorkingDirectory)

    val sessionPicker = HarnessSessionPicker()

    fun newSessionStartRequest(
        resumePlan: LaunchResumePlan = LaunchResumePlan.AcpNewSession,
        worktreeRecordId: String? = null,
    ): AcpSessionStartRequest =
        AcpSessionStartRequest(
            launchPlan = launchPlan,
            editorContext = editorContext,
            resumePlan = resumePlan,
            sessionPicker = sessionPicker,
            worktreeRecordId = worktreeRecordId,
        )

    fun dispose() {
        controller.dispose()
        disposable.dispose()
    }

    private fun minimalEditorContext(
        listener: AcpSessionListener,
        parentDisposable: Disposable,
        workingDirectory: String,
    ): AcpEditorContext {
        val project = fakeTranscriptProject()
        return AcpEditorContext(
            project = project,
            configurationId = "harness-config",
            launchContext = AgentLaunchContext(workingDirectoryOverride = workingDirectory),
            scopeRoot = Path.of(workingDirectory),
            listener = listener,
            shellPaneHost = ShellPaneHost(project, parentDisposable),
            permissionPromptUi = PermissionPromptUi { _, _ -> error("unexpected permission prompt in harness") },
            permissionMemoryStore = PermissionMemoryStore(AgentSettingsState()),
            authPromptUi =
                object : AuthPromptUi {
                    override suspend fun promptApiKey(
                        methodName: String,
                        description: String?,
                    ): AuthPromptResult = AuthPromptResult.Continue

                    override suspend fun promptOAuthLink(
                        methodName: String,
                        description: String?,
                        link: String,
                    ): AuthPromptResult = AuthPromptResult.Continue

                    override suspend fun waitForTerminalAuthCompletion(
                        methodName: String,
                        description: String?,
                    ): AuthPromptResult = AuthPromptResult.Continue
                },
        )
    }
}

class RecordingAcpSessionListener : AcpSessionListener {
    val structuredUpdates = mutableListOf<StructuredUpdate>()
    val errors = mutableListOf<String>()

    override fun onStructuredUpdate(update: StructuredUpdate) {
        structuredUpdates.add(update)
    }

    override fun onError(message: String) {
        errors.add(message)
    }

    override fun onUsageUpdate(usage: AccumulatedUsage) = Unit
}

private class HarnessClientSessionOperationsFactory(
    private val listener: AcpSessionListener,
) : AcpClientSessionOperationsFactory {
    override fun create(context: AcpEditorContext): ClientSessionOperations = HarnessClientSessionOperations(listener)
}

private class HarnessClientSessionOperations(
    private val listener: AcpSessionListener,
) : ClientSessionOperations {
    override suspend fun notify(
        notification: SessionUpdate,
        _meta: JsonElement?,
    ) {
        TranscriptEventIngestion.ingest(notification).forEach(listener::onStructuredUpdate)
    }

    override suspend fun requestPermissions(
        toolCall: SessionUpdate.ToolCallUpdate,
        permissions: List<PermissionOption>,
        _meta: JsonElement?,
    ): RequestPermissionResponse = error("unexpected permission request in session-loop harness")
}

class HarnessSessionPicker(
    private val result: String? = null,
) : SessionPicker {
    var pickCalled = false
        private set

    override suspend fun pickSession(candidates: List<SessionSummary>): String? {
        pickCalled = true
        return result
    }
}
