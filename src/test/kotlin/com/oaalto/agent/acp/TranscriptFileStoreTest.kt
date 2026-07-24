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
    fun `resolve path is under idea agent-cli transcripts`() {
        val projectDir = Files.createTempDirectory("transcript-store").toFile()
        projectDir.deleteOnExit()
        val store = TranscriptFileStore(projectDir.absolutePath)

        val path = store.resolvePath("abc123").toString().replace('\\', '/')

        assertTrue(path.endsWith(".idea/agent-cli/transcripts/abc123.txt"))
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
