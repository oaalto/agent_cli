package com.oaalto.agent.acp

import com.oaalto.agent.AgentCliCorrelationToken
import com.oaalto.agent.AgentCliSessionContext
import com.oaalto.agent.settings.LaunchMode

internal data class SessionTranscriptCallbacks(
    val blocksProvider: () -> List<TranscriptBlock>,
    val restoreLines: (String) -> Unit,
    val appendPlainLine: (String) -> Unit,
    val logWarn: (String, Throwable?, AgentCliSessionContext?) -> Unit,
    val logContext: () -> AgentCliSessionContext,
)

internal class SessionTranscriptCoordinator(
    projectBasePath: String?,
    private val callbacks: SessionTranscriptCallbacks,
) {
    val diagnostics = SessionDiagnosticsCollector()
    private val fileStore = projectBasePath?.let { TranscriptFileStore(it) }
    private val snapshotWriter =
        fileStore?.let { store ->
            DebouncedTranscriptSnapshotWriter(
                fileStore = store,
                blocksProvider = callbacks.blocksProvider,
                onWriteFailure = { throwable -> logWriteFailure(throwable) },
            )
        }

    fun onBlocksChanged() {
        snapshotWriter?.onBlocksChanged()
    }

    fun decorateError(message: String): String {
        val token = AgentCliCorrelationToken.generate()
        diagnostics.record(token, message)
        val tokenSuffix = AgentCliCorrelationToken.format(token)
        callbacks.logWarn("Transcript error: $message $tokenSuffix", null, callbacks.logContext())
        return "$message $tokenSuffix"
    }

    fun restoreIfPresent(sessionId: String) {
        val store = fileStore ?: return
        store.read(sessionId).fold(
            onSuccess = { content -> callbacks.restoreLines(content) },
            onFailure = { throwable ->
                val token = AgentCliCorrelationToken.generate()
                diagnostics.record(token, "Failed to restore session transcript")
                callbacks.logWarn(
                    "Transcript restore failed ${AgentCliCorrelationToken.format(token)}: ${throwable.message}",
                    throwable,
                    callbacks.logContext().copy(sessionId = sessionId),
                )
                callbacks.appendPlainLine(
                    "Error: Failed to restore session transcript ${AgentCliCorrelationToken.format(token)}",
                )
            },
        )
    }

    fun bindSession(sessionId: String) {
        snapshotWriter?.setSessionId(sessionId)
    }

    fun dispose() {
        snapshotWriter?.dispose()
    }

    fun clipboardText(): String {
        val context = callbacks.logContext()
        return diagnostics.formatClipboardBundle(
            context = context,
            worktreeLabel =
                SessionDiagnosticsCollector.worktreeLabel(
                    context.launchMode ?: LaunchMode.ACP_CLIENT,
                    context.worktreePath,
                ),
        )
    }

    private fun logWriteFailure(throwable: Throwable) {
        val token = AgentCliCorrelationToken.generate()
        diagnostics.record(token, "Failed to save session transcript")
        callbacks.logWarn(
            "Transcript write failed ${AgentCliCorrelationToken.format(token)}: ${throwable.message}",
            throwable,
            callbacks.logContext(),
        )
    }
}
