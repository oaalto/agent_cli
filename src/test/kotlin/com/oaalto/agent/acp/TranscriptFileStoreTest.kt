package com.oaalto.agent.acp

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TranscriptFileStoreTest {
    @Test
    fun `read missing file returns empty content`() {
        val projectDir = Files.createTempDirectory("transcript-store").toFile()
        projectDir.deleteOnExit()
        val store = TranscriptFileStore(projectDir.absolutePath)

        val result = store.read("session-a")

        assertTrue(result.isSuccess)
        assertEquals("", result.getOrThrow())
    }

    @Test
    fun `write creates directory and round trips content`() {
        val projectDir = Files.createTempDirectory("transcript-store").toFile()
        projectDir.deleteOnExit()
        val store = TranscriptFileStore(projectDir.absolutePath)
        val sessionId = "session-b"
        val content = "> hello\nagent reply"

        val writeResult = store.write(sessionId, content)
        val readResult = store.read(sessionId)

        assertTrue(writeResult.isSuccess)
        assertTrue(readResult.isSuccess)
        assertEquals(content, readResult.getOrThrow())
        assertTrue(Files.exists(store.resolvePath(sessionId)))
        assertTrue(
            store
                .resolvePath(sessionId)
                .fileName
                .toString()
                .matches(Regex("\\d{4}-\\d{2}-\\d{2}_\\d{2}-\\d{2}-\\d{2}_session-b\\.txt")),
        )
    }

    @Test
    fun `overwrite replaces entire snapshot`() {
        val projectDir = Files.createTempDirectory("transcript-store").toFile()
        projectDir.deleteOnExit()
        val store = TranscriptFileStore(projectDir.absolutePath)
        val sessionId = "session-c"

        val writeResult = store.write(sessionId, "first snapshot")
        val secondWrite = store.write(sessionId, "second snapshot")

        assertTrue(writeResult.isSuccess)
        assertTrue(secondWrite.isSuccess)

        assertEquals("second snapshot", store.read(sessionId).getOrThrow())
    }

    @Test
    fun `sessions are isolated by session id`() {
        val projectDir = Files.createTempDirectory("transcript-store").toFile()
        projectDir.deleteOnExit()
        val store = TranscriptFileStore(projectDir.absolutePath)

        assertTrue(store.write("session-a", "content-a").isSuccess)
        assertTrue(store.write("session-b", "content-b").isSuccess)

        assertEquals("content-a", store.read("session-a").getOrThrow())
        assertEquals("content-b", store.read("session-b").getOrThrow())
    }

    @Test
    fun `resolve path uses timestamped filename under idea agent-cli transcripts`() {
        val projectDir = Files.createTempDirectory("transcript-store").toFile()
        projectDir.deleteOnExit()
        val store = TranscriptFileStore(projectDir.absolutePath)
        store.bindSession("abc123")

        val path = store.resolvePath("abc123").toString().replace('\\', '/')

        assertTrue(path.contains(".idea/agent-cli/transcripts/"))
        assertTrue(path.endsWith("_abc123.txt"))
    }

    @Test
    fun `read finds timestamped file by session id suffix`() {
        val projectDir = Files.createTempDirectory("transcript-store").toFile()
        projectDir.deleteOnExit()
        val store = TranscriptFileStore(projectDir.absolutePath)
        val transcriptsDir = store.resolveDirectory()
        Files.createDirectories(transcriptsDir)
        val file = transcriptsDir.resolve("2026-08-04_08-26-30_session-d.txt")
        Files.writeString(file, "restored content")

        assertEquals("restored content", store.read("session-d").getOrThrow())
    }

    @Test
    fun `read falls back to legacy session id filename`() {
        val projectDir = Files.createTempDirectory("transcript-store").toFile()
        projectDir.deleteOnExit()
        val store = TranscriptFileStore(projectDir.absolutePath)
        val transcriptsDir = store.resolveDirectory()
        Files.createDirectories(transcriptsDir)
        val file = transcriptsDir.resolve("legacy-session.txt")
        Files.writeString(file, "legacy content")

        assertEquals("legacy content", store.read("legacy-session").getOrThrow())
    }

    @Test
    fun `bind session reuses existing timestamped file for resume writes`() {
        val projectDir = Files.createTempDirectory("transcript-store").toFile()
        projectDir.deleteOnExit()
        val store = TranscriptFileStore(projectDir.absolutePath)
        val transcriptsDir = store.resolveDirectory()
        Files.createDirectories(transcriptsDir)
        val existing = transcriptsDir.resolve("2026-08-01_10-30-00_session-e.txt")
        Files.writeString(existing, "original")

        store.bindSession("session-e")
        assertTrue(store.write("session-e", "updated").isSuccess)

        assertEquals("updated", Files.readString(existing))
        assertEquals(1, Files.list(transcriptsDir).count())
    }

    @Test
    fun `sanitize session id for filesystem unsafe characters`() {
        val projectDir = Files.createTempDirectory("transcript-store").toFile()
        projectDir.deleteOnExit()
        val store = TranscriptFileStore(projectDir.absolutePath)
        val sessionId = "session/with:unsafe"

        store.bindSession(sessionId)

        val fileName = store.resolvePath(sessionId).fileName.toString()
        assertTrue(fileName.endsWith("_session-with-unsafe.txt"))
    }

    @Test
    fun `companion resolveTranscriptsDirectory returns normalized directory path`() {
        val projectDir = Files.createTempDirectory("transcript-store").toFile()
        projectDir.deleteOnExit()

        val path =
            TranscriptFileStore
                .resolveTranscriptsDirectory(projectDir.absolutePath)
                .toString()
                .replace('\\', '/')

        assertTrue(path.endsWith(".idea/agent-cli/transcripts"))
    }

    @Test
    fun `companion resolves to directory under given base path`() {
        val projectDir = Files.createTempDirectory("transcript-store").toFile()
        projectDir.deleteOnExit()
        val store = TranscriptFileStore(projectDir.absolutePath)

        val directory =
            TranscriptFileStore
                .resolveTranscriptsDirectory(projectDir.absolutePath)
        val storeDirectory = store.resolveDirectory()

        assertEquals(storeDirectory, directory)
    }
}
