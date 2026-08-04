package com.oaalto.agent.acp

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap

internal class TranscriptFileStore(
    private val projectBasePath: String,
) {
    private val sessionPaths = ConcurrentHashMap<String, Path>()

    fun bindSession(sessionId: String) {
        sessionPaths.computeIfAbsent(sessionId) { resolveExistingPath(sessionId) ?: newPath(sessionId) }
    }

    fun resolvePath(sessionId: String): Path =
        sessionPaths[sessionId]
            ?: resolveExistingPath(sessionId)
            ?: newPath(sessionId)

    fun resolveDirectory(): Path = Paths.get(projectBasePath, TRANSCRIPT_DIR).normalize()

    fun read(sessionId: String): Result<String> =
        runCatching {
            val path = resolveExistingPath(sessionId) ?: return@runCatching ""
            Files.readString(path, StandardCharsets.UTF_8)
        }

    fun write(
        sessionId: String,
        content: String,
    ): Result<Unit> =
        runCatching {
            bindSession(sessionId)
            val path = sessionPaths.getValue(sessionId)
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

    private fun newPath(
        sessionId: String,
        startedAt: LocalDateTime = LocalDateTime.now(),
    ): Path {
        val timestamp = TIMESTAMP_FORMAT.format(startedAt)
        val sanitized = sanitizeSessionId(sessionId)
        return resolveDirectory().resolve("${timestamp}_$sanitized.txt")
    }

    private fun resolveExistingPath(sessionId: String): Path? {
        val sanitized = sanitizeSessionId(sessionId)
        val directory = resolveDirectory()
        if (Files.isDirectory(directory)) {
            Files.list(directory).use { stream ->
                val timestamped =
                    stream
                        .filter { Files.isRegularFile(it) }
                        .filter { path ->
                            path.fileName.toString().endsWith("_$sanitized.txt")
                        }.findFirst()
                if (timestamped.isPresent) {
                    return timestamped.get()
                }
            }
        }
        val legacy = directory.resolve("$sessionId.txt")
        return legacy.takeIf { Files.isRegularFile(it) }
    }

    private fun sanitizeSessionId(sessionId: String): String {
        val replaced =
            sessionId
                .map { character ->
                    if (isFilesystemSafeSessionIdCharacter(character)) character else '-'
                }.joinToString("")
        return replaced.replace(Regex("-+"), "-").trim('-')
    }

    private fun isFilesystemSafeSessionIdCharacter(character: Char): Boolean =
        character.isLetterOrDigit() || character == '.' || character == '_' || character == '-'

    companion object {
        private const val TRANSCRIPT_DIR = ".idea/agent-cli/transcripts"
        private val TIMESTAMP_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")

        fun resolveTranscriptsDirectory(projectBasePath: String): Path =
            Paths.get(projectBasePath, TRANSCRIPT_DIR).normalize()
    }
}
