# Code Context

## Files Retrieved

1. `src/main/kotlin/com/oaalto/agent/acp/TranscriptHtmlAppender.kt` (lines 1-135) — HTML transcript buffer; streaming agent text with inline cursor; committed vs in-flight body state.
2. `src/main/kotlin/com/oaalto/agent/acp/AcpAgentEditor.kt` (lines 1-409) — IntelliJ `FileEditor`; owns `TranscriptHtmlAppender`, `AcpSessionListener`, session lifecycle, prompt input.
3. `src/main/kotlin/com/oaalto/agent/acp/TranscriptStreamingCursor.kt` (lines 1-39) — Pure HTML helpers for streaming cursor entity (`&#9612;`) and finalized blocks.
4. `src/main/kotlin/com/oaalto/agent/acp/AcpPromptEventDispatcher.kt` (lines 1-28) — Routes `SessionUpdate` events to listener; finalizes stream before non-chunk updates.
5. `src/main/kotlin/com/oaalto/agent/acp/AcpEditorLayout.kt` (lines 1-46) — UI layout: transcript column + prompt/shell splitters.
6. `src/main/kotlin/com/oaalto/agent/acp/AcpClientSessionOperationsImpl.kt` (lines 1-230) — ACP `ClientSessionOperations`; `notify()` renders updates to transcript.
7. `src/test/kotlin/com/oaalto/agent/acp/TranscriptHtmlAppenderStreamingTest.kt` (lines 1-175) — Streaming cursor tests **and** `AcpPromptEventDispatcherTest` (no separate test file).
8. `src/test/kotlin/com/oaalto/agent/acp/TranscriptStreamingCursorTest.kt` (lines 1-63) — Cursor HTML entity/strip/hasCursor tests.
9. `CHANGELOG.md` (lines 1-80) — 2026-06-19 streaming cursor work documented.
10. `src/main/kotlin/com/oaalto/agent/acp/AcpSessionListener.kt` (lines 1-17) — Listener contract (referenced by dispatcher/editor).
11. `src/main/kotlin/com/oaalto/agent/acp/AcpSessionControllerImpl.kt` (lines 208-256, 251-256) — Prompt event handling calls dispatcher.

**Not found:** standalone `AcpPromptEventDispatcherTest.kt` — tests live in `TranscriptHtmlAppenderStreamingTest.kt` as inner class `AcpPromptEventDispatcherTest`.

---

## Search Results

### `TranscriptHtmlAppender` usages

| File | Line | Usage |
|------|------|-------|
| `AcpAgentEditor.kt` | 62 | `private val transcript = TranscriptHtmlAppender(transcriptPane)` |
| `TranscriptHtmlAppender.kt` | 15 | class definition |
| `TranscriptHtmlAppenderStreamingTest.kt` | 16, 80, 82 | test class and factory |

### `AcpEditorLayout.buildRootPanel` usages

| File | Line | Usage |
|------|------|-------|
| `AcpEditorLayout.kt` | 18 | `fun buildRootPanel(...)` definition |
| `AcpAgentEditor.kt` | 159 | `AcpEditorLayout.buildRootPanel(transcriptArea = transcriptPane, ...)` in `init` |

### `notify` in `AcpClientSessionOperationsImpl`

| Line | Code |
|------|------|
| 43-48 | `override suspend fun notify(notification: SessionUpdate, _meta: JsonElement?) { TranscriptRenderer.renderUpdate(notification).forEach(listener::onTranscriptHtml) }` |

**Gap:** `notify()` calls `onTranscriptHtml` directly — does **not** call `onFinalizeAgentStream()` first. Contrast with `AcpPromptEventDispatcher`, which finalizes before non-chunk updates.

---

## Full File Contents

### 1. TranscriptHtmlAppender.kt

```kotlin
package com.oaalto.agent.acp

import javax.swing.JEditorPane

/**
 * Manages appending content into a [JEditorPane] in `text/html` mode.
 *
 * Handles the HTML document wrapper, line-break separators between entries,
 * HTML escaping, wrapping plain text in colored spans, and agent-message
 * streaming with an inline cursor at the live edge.
 *
 * [committedBodyHtml] is the source of truth for finalized transcript content;
 * the active stream block is re-rendered from [streamingPlainText] on each chunk.
 */
internal class TranscriptHtmlAppender(
    private val pane: JEditorPane,
) {
    private var htmlBodyInitialized = false
    private var isFirstEntry = true
    private var committedBodyHtml = ""
    private var isAgentStreamActive = false
    private val streamingPlainText = StringBuilder()
    private var streamLineSeparator = ""

    // -- Public append methods -------------------------------------------------

    /**
     * Continues the active agent message stream, or starts a new one when needed.
     * Empty and whitespace-only chunks are ignored.
     */
    fun startOrContinueAgentStream(text: String) {
        if (text.isBlank()) return
        ensureBody()
        if (!isAgentStreamActive) {
            streamLineSeparator = if (isFirstEntry) "" else TranscriptRenderHelpers.HTML_LINE_BREAK
            isFirstEntry = false
            isAgentStreamActive = true
            streamingPlainText.clear()
        }
        streamingPlainText.append(text)
        syncPane()
        pane.caretPosition = pane.document.length
    }

    /**
     * Removes the streaming cursor from the active agent block. No-op when not streaming.
     */
    fun finalizeAgentStream() {
        if (!isAgentStreamActive) return
        val escaped = TranscriptUpdateRenderer.escapeHtml(streamingPlainText.toString())
        committedBodyHtml += TranscriptStreamingCursor.finalizedBlockHtml(escaped, streamLineSeparator)
        isAgentStreamActive = false
        streamingPlainText.clear()
        syncPane()
    }

    fun isAgentStreamActive(): Boolean = isAgentStreamActive

    /**
     * Appends plain [text] that will be HTML-escaped and inserted inline
     * (no `<br>` separator). Used for non-streaming inline append.
     */
    fun appendText(text: String) {
        ensureBody()
        val escaped = TranscriptUpdateRenderer.escapeHtml(text)
        committedBodyHtml += escaped
        syncPane()
        pane.caretPosition = pane.document.length
    }

    /**
     * Appends plain [line] as a new line. The text is HTML-escaped and wrapped
     * in a colored span. A `<br>` is prepended unless this is the first entry.
     */
    fun appendLine(line: String) {
        ensureBody()
        val escaped = TranscriptUpdateRenderer.escapeHtml(line)
        val htmlLine =
            when {
                line.startsWith("> ") -> userSpan(escaped)
                else -> neutralSpan(escaped)
            }
        val separator = if (isFirstEntry) "" else TranscriptRenderHelpers.HTML_LINE_BREAK
        isFirstEntry = false
        committedBodyHtml += separator + htmlLine
        syncPane()
        pane.caretPosition = pane.document.length
    }

    /**
     * Appends a pre-rendered HTML [fragment] as a new line without escaping.
     * A `<br>` is prepended unless this is the first entry.
     */
    fun appendHtml(fragment: String) {
        ensureBody()
        val separator = if (isFirstEntry) "" else TranscriptRenderHelpers.HTML_LINE_BREAK
        isFirstEntry = false
        committedBodyHtml += separator + fragment
        syncPane()
        pane.caretPosition = pane.document.length
    }

    /** Exposes the rendered body HTML for tests in the same module. */
    internal fun displayBodyHtmlForTest(): String = currentBodyHtml()

    // -- Internal helpers ------------------------------------------------------

    private fun ensureBody() {
        if (!htmlBodyInitialized) {
            htmlBodyInitialized = true
            syncPane()
        }
    }

    private fun currentBodyHtml(): String =
        if (isAgentStreamActive) {
            val escaped = TranscriptUpdateRenderer.escapeHtml(streamingPlainText.toString())
            committedBodyHtml + TranscriptStreamingCursor.streamBlockHtml(escaped, streamLineSeparator)
        } else {
            committedBodyHtml
        }

    private fun syncPane() {
        pane.text =
            TranscriptRenderHelpers.htmlDocumentStart() +
            currentBodyHtml() +
            TranscriptRenderHelpers.HTML_DOCUMENT_END
    }

    private fun userSpan(text: String): String =
        "<span style=\"color:#569cd6;font-family:monospace;font-size:12px\">$text</span>"

    private fun neutralSpan(text: String): String =
        "<span style=\"color:#d4d4d4;font-family:monospace;font-size:12px\">$text</span>"
}
```

### 2. AcpAgentEditor.kt

```kotlin
package com.oaalto.agent.acp

import com.intellij.codeHighlighting.BackgroundEditorHighlighter
import com.intellij.ide.structureView.StructureViewBuilder
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorLocation
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import com.oaalto.agent.AgentVirtualFile
import com.oaalto.agent.acp.auth.AuthPromptResult
import com.oaalto.agent.acp.auth.AuthPromptUi
import com.oaalto.agent.acp.filesystem.SessionScopeResolver
import com.oaalto.agent.acp.permission.PermissionPromptUi
import com.oaalto.agent.acp.ui.AuthPromptPanel
import com.oaalto.agent.acp.ui.PermissionPromptPanel
import com.oaalto.agent.acp.ui.PromptInputBar
import com.oaalto.agent.acp.ui.SessionPickerDialog
import com.oaalto.agent.acp.ui.ShellPaneHost
import com.oaalto.agent.settings.AgentSettingsState
import com.oaalto.agent.worktree.AgentWorktreeStateService
import com.oaalto.agent.worktree.resume.LaunchResumePlan
import com.oaalto.agent.worktree.resume.SessionSummary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.awt.BorderLayout
import java.beans.PropertyChangeListener
import java.beans.PropertyChangeSupport
import javax.swing.JComponent
import javax.swing.JEditorPane
import javax.swing.JPanel
import javax.swing.SwingUtilities
import kotlin.coroutines.resume

class AcpAgentEditor(
    private val project: Project,
    private val file: AgentVirtualFile,
    private val sessionControllerFactory: (AcpSessionListener) -> AcpSessionController = ::defaultSessionController,
) : FileEditor,
    Disposable {
    private val propertyChangeSupport = PropertyChangeSupport(this)
    private val userData = UserDataHolderBase()
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val transcriptPane =
        JEditorPane("text/html", "").apply {
            isEditable = false
            background = JBColor.PanelBackground
            border = JBUI.Borders.empty(8)
        }
    private val transcript = TranscriptHtmlAppender(transcriptPane)
    private val permissionPromptPanel = PermissionPromptPanel()
    private val authPromptPanel = AuthPromptPanel()
    private val shellPaneHost = ShellPaneHost(project, this)
    private val promptInputBar =
        PromptInputBar { text ->
            transcript.finalizeAgentStream()
            transcript.appendLine("")
            transcript.appendLine("> $text")
            transcript.appendLine("")
            coroutineScope.launch {
                runCatching { sessionController.prompt(text) }.onFailure { throwable ->
                    logger.warn("Failed to send ACP prompt", throwable)
                    transcript.finalizeAgentStream()
                    transcript.appendHtml(
                        TranscriptRenderHelpers.formatErrorHtml(
                            throwable.message ?: throwable.javaClass.simpleName,
                        ),
                    )
                }
            }
        }
    private val rootPanel = JPanel(BorderLayout())
    private val sessionListener =
        object : AcpSessionListener {
            override fun onTranscriptAppend(text: String) {
                transcript.startOrContinueAgentStream(text)
            }

            override fun onFinalizeAgentStream() {
                transcript.finalizeAgentStream()
            }

            override fun onTranscriptHtml(fragment: String) {
                transcript.finalizeAgentStream()
                transcript.appendHtml(fragment)
            }

            override fun onTranscriptPlainLine(line: String) {
                transcript.finalizeAgentStream()
                transcript.appendLine(line)
            }

            override fun onError(message: String) {
                transcript.finalizeAgentStream()
                transcript.appendHtml(TranscriptRenderHelpers.formatErrorHtml(message))
                runOnEdt { promptInputBar.setEnabled(false) }
            }
        }
    private val permissionPromptUi =
        PermissionPromptUi { title, options ->
            val deferred =
                onEdtAsync {
                    permissionPromptPanel.suspendForPrompt(title, options)
                }
            deferred.await()
        }
    private val authPromptUi =
        object : AuthPromptUi {
            override suspend fun promptApiKey(
                methodName: String,
                description: String?,
            ): AuthPromptResult {
                val deferred =
                    onEdtAsync {
                        authPromptPanel.suspendForApiKey(methodName, description)
                    }
                return deferred.await()
            }

            override suspend fun promptOAuthLink(
                methodName: String,
                description: String?,
                link: String,
            ): AuthPromptResult {
                val deferred =
                    onEdtAsync {
                        authPromptPanel.suspendForOAuth(methodName, description, link)
                    }
                return deferred.await()
            }

            override suspend fun waitForTerminalAuthCompletion(
                methodName: String,
                description: String?,
            ): AuthPromptResult {
                val deferred =
                    onEdtAsync {
                        authPromptPanel.suspendForTerminalAuth(methodName, description)
                    }
                return deferred.await()
            }
        }
    private val sessionController: AcpSessionController = sessionControllerFactory(sessionListener)

    init {
        rootPanel.add(
            AcpEditorLayout.buildRootPanel(
                transcriptArea = transcriptPane,
                permissionPromptPanel = permissionPromptPanel,
                authPromptPanel = authPromptPanel,
                promptInputBar = promptInputBar,
                shellPaneHost = shellPaneHost,
            ),
            BorderLayout.CENTER,
        )
        promptInputBar.setEnabled(false)
        startSession()
    }

    override fun getComponent(): JComponent = rootPanel

    override fun getPreferredFocusedComponent(): JComponent = promptInputBar.component

    override fun getName(): String = "Agent CLI"

    override fun getFile(): VirtualFile = file

    override fun setState(state: FileEditorState) = Unit

    override fun isModified(): Boolean = false

    override fun isValid(): Boolean = file.isValid && !project.isDisposed

    override fun selectNotify() = Unit

    override fun deselectNotify() = Unit

    override fun addPropertyChangeListener(listener: PropertyChangeListener) {
        propertyChangeSupport.addPropertyChangeListener(listener)
    }

    override fun removePropertyChangeListener(listener: PropertyChangeListener) {
        propertyChangeSupport.removePropertyChangeListener(listener)
    }

    override fun getBackgroundHighlighter(): BackgroundEditorHighlighter? = null

    override fun getCurrentLocation(): FileEditorLocation? = null

    override fun getStructureViewBuilder(): StructureViewBuilder? = null

    override fun <T : Any?> getUserData(key: Key<T>): T? = userData.getUserData(key)

    override fun <T : Any?> putUserData(
        key: Key<T>,
        value: T?,
    ) {
        userData.putUserData(key, value)
    }

    override fun dispose() {
        permissionPromptPanel.cancelPending()
        authPromptPanel.cancelPending()
        coroutineScope.launch {
            runCatching { sessionController.cancelPrompt() }
        }
        sessionController.dispose()
        coroutineScope.cancel()
    }

    private fun startSession() {
        val configuration = AgentSettingsState.getInstance().getConfigurationById(file.configurationId)
        if (configuration == null) {
            transcript.appendHtml(
                TranscriptRenderHelpers.formatErrorHtml("Agent configuration was removed or is unavailable."),
            )
            promptInputBar.setEnabled(false)
            return
        }

        coroutineScope.launch {
            launchAndConnect(configuration)
        }
    }

    private suspend fun launchAndConnect(configuration: Any?) {
        @Suppress("UNCHECKED_CAST")
        val typedConfig = configuration as AgentSettingsState.AgentCliConfiguration
        val launchPlan =
            AcpProcessLauncher
                .buildLaunchPlan(
                    projectContext = project.toAgentProjectContext(),
                    configuration = typedConfig,
                    launchContext = file.launchContext,
                ).getOrElse { throwable ->
                    transcript.appendHtml(
                        TranscriptRenderHelpers.formatErrorHtml(
                            throwable.message ?: "Failed to build launch command.",
                        ),
                    )
                    runOnEdt { promptInputBar.setEnabled(false) }
                    return
                }

        transcript.appendLine("Connecting to ${typedConfig.name}...")
        runCatching {
            val scopeRoot =
                SessionScopeResolver.hostScopeRoot(
                    sessionWorkingDirectory = launchPlan.sessionWorkingDirectory,
                    projectBasePath = project.basePath,
                    workingDirectoryOverride = file.launchContext.workingDirectoryOverride,
                )
            val editorContext =
                AcpEditorContext(
                    project = project,
                    configurationId = file.configurationId,
                    launchContext = file.launchContext,
                    scopeRoot = scopeRoot,
                    listener = sessionListener,
                    shellPaneHost = shellPaneHost,
                    permissionPromptUi = permissionPromptUi,
                    authPromptUi = authPromptUi,
                )
            sessionController.connect(launchPlan, editorContext)
            openSessionFromResumePlan(launchPlan.sessionWorkingDirectory)
            transcript.appendLine("Connected to ${typedConfig.name}.")
            runOnEdt {
                promptInputBar.setEnabled(true)
                promptInputBar.requestFocus()
            }
        }.onFailure { throwable ->
            logger.warn("Failed to start ACP session", throwable)
            transcript.appendHtml(
                TranscriptRenderHelpers.formatErrorHtml(
                    throwable.message ?: throwable.javaClass.simpleName,
                ),
            )
            runOnEdt { promptInputBar.setEnabled(false) }
        }
    }

    private suspend fun openSessionFromResumePlan(sessionWorkingDirectory: String) {
        when (val plan = file.launchContext.resumePlan) {
            is LaunchResumePlan.AcpLoad -> {
                runCatching {
                    sessionController.loadSession(plan.sessionId)
                    persistBoundSessionId(plan.sessionId)
                }.onFailure { throwable ->
                    logger.warn("ACP session load failed for ${plan.sessionId}", throwable)
                    transcript.appendLine(
                        "Stored session is unavailable (${throwable.message ?: "load failed"}). " +
                            "Choose a session to resume or start fresh.",
                    )
                    pickSessionOrStartFresh(sessionWorkingDirectory)
                }
            }
            is LaunchResumePlan.AcpPickSession -> {
                if (plan.candidates.isNotEmpty()) {
                    pickSessionFromCandidates(plan.candidates)
                } else {
                    transcript.appendLine(
                        "No stored ACP session for this worktree. Choose a session to resume or start fresh.",
                    )
                    pickSessionOrStartFresh(sessionWorkingDirectory)
                }
            }
            LaunchResumePlan.AcpNewSession, null -> {
                sessionController.newSession()
                persistCurrentSessionId()
            }
            is LaunchResumePlan.Pty -> error("PTY resume plan is not valid for ACP editor")
        }
    }

    private suspend fun pickSessionOrStartFresh(sessionWorkingDirectory: String) {
        val sessions =
            runCatching {
                sessionController.listSessions(sessionWorkingDirectory)
            }.getOrElse { throwable ->
                logger.warn("ACP listSessions failed", throwable)
                transcript.appendHtml(
                    TranscriptRenderHelpers.formatErrorHtml(
                        throwable.message ?: "Failed to list sessions",
                    ),
                )
                emptyList()
            }
        pickSessionFromCandidates(sessions)
    }

    private suspend fun pickSessionFromCandidates(candidates: List<SessionSummary>) {
        if (candidates.isEmpty()) {
            sessionController.newSession()
            persistCurrentSessionId()
            transcript.appendLine("Started a new ACP session.")
            return
        }
        val selectedId =
            onEdtAsync {
                SessionPickerDialog.show(project, candidates)
            }
        if (selectedId == null) {
            sessionController.newSession()
            persistCurrentSessionId()
            transcript.appendLine("Started a new ACP session.")
            return
        }
        runCatching {
            sessionController.loadSession(selectedId)
            persistBoundSessionId(selectedId)
            transcript.appendLine("Resumed session $selectedId.")
        }.onFailure { throwable ->
            logger.warn("ACP session load failed for picker selection $selectedId", throwable)
            transcript.appendHtml(
                TranscriptRenderHelpers.formatErrorHtml(
                    throwable.message ?: "Failed to load selected session",
                ),
            )
            sessionController.newSession()
            persistCurrentSessionId()
        }
    }

    private fun persistCurrentSessionId() {
        val sessionId = sessionController.currentSessionId() ?: return
        persistBoundSessionId(sessionId)
    }

    private fun persistBoundSessionId(sessionId: String) {
        val recordId = file.launchContext.worktreeId ?: return
        AgentWorktreeStateService.getInstance().setAcpSessionId(recordId, sessionId)
    }

    private fun runOnEdt(action: () -> Unit) {
        if (SwingUtilities.isEventDispatchThread()) {
            action()
        } else {
            ApplicationManager.getApplication().invokeLater(action, ModalityState.any())
        }
    }

    private suspend fun <T> onEdtAsync(block: () -> T): T =
        suspendCancellableCoroutine { continuation ->
            ApplicationManager.getApplication().invokeLater({
                if (continuation.isActive) {
                    continuation.resume(block())
                }
            }, ModalityState.any())
        }

    companion object {
        private val logger = Logger.getInstance(AcpAgentEditor::class.java)

        private fun defaultSessionController(listener: AcpSessionListener): AcpSessionController =
            AcpSessionControllerImpl(listener)
    }
}
```

### 3. TranscriptStreamingCursor.kt

```kotlin
package com.oaalto.agent.acp

/**
 * Pure HTML operations for the in-progress agent message streaming cursor.
 *
 * Chunk text passed into these helpers must already be HTML-escaped.
 * Avoid custom element attributes here — Swing's HTML editor drops unknown attrs.
 *
 * The cursor is rendered as an HTML entity (`&#9612;`) rather than a literal
 * unicode character so that JEditorPane does not strip it during round-trips.
 */
internal object TranscriptStreamingCursor {
    /** HTML entity for U+258A (▊) — survives JEditorPane HTML round-trip. */
    const val CURSOR_HTML: String = "&#9612;"

    private const val AGENT_SPAN_OPEN: String =
        "<span style=\"color:#d4d4d4;font-family:monospace;font-size:12px\">"

    /** Builds an agent stream block with cursor at the live edge. */
    fun streamBlockHtml(
        escapedText: String,
        lineSeparator: String,
    ): String = lineSeparator + AGENT_SPAN_OPEN + escapedText + CURSOR_HTML + "</span>"

    /** Builds a finalized agent block without the cursor. */
    fun finalizedBlockHtml(
        escapedText: String,
        lineSeparator: String,
    ): String = lineSeparator + AGENT_SPAN_OPEN + escapedText + "</span>"

    /** Strips the cursor entity from [html], leaving agent text intact. */
    fun stripCursor(html: String): String = html.replace(CURSOR_HTML, "")

    /** Detects cursor in any form: decimal entity, hex entity, or literal character. */
    fun hasCursor(html: String): Boolean =
        html.contains(CURSOR_HTML) ||
            html.contains("&#x258a;", ignoreCase = true) ||
            html.contains("\u258a")
}
```

### 4. AcpPromptEventDispatcher.kt

```kotlin
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
```

### 5. AcpEditorLayout.kt

```kotlin
package com.oaalto.agent.acp

import com.intellij.openapi.ui.Splitter
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import com.oaalto.agent.acp.ui.AuthPromptPanel
import com.oaalto.agent.acp.ui.PermissionPromptPanel
import com.oaalto.agent.acp.ui.PromptInputBar
import com.oaalto.agent.acp.ui.ShellPaneHost
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel

internal object AcpEditorLayout {
    const val PROMPT_SPLIT_RATIO = 0.2f
    const val TRANSCRIPT_SPLIT_RATIO = 0.72f

    fun buildRootPanel(
        transcriptArea: JComponent,
        permissionPromptPanel: PermissionPromptPanel,
        authPromptPanel: AuthPromptPanel,
        promptInputBar: PromptInputBar,
        shellPaneHost: ShellPaneHost,
    ): JPanel {
        val transcriptColumn =
            JPanel(BorderLayout()).apply {
                add(JBScrollPane(transcriptArea), BorderLayout.CENTER)
                add(permissionPromptPanel.component, BorderLayout.SOUTH)
                add(authPromptPanel.component, BorderLayout.NORTH)
            }
        val bottomSplitter =
            Splitter(true, PROMPT_SPLIT_RATIO).apply {
                firstComponent = promptInputBar.component
                secondComponent = shellPaneHost.component
            }
        val mainSplitter =
            Splitter(true, TRANSCRIPT_SPLIT_RATIO).apply {
                firstComponent = transcriptColumn
                secondComponent = bottomSplitter
            }
        return JPanel(BorderLayout()).apply {
            add(mainSplitter, BorderLayout.CENTER)
            border = JBUI.Borders.empty()
        }
    }
}
```

### 6. AcpClientSessionOperationsImpl.kt

```kotlin
package com.oaalto.agent.acp

import com.agentclientprotocol.common.ClientSessionOperations
import com.agentclientprotocol.model.CreateTerminalResponse
import com.agentclientprotocol.model.EnvVariable
import com.agentclientprotocol.model.KillTerminalCommandResponse
import com.agentclientprotocol.model.PermissionOption
import com.agentclientprotocol.model.ReadTextFileResponse
import com.agentclientprotocol.model.ReleaseTerminalResponse
import com.agentclientprotocol.model.RequestPermissionResponse
import com.agentclientprotocol.model.SessionUpdate
import com.agentclientprotocol.model.TerminalExitStatus
import com.agentclientprotocol.model.TerminalOutputResponse
import com.agentclientprotocol.model.WaitForTerminalExitResponse
import com.agentclientprotocol.model.WriteTextFileResponse
import com.agentclientprotocol.protocol.JsonRpcException
import com.agentclientprotocol.rpc.JsonRpcErrorCode
import com.oaalto.agent.acp.filesystem.IdeScopedFileSystemAccess
import com.oaalto.agent.acp.filesystem.ScopedFileSystemOperations
import com.oaalto.agent.acp.permission.PermissionCoordinator
import com.oaalto.agent.acp.terminal.TerminalSessionRegistry
import com.oaalto.agent.acp.ui.ShellPaneHost
import kotlinx.serialization.json.JsonElement
import org.jetbrains.plugins.terminal.ShellTerminalWidget
import org.jetbrains.plugins.terminal.TerminalUtil

class AcpClientSessionOperationsImpl(
    private val editorContext: AcpEditorContext,
    private val scopedFileSystem: ScopedFileSystemOperations,
    private val fileSystemAccess: IdeScopedFileSystemAccess,
    private val permissionCoordinator: PermissionCoordinator,
    private val terminalSessionRegistry: TerminalSessionRegistry,
) : ClientSessionOperations {
    private val listener: AcpSessionListener
        get() = editorContext.listener

    override suspend fun requestPermissions(
        toolCall: SessionUpdate.ToolCallUpdate,
        permissions: List<PermissionOption>,
        _meta: JsonElement?,
    ): RequestPermissionResponse = permissionCoordinator.requestSessionPermission(toolCall, permissions)

    override suspend fun notify(
        notification: SessionUpdate,
        _meta: JsonElement?,
    ) {
        TranscriptRenderer.renderUpdate(notification).forEach(listener::onTranscriptHtml)
    }

    override suspend fun fsReadTextFile(
        path: String,
        line: UInt?,
        limit: UInt?,
        _meta: JsonElement?,
    ): ReadTextFileResponse {
        when (val scope = scopedFileSystem.resolveForRead(path)) {
            is ScopedFileSystemOperations.ScopeResult.OutOfScope ->
                throw fsError(scope.message)
            is ScopedFileSystemOperations.ScopeResult.InScope -> {
                val readResult = fileSystemAccess.readText(scope.resolved)
                return when (readResult) {
                    is IdeScopedFileSystemAccess.AccessResult.Failure ->
                        throw fsError(readResult.message)
                    is IdeScopedFileSystemAccess.AccessResult.Success -> {
                        val content = sliceLines(readResult.content, line, limit)
                        ReadTextFileResponse(content = content)
                    }
                }
            }
        }
    }

    override suspend fun fsWriteTextFile(
        path: String,
        content: String,
        _meta: JsonElement?,
    ): WriteTextFileResponse {
        when (val scope = scopedFileSystem.resolveForWrite(path)) {
            is ScopedFileSystemOperations.ScopeResult.OutOfScope ->
                throw fsError(scope.message)
            is ScopedFileSystemOperations.ScopeResult.InScope -> {
                fileSystemAccess.isBlockedForWrite(scope.resolved)?.let { message ->
                    throw fsError(message)
                }
                if (!permissionCoordinator.requestWritePermission(path)) {
                    throw fsError("Write permission denied for $path")
                }
                return when (val writeResult = fileSystemAccess.writeText(scope.resolved, content)) {
                    is IdeScopedFileSystemAccess.AccessResult.Failure ->
                        throw fsError(writeResult.message)
                    is IdeScopedFileSystemAccess.AccessResult.Success ->
                        WriteTextFileResponse()
                }
            }
        }
    }

    override suspend fun terminalCreate(
        command: String,
        args: List<String>,
        cwd: String?,
        env: List<EnvVariable>,
        outputByteLimit: ULong?,
        _meta: JsonElement?,
    ): CreateTerminalResponse {
        val envPairs = env.map { it.name to it.value }
        val widget =
            editorContext.shellPaneHost.startCommand(
                command = command,
                args = args,
                cwd = cwd,
                env = envPairs,
            )
        val session = terminalSessionRegistry.register(widget)
        listener.onTranscriptPlainLine(
            TranscriptRenderer.formatTerminalCreate(
                ShellPaneHost.formatCommandLabel(command, args),
                session.terminalId,
            ),
        )
        return CreateTerminalResponse(terminalId = session.terminalId)
    }

    override suspend fun terminalOutput(
        terminalId: String,
        _meta: JsonElement?,
    ): TerminalOutputResponse {
        val session =
            terminalSessionRegistry.get(terminalId)
                ?: throw fsError("Unknown terminal: $terminalId")
        val fullText = readTerminalText(session.widget)
        val cursor = session.outputCursor.coerceAtMost(fullText.length)
        val delta = fullText.substring(cursor)
        session.outputCursor = fullText.length
        val exitStatus = terminalExitStatus(session.widget)
        return TerminalOutputResponse(
            output = delta,
            truncated = false,
            exitStatus = exitStatus,
        )
    }

    override suspend fun terminalRelease(
        terminalId: String,
        _meta: JsonElement?,
    ): ReleaseTerminalResponse {
        terminalSessionRegistry.release(terminalId)
        return ReleaseTerminalResponse()
    }

    override suspend fun terminalWaitForExit(
        terminalId: String,
        _meta: JsonElement?,
    ): WaitForTerminalExitResponse {
        val session =
            terminalSessionRegistry.get(terminalId)
                ?: throw fsError("Unknown terminal: $terminalId")
        val exitStatus = awaitTerminalExit(session.widget)
        return WaitForTerminalExitResponse(
            exitCode = exitStatus?.exitCode,
            signal = exitStatus?.signal,
        )
    }

    override suspend fun terminalKill(
        terminalId: String,
        _meta: JsonElement?,
    ): KillTerminalCommandResponse {
        val session =
            terminalSessionRegistry.get(terminalId)
                ?: throw fsError("Unknown terminal: $terminalId")
        runCatching { session.widget.getProcessTtyConnector()?.close() }
        return KillTerminalCommandResponse()
    }

    private fun readTerminalText(widget: ShellTerminalWidget): String = runCatching { widget.text }.getOrDefault("")

    private fun terminalExitStatus(widget: ShellTerminalWidget): TerminalExitStatus? {
        if (TerminalUtil.hasRunningCommands(widget.ttyConnector)) return null
        return awaitTerminalExit(widget)
    }

    private fun awaitTerminalExit(widget: ShellTerminalWidget): TerminalExitStatus? {
        val connector = widget.processTtyConnector ?: return null
        return runCatching {
            val exitCode = connector.waitFor()
            TerminalExitStatus(exitCode = exitCode.toUInt())
        }.getOrNull()
    }

    private fun sliceLines(
        content: String,
        line: UInt?,
        limit: UInt?,
    ): String {
        if (line == null && limit == null) return content
        val lines = content.lines()
        val start = line?.toInt() ?: 0
        val end =
            if (limit != null) {
                (start + limit.toInt()).coerceAtMost(lines.size)
            } else {
                lines.size
            }
        return lines.drop(start).take(end - start).joinToString("\n")
    }

    private fun fsError(message: String): JsonRpcException =
        JsonRpcException(
            code = JsonRpcErrorCode.RESOURCE_NOT_FOUND.code,
            message = message,
        )

    companion object {
        fun create(editorContext: AcpEditorContext): AcpClientSessionOperationsImpl {
            val scoped =
                ScopedFileSystemOperations.create(
                    scopeRoot = editorContext.scopeRoot,
                    projectBasePath = editorContext.project.basePath,
                )
            return AcpClientSessionOperationsImpl(
                editorContext = editorContext,
                scopedFileSystem = scoped,
                fileSystemAccess = IdeScopedFileSystemAccess(editorContext.project),
                permissionCoordinator = editorContext.permissionCoordinator(),
                terminalSessionRegistry = editorContext.terminalSessionRegistry,
            )
        }
    }
}
```

### 7. TranscriptHtmlAppenderStreamingTest.kt (includes AcpPromptEventDispatcherTest)

```kotlin
@file:Suppress("OPT_IN_USAGE")

package com.oaalto.agent.acp

import com.agentclientprotocol.model.ContentBlock
import com.agentclientprotocol.model.SessionUpdate
import com.agentclientprotocol.model.ToolCallId
import com.agentclientprotocol.model.ToolCallStatus
import com.agentclientprotocol.model.ToolKind
import javax.swing.JEditorPane
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TranscriptHtmlAppenderStreamingTest {
    @Test
    fun `streaming chunks show cursor until finalize`() {
        val (appender, pane) = appender()

        appender.startOrContinueAgentStream("Hel")
        appender.startOrContinueAgentStream("lo")

        assertTrue(TranscriptStreamingCursor.hasCursor(appender.displayBodyHtmlForTest()))
        assertTrue(appender.displayBodyHtmlForTest().contains("Hello"))

        appender.finalizeAgentStream()

        assertFalse(TranscriptStreamingCursor.hasCursor(appender.displayBodyHtmlForTest()))
        assertTrue(appender.displayBodyHtmlForTest().contains("Hello"))
        assertFalse(appender.isAgentStreamActive())
    }

    @Test
    fun `empty and whitespace chunks do not start stream`() {
        val appender = appender().first

        appender.startOrContinueAgentStream("")
        appender.startOrContinueAgentStream("   ")

        assertFalse(appender.isAgentStreamActive())
        assertFalse(TranscriptStreamingCursor.hasCursor(appender.displayBodyHtmlForTest()))
    }

    @Test
    fun `second stream cycle after finalize gets a new cursor`() {
        val appender = appender().first

        appender.startOrContinueAgentStream("first")
        appender.finalizeAgentStream()
        appender.startOrContinueAgentStream("second")

        assertTrue(TranscriptStreamingCursor.hasCursor(appender.displayBodyHtmlForTest()))
        assertTrue(appender.displayBodyHtmlForTest().contains("first"))
        assertTrue(appender.displayBodyHtmlForTest().contains("second"))
    }

    @Test
    fun `html special characters are escaped in streamed chunks`() {
        val appender = appender().first

        appender.startOrContinueAgentStream("<tag> & more")

        assertTrue(appender.displayBodyHtmlForTest().contains("&lt;tag&gt; &amp; more"))
        assertFalse(appender.displayBodyHtmlForTest().contains("<tag> & more"))
    }

    @Test
    fun `double finalize is safe`() {
        val appender = appender().first

        appender.startOrContinueAgentStream("text")
        appender.finalizeAgentStream()
        val afterFirst = appender.displayBodyHtmlForTest()
        appender.finalizeAgentStream()

        assertEquals(afterFirst, appender.displayBodyHtmlForTest())
    }

    private fun appender(): Pair<TranscriptHtmlAppender, JEditorPane> {
        val pane = JEditorPane("text/html", "")
        return TranscriptHtmlAppender(pane) to pane
    }
}

class AcpPromptEventDispatcherTest {
    @Test
    fun `agent chunks append without finalize`() {
        val listener = RecordingListener()

        AcpPromptEventDispatcher.dispatchSessionUpdate(
            SessionUpdate.AgentMessageChunk(content = ContentBlock.Text("Hi")),
            listener,
        )

        assertEquals(listOf("Hi"), listener.appendedChunks)
        assertEquals(0, listener.finalizeCount)
    }

    @Test
    fun `tool call finalizes before html append`() {
        val listener = RecordingListener()
        listener.streamingActive = true

        AcpPromptEventDispatcher.dispatchSessionUpdate(
            SessionUpdate.ToolCall(
                toolCallId = ToolCallId("1"),
                title = "read file",
                kind = ToolKind.READ,
                status = ToolCallStatus.IN_PROGRESS,
            ),
            listener,
        )

        assertEquals(1, listener.finalizeCount)
        assertTrue(listener.htmlFragments.isNotEmpty())
    }

    @Test
    fun `prompt completion finalizes stream`() {
        val listener = RecordingListener()

        AcpPromptEventDispatcher.dispatchPromptCompleted(listener)

        assertEquals(1, listener.finalizeCount)
    }

    @Test
    fun `chunk then tool sequence leaves no cursor in listener contract`() {
        val listener = RecordingListener()

        AcpPromptEventDispatcher.dispatchSessionUpdate(
            SessionUpdate.AgentMessageChunk(content = ContentBlock.Text("Thinking")),
            listener,
        )
        AcpPromptEventDispatcher.dispatchSessionUpdate(
            SessionUpdate.ToolCall(
                toolCallId = ToolCallId("1"),
                title = "read README.md",
                kind = ToolKind.READ,
                status = ToolCallStatus.IN_PROGRESS,
            ),
            listener,
        )
        AcpPromptEventDispatcher.dispatchPromptCompleted(listener)

        assertEquals(listOf("Thinking"), listener.appendedChunks)
        assertEquals(2, listener.finalizeCount)
        assertTrue(listener.htmlFragments.any { it.contains("read README.md") })
    }

    private class RecordingListener : AcpSessionListener {
        val appendedChunks = mutableListOf<String>()
        val htmlFragments = mutableListOf<String>()
        var finalizeCount = 0
        var streamingActive = false

        override fun onTranscriptAppend(text: String) {
            appendedChunks += text
        }

        override fun onFinalizeAgentStream() {
            finalizeCount++
            streamingActive = false
        }

        override fun onTranscriptHtml(fragment: String) {
            htmlFragments += fragment
        }

        override fun onTranscriptPlainLine(line: String) = Unit

        override fun onError(message: String) = Unit
    }
}
```

### 8. TranscriptStreamingCursorTest.kt

```kotlin
package com.oaalto.agent.acp

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TranscriptStreamingCursorTest {
    @Test
    fun `stream block includes cursor entity and agent styling`() {
        val block = TranscriptStreamingCursor.streamBlockHtml("Hello", "")

        assertTrue(block.contains("Hello"))
        assertTrue(block.contains(TranscriptStreamingCursor.CURSOR_HTML))
        assertTrue(TranscriptStreamingCursor.hasCursor(block))
        assertTrue(block.contains("color:#d4d4d4"))
        assertTrue(block.contains("monospace"))
    }

    @Test
    fun `finalized block removes cursor`() {
        val block = TranscriptStreamingCursor.finalizedBlockHtml("Done", "")

        assertTrue(block.contains("Done"))
        assertFalse(TranscriptStreamingCursor.hasCursor(block))
    }

    @Test
    fun `stripCursor removes entity and preserves text`() {
        val block = TranscriptStreamingCursor.streamBlockHtml("Hello", "")
        val stripped = TranscriptStreamingCursor.stripCursor(block)

        assertTrue(stripped.contains("Hello"))
        assertFalse(TranscriptStreamingCursor.hasCursor(stripped))
    }

    @Test
    fun `escaped special characters remain safe in stream block`() {
        val escaped = TranscriptUpdateRenderer.escapeHtml("<a & b>")
        val block = TranscriptStreamingCursor.streamBlockHtml(escaped, "")

        assertTrue(block.contains("&lt;a &amp; b&gt;"))
        assertFalse(block.contains("<a & b>"))
    }

    @Test
    fun `hasCursor recognizes hex entity form`() {
        assertTrue(TranscriptStreamingCursor.hasCursor("Hello&#x258a;"))
    }

    @Test
    fun `hasCursor recognizes literal unicode character`() {
        assertTrue(TranscriptStreamingCursor.hasCursor("Hello\u258a"))
    }

    @Test
    fun `double stripCursor is idempotent`() {
        val block = TranscriptStreamingCursor.streamBlockHtml("x", "")
        val once = TranscriptStreamingCursor.stripCursor(block)
        val twice = TranscriptStreamingCursor.stripCursor(once)
        assertEquals(once, twice)
    }
}
```

### 9. CHANGELOG.md (first 80 lines)

```markdown
# Changelog

## 2026-06-19

### Added

- **ACP transcript streaming cursor PRD** (`docs/acp-output-rendering-roadmap.md`): Roadmap Step 3 PRD and `ready-for-agent` issue for inline streaming cursor on agent text — finalize before tool/thought/status interrupts and on prompt completion. made by: Olli Aalto. made with: Cursor. model: Composer

- **ACP transcript tool status badges PRD** (`docs/prd/acp-transcript-tool-status-badges.md`, `docs/issues/acp-transcript-step2-tool-status-badges.md`): PRD and issue for Step 2 of output rendering — badge-first tool call lines with status-colored badges and ✓/✗ iconography, replacing Step 1's bracketed `(status)` format. made by: Olli Aalto. made with: Cursor. model: Composer

- **ACP transcript HTML rendering PRD** (`docs/prd/acp-transcript-html-rendering.md`, `docs/issues/acp-transcript-step1-html-rendering.md`): PRD and issue for Step 1 of output rendering — replace `JBTextArea` with HTML `JEditorPane` for color-coded source differentiation. made by: Olli Aalto. made with: Claude. model: claude-sonnet-4-20250514

### Changed

- **ACP transcript streaming cursor** (`acp/TranscriptStreamingCursor.kt`, `acp/TranscriptHtmlAppender.kt`, `acp/AcpPromptEventDispatcher.kt`, `acp/AcpSessionControllerImpl.kt`, `acp/AcpAgentEditor.kt`, `acp/AcpSessionListener.kt`, tests): Inline `▊` cursor on streaming agent text; finalize before tool/thought/status interrupts and on prompt completion; HTML-escaped chunks with second-burst stream cycles. made by: Olli Aalto. made with: Cursor. model: Composer

### Fixed

- **ACP streaming cursor JEditorPane rendering** (`acp/TranscriptStreamingCursor.kt`): Replace literal unicode `\u258a` with HTML entity `&#9612;` so JEditorPane does not strip the cursor character during HTML round-trips; add `stripCursor` helper; update `hasCursor` to detect all entity forms. made by: Olli Aalto. made with: Cursor. model: Composer

- **ACP transcript tool status badge review fixes** (`acp/TranscriptRenderHelpers.kt`, `acp/TranscriptRenderer.kt`, `acp/TranscriptRendererTest.kt`, `docs/`): Structural legacy-format assertions on all tool-call tests; UTF-8 charset in `htmlDocumentStart` with icon round-trip test; `formatToolStatus` aligned to badge-first plain text; `ToolCallUpdate` null-title integration test; PRD marked implemented and roadmap current state refreshed. made by: Olli Aalto. made with: Cursor. model: Composer

- **ACP transcript tool status badges** (`acp/TranscriptRenderHelpers.kt`, `acp/TranscriptRendererTest.kt`): Replace Step 1 bracketed `[kind] title (status)` tool lines with badge-first HTML — colored status badge with ✓/✗ icons for completed/failed, muted title after the badge, no parenthetical status text. made by: Olli Aalto. made with: Cursor. model: Composer

- **ACP transcript HTML rendering** (`acp/TranscriptUpdateRenderer.kt`, `acp/TranscriptRenderer.kt`, `acp/AcpAgentEditor.kt`): Replace `JBTextArea` with `JEditorPane` (text/html); `TranscriptUpdateRenderer` produces color-coded HTML `<span>` fragments; `TranscriptRenderer` adds HTML helpers (`formatToolStatusHtml`, `formatErrorHtml`, `htmlDocumentStart`); `escapeHtml` prevents injection; extract `launchAndConnect` to keep `LongMethod` under threshold; add `thresholdInObjects`/`thresholdInClasses` to detekt config for new function count. made by: Olli Aalto. made with: pi (worker). model: 

- **ACP transcript HTML rendering tests** (`acp/TranscriptRendererTest.kt`): Add HTML fragment output tests for all `SessionUpdate` subtypes, HTML escaping, and HTML helper methods; update chronological order test for HTML output. made by: Olli Aalto. made with: pi (worker). model:

- **ACP roadmap status** (`docs/acp-output-rendering-roadmap.md`): Add status header linking to Step 1 PRD. made by: Olli Aalto. made with: Claude. model: claude-sonnet-4-20250514

- **ACP transcript HTML helper extraction** (`acp/TranscriptHtmlAppender.kt`, `acp/TranscriptRenderHelpers.kt`, `acp/TranscriptRenderer.kt`, `acp/AcpAgentEditor.kt`): Extract HTML append logic from `AcpAgentEditor` into `TranscriptHtmlAppender` and HTML formatting helpers from `TranscriptRenderer` into `TranscriptRenderHelpers`; restore original detekt `thresholdInClasses` (15) and use `thresholdInObjects: 12` instead of the previous `16`/`17` bumps. made by: Olli Aalto. made with: pi (worker). model:

- **ACP transcript line/stream separation and user echo** (`acp/AcpAgentEditor.kt`): Show "Connecting..." message before `sessionController.connect()`; make `appendTranscriptLine` append a trailing newline so subsequent streaming chunks don't merge onto the same line; insert a blank line before echoing the user prompt as `&gt; ` so the input is visually separated in the output. made by: Olli Aalto.

## 2026-06-17

### Fixed

- **ACP auth and transcript rendering** (`acp/auth/`, `acp/ui/AuthPromptPanel.kt`, `acp/TranscriptRenderer.kt`, `acp/AcpAgentEditor.kt`): Try silent `authenticate` before showing auth UI so already-logged-in Cursor sessions skip prompts; route credential-less Agent Auth to Shell-pane confirmation instead of an API-key field; render auth messages in a multiline read-only area; normalize transcript line endings and `<br>` tags. made by: Olli Aalto. made with: Cursor. model: Composer

- **ACP environment variable settings** (`settings/AgentSettingsConfigurable.kt`): Replace the `KEY=VALUE` textarea with a Name/Value table so each variable has a dedicated edit cell. made by: Olli Aalto. made with: Cursor. model: Composer

### Changed

- **Detekt step 5 thresholds** (`detekt.yml`, `acp/TranscriptRenderer.kt`, `settings/`): Tighten `ReturnCount` (max 3, guard-clause exclusion), `CyclomaticComplexMethod` (10), `CognitiveComplexMethod` (12), and `TooManyFunctions` (15); extract transcript/settings helpers to satisfy new limits. made by: Olli Aalto. made with: Cursor. model: Composer

- **Detekt strict compliance** (`detekt.yml`, `src/main/kotlin/`): Complete detekt burn-down for defaults — extract worktree/settings/MCP helpers, shared WSL path resolvers, `AgentWslCommandRequest`, UI metric constants, and `ignoreOverridden` for IntelliJ interface methods; `./gradlew qualityGate` passes clean. made by: Olli Aalto. made with: Cursor. model: Composer

- **ReturnCount compliance** (`src/main/kotlin/`): Refactor multi-return functions to `when` expressions, `Result.flatMap` chains, and shared `WslPathResolver` / `WorkingDirectoryResolver` helpers so detekt `ReturnCount` passes without changing launch behavior. made by: Olli Aalto. made with: Cursor. model: Composer

- **Detekt defaults** (`detekt.yml`): Drop relaxed overrides (long methods, wide parameter lists, disabled exception/style rules) and rely on detekt defaults; keep IntelliJ-friendly `MagicNumber` ignores and disable `MaxLineLength` (ktlint owns line length). made by: Olli Aalto. made with: Cursor. model: Composer

- **Per-project agent selection** (`settings/`, `SelectAgentConfigurationActionGroup.kt`, `RunAgentSplitButtonAction.kt`, `OpenAgentEditorAction.kt`): Split global default from per-project selected agent; toolbar and Run actions use `AgentConfigurationSelector` with workspace-scoped project state. made by: Olli Aalto. made with: Cursor. model: Composer

- **Settings UI structure** (`settings/`): Extract table models and UI factory helpers from `AgentSettingsConfigurable` to satisfy detekt size limits without behavior changes. made by: Olli Aalto. made with: Cursor. model: Composer

### Documentation

- **Per-project agent selection** (`CONTEXT.md`, `docs/adr/0003-per-project-agent-selection.md`): Glossary terms for **Selected agent** vs **Default agent configuration**; ADR records split persistence, migration, and `AgentConfigurationSelector` facade before implementation. made by: Olli Aalto. made with: Cursor. model: Composer

## 2026-06-16

### Added

- **Code quality tooling** (`build.gradle.kts`, `.editorconfig`, `detekt.yml`, `qodana.yml`, `.github/`): detekt static analysis, JaCoCo coverage reports, Kotlin `allWarningsAsErrors`, JDK 21 toolchain with Foojay auto-provisioning, Qodana workflow, Dependabot for Gradle/Actions/npm, and `package-lock.json`. CI now runs `qualityGate` and `verifyPlugin` before building artifacts; pre-commit runs `ktlintCheck` after wiki-lint. made by: Olli Aalto. made with: Cursor. model: Composer

- **ACP client session and UI** (`acp/`, `build.gradle.kts`): Kotlin ACP SDK 0.24.0; `AcpAgentEditor` with Transcript, Prompt, and idle Shell panes; `AcpSessionController` connect/newSession/prompt/dispose loop over stdio; WSL and node-wrapper launch via shared Command Builder; tool-call status lines in transcript. Implements PRD 3.0-02 issues 02-01 through 02-05. made by: Olli Aalto. made with: Cursor. model: Composer

- **3.0 launch slices and Launch Mode** (`settings/`, `pty/`, `acp/`, `AgentEditorFactory`): Per-configuration Launch Mode (Terminal / ACP) in settings with legacy migration to PTY Passthrough; PTY editor extracted to `pty` slice; ACP stub editor and factory routing by launch mode. Implements PRD 3.0-01 issues 01-01 through 01-03. made by: Olli Aalto. made with: Cursor. model: Composer

- **Worktree ACP session resume** (`worktree/`, `acp/`): Optional `acpSessionId` on managed worktree records; `ResumeStrategy` / `LaunchResumePlan` split PTY CLI resume from ACP `session/load` and `listSessions`; worktree launch coordinator; session picker fallback; bound-session UI indicator and pending-launch routing. Implements PRD 3.0-04 issues 04-01 through 04-06. made by: Olli Aalto. made with: Cursor. model: Composer

- **MCP exposure and acp.json portability** (`settings/`, `acp/mcp/`, `acp/AcpProcessLauncher.kt`, `acp/AcpSessionControllerImpl.kt`): Per-configuration `useIdeaMcp` / `useCustomMcp` toggles (default off) and ACP launch env vars persisted in `agentSettings.xml`; `McpCapabilityBridge` resolves IntelliJ and user MCP servers for ACP Client sessions; optional `com.intellij.mcpServer` dependency with runtime probe; manual import/export of `agent_servers` via Agent Settings toolbar. Implements PRD 3.0-05 issues 05-01 through 05-07. made by: Olli Aalto. made with: Cursor. model: Composer

### Fixed

- **MCP settings follow-up** (`settings/AiAssistantPresence.kt`, `settings/AgentSettingsConfigurable.kt`, `acp/AcpLaunchPlan.kt`, `pty/`, `worktree/resume/`, `CONTEXT.md`, `docs/adr/0001-custom-acp-client-in-plugin.md`): Enable IntelliJ MCP toggle when either AI Assistant or MCP Server plugin is present; hide MCP detail hints for PTY configs; pass explicit empty env maps at remaining `buildWslCommand` call sites; add `sessionMcpServers()` test coverage. made by: Olli Aalto. made with: Cursor. model: Composer

- **MCP and ACP launch review fixes** (`settings/`, `acp/`, `acp/mcp/`, `AgentCommandBuilder.kt`): Probe both JetBrains AI Assistant and MCP Server plugins; persist env vars on configuration row switch; inject WSL env via `env` prefix; gate session MCP servers with `exposeMcp`; skip invalid user MCP entries; avoid auto-starting IntelliJ MCP on launch; disable ACP-only detail controls for PTY; export WSL/node-wrapper metadata; warn before exporting plaintext env secrets. made by: Olli Aalto. made with: Cursor. model: Composer
```

---

## Key Code

### AcpSessionListener (contract)

```kotlin
interface AcpSessionListener {
    fun onTranscriptAppend(text: String)           // streaming chunks (escape at display)
    fun onFinalizeAgentStream()                    // remove cursor, commit stream block
    fun onTranscriptHtml(fragment: String)         // pre-rendered HTML line
    fun onTranscriptPlainLine(line: String)        // plain status line
    fun onError(message: String)
}
```

### Prompt event routing (AcpSessionControllerImpl)

```kotlin
private suspend fun handlePromptEvent(event: Event) {
    when (event) {
        is Event.SessionUpdateEvent -> AcpPromptEventDispatcher.dispatchSessionUpdate(event.update, listener)
        is Event.PromptResponseEvent -> AcpPromptEventDispatcher.dispatchPromptCompleted(listener)
    }
}
```

### Editor listener → appender mapping

```kotlin
override fun onTranscriptAppend(text: String) = transcript.startOrContinueAgentStream(text)
override fun onFinalizeAgentStream() = transcript.finalizeAgentStream()
override fun onTranscriptHtml(fragment: String) {
    transcript.finalizeAgentStream()
    transcript.appendHtml(fragment)
}
```

### Streaming state model (TranscriptHtmlAppender)

- `committedBodyHtml` — finalized transcript body (no cursor).
- `streamingPlainText` + `isAgentStreamActive` — live agent message; re-rendered each chunk with `TranscriptStreamingCursor.streamBlockHtml`.
- `finalizeAgentStream()` — moves stream into `committedBodyHtml` via `finalizedBlockHtml`, clears cursor.
- `syncPane()` — full document rewrite: `htmlDocumentStart() + currentBodyHtml() + HTML_DOCUMENT_END`.

---

## Architecture

```
User types prompt (PromptInputBar)
    → AcpAgentEditor: finalize stream, echo "> text"
    → AcpSessionControllerImpl.prompt()
        → collect prompt events
        → AcpPromptEventDispatcher.dispatchSessionUpdate / dispatchPromptCompleted
            → AcpSessionListener callbacks
                → TranscriptHtmlAppender (JEditorPane text/html)

Parallel path (agent-initiated notifications):
    AcpClientSessionOperationsImpl.notify(SessionUpdate)
        → TranscriptRenderer.renderUpdate()
        → listener.onTranscriptHtml()   [editor also finalizes in listener impl]

Terminal/fs/permission paths:
    AcpClientSessionOperationsImpl → listener.onTranscriptPlainLine / permissionCoordinator
```

**Layout:** `AcpEditorLayout.buildRootPanel` — vertical split 72% transcript column (scroll + auth north + permission south) / 28% bottom (20% prompt input / 80% shell).

**Finalize triggers:**
1. `AgentMessageChunk` → append only (no finalize).
2. Any other `SessionUpdate` via dispatcher → finalize then HTML.
3. `PromptResponseEvent` → finalize.
4. User sends new prompt → finalize before echo.
5. `cancelPrompt` / `dispose` / errors → finalize.
6. Editor `onTranscriptHtml` / `onTranscriptPlainLine` / `onError` → finalize first.

**Risk / open question:** `AcpClientSessionOperationsImpl.notify()` bypasses `AcpPromptEventDispatcher` and does not call `onFinalizeAgentStream()` itself. Finalize still happens because `AcpAgentEditor`'s listener finalizes inside `onTranscriptHtml`. Out-of-band `notify` during an active stream should be safe as long as all HTML paths go through the editor listener — but the pattern is inconsistent with the dispatcher.

---

## Start Here

**`src/main/kotlin/com/oaalto/agent/acp/AcpAgentEditor.kt`** — entry point tying UI (`TranscriptHtmlAppender`, `AcpEditorLayout`), `AcpSessionListener`, and `AcpSessionControllerImpl`. Any streaming-behavior change must align listener callbacks with `AcpPromptEventDispatcher` and consider `notify()` in `AcpClientSessionOperationsImpl`.
