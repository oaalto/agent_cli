package com.oaalto.agent.acp

import com.oaalto.agent.acp.transcript.model.TranscriptBlock
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

internal class DebouncedTranscriptSnapshotWriter(
    private val fileStore: TranscriptFileStore,
    private val blocksProvider: () -> List<TranscriptBlock>,
    private val debounceMs: Long = DEFAULT_DEBOUNCE_MS,
    private val scheduler: ScheduledExecutorService = SHARED_SCHEDULER,
    private val onWriteFailure: ((Throwable) -> Unit)? = null,
) {
    @Volatile
    private var sessionId: String? = null

    @Volatile
    private var disposed = false

    private var pendingFlush: ScheduledFuture<*>? = null

    fun setSessionId(id: String?) {
        if (sessionId == id) return
        flushNow()
        sessionId = id
        if (id != null) {
            scheduleFlush()
        }
    }

    fun onBlocksChanged() {
        if (disposed) return
        scheduleFlush()
    }

    fun flushNow() {
        pendingFlush?.cancel(false)
        pendingFlush = null
        val id = sessionId ?: return
        val content = TranscriptTextSerializer.serialize(blocksProvider())
        fileStore.write(id, content).onFailure { throwable ->
            onWriteFailure?.invoke(throwable)
        }
    }

    fun dispose() {
        pendingFlush?.cancel(false)
        pendingFlush = null
        flushNow()
        disposed = true
    }

    private fun scheduleFlush() {
        if (disposed) return
        pendingFlush?.cancel(false)
        pendingFlush =
            scheduler.schedule(
                {
                    if (!disposed) {
                        flushNow()
                    }
                },
                debounceMs,
                TimeUnit.MILLISECONDS,
            )
    }

    companion object {
        const val DEFAULT_DEBOUNCE_MS = 400L

        // ponytail: single shared scheduler; dedicated per-editor executor if write contention shows up
        private val SHARED_SCHEDULER: ScheduledExecutorService =
            Executors.newSingleThreadScheduledExecutor { runnable ->
                Thread(runnable, "agent-cli-transcript-writer").apply { isDaemon = true }
            }
    }
}
