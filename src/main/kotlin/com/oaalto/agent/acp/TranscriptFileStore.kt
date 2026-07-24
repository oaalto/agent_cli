package com.oaalto.agent.acp

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption

internal class TranscriptFileStore(
    private val projectBasePath: String,
) {
    fun resolvePath(sessionId: String): Path =
        Paths
            .get(projectBasePath, TRANSCRIPT_DIR, "$sessionId.txt")
            .normalize()

    fun read(sessionId: String): Result<String> =
        runCatching {
            val path = resolvePath(sessionId)
            if (!Files.exists(path)) return@runCatching ""
            Files.readString(path, StandardCharsets.UTF_8)
        }

    fun write(
        sessionId: String,
        content: String,
    ): Result<Unit> =
        runCatching {
            val path = resolvePath(sessionId)
            Files.createDirectories(path.parent)
            Files.writeString(
                path,
                content,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE,
            )
        }

    companion object {
        private const val TRANSCRIPT_DIR = ".idea/agent-cli/transcripts"
    }
}
