package com.oaalto.agent.acp

import com.oaalto.agent.acp.transcript.model.StructuredUpdate
import com.oaalto.agent.acp.transcript.model.TranscriptModel
import java.nio.file.Files
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals

class DebouncedTranscriptSnapshotWriterTest {
    @Test
    fun `flushNow writes serialized blocks to file store`() {
        val projectDir = Files.createTempDirectory("transcript-writer").toFile()
        projectDir.deleteOnExit()
        val store = TranscriptFileStore(projectDir.absolutePath)
        val model = TranscriptModel()
        model.apply(StructuredUpdate.AppendUserEcho("hello"))
        model.apply(StructuredUpdate.AppendAgentText("world"))
        model.apply(StructuredUpdate.FinalizeAgentStream)
        val writer =
            DebouncedTranscriptSnapshotWriter(
                fileStore = store,
                blocksProvider = { model.blocks() },
                debounceMs = 0,
                scheduler = Executors.newSingleThreadScheduledExecutor(),
            )

        writer.setSessionId("session-1")
        writer.flushNow()

        assertEquals("> hello\nworld", store.read("session-1").getOrThrow())
        writer.dispose()
    }

    @Test
    fun `no write when session id is unset`() {
        val projectDir = Files.createTempDirectory("transcript-writer").toFile()
        projectDir.deleteOnExit()
        val store = TranscriptFileStore(projectDir.absolutePath)
        val model = TranscriptModel()
        model.apply(StructuredUpdate.AppendUserEcho("orphan"))
        val writer =
            DebouncedTranscriptSnapshotWriter(
                fileStore = store,
                blocksProvider = { model.blocks() },
                debounceMs = 0,
                scheduler = Executors.newSingleThreadScheduledExecutor(),
            )

        writer.flushNow()

        assertEquals("", store.read("session-orphan").getOrThrow())
        writer.dispose()
    }

    @Test
    fun `session id change switches file target`() {
        val projectDir = Files.createTempDirectory("transcript-writer").toFile()
        projectDir.deleteOnExit()
        val store = TranscriptFileStore(projectDir.absolutePath)
        val model = TranscriptModel()
        val writer =
            DebouncedTranscriptSnapshotWriter(
                fileStore = store,
                blocksProvider = { model.blocks() },
                debounceMs = 0,
                scheduler = Executors.newSingleThreadScheduledExecutor(),
            )

        model.apply(StructuredUpdate.AppendUserEcho("first"))
        writer.setSessionId("session-a")
        writer.flushNow()

        writer.setSessionId("session-b")
        model.apply(StructuredUpdate.AppendUserEcho("second"))
        writer.flushNow()

        assertEquals("> first", store.read("session-a").getOrThrow())
        assertEquals("> first\n> second", store.read("session-b").getOrThrow())
        writer.dispose()
    }

    @Test
    fun `onBlocksChanged schedules flush without asserting delay`() {
        val projectDir = Files.createTempDirectory("transcript-writer").toFile()
        projectDir.deleteOnExit()
        val store = TranscriptFileStore(projectDir.absolutePath)
        val model = TranscriptModel()
        val scheduler = Executors.newSingleThreadScheduledExecutor()
        val writer =
            DebouncedTranscriptSnapshotWriter(
                fileStore = store,
                blocksProvider = { model.blocks() },
                debounceMs = 50,
                scheduler = scheduler,
            )
        writer.setSessionId("session-delay")

        model.apply(StructuredUpdate.AppendUserEcho("delayed"))
        writer.onBlocksChanged()
        writer.flushNow()

        assertEquals("> delayed", store.read("session-delay").getOrThrow())
        writer.dispose()
        scheduler.shutdown()
        scheduler.awaitTermination(1, TimeUnit.SECONDS)
    }
}
