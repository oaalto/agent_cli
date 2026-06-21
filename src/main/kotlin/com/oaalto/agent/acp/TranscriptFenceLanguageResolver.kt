package com.oaalto.agent.acp

import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.fileTypes.PlainTextFileType

/** Maps fence language tags to IntelliJ [FileType] instances for syntax highlighting. */
internal object TranscriptFenceLanguageResolver {
    private val languageAliases =
        mapOf(
            "kotlin" to "kt",
            "javascript" to "js",
            "typescript" to "ts",
            "python" to "py",
            "shell" to "sh",
            "bash" to "sh",
            "markdown" to "md",
            "yaml" to "yml",
            "dockerfile" to "dockerfile",
        )

    fun normalizeExtension(languageId: String?): String? {
        if (languageId.isNullOrBlank()) {
            return null
        }
        val normalized = languageId.lowercase().trim()
        return languageAliases[normalized] ?: normalized
    }

    fun resolveFileType(languageId: String?): FileType {
        val extension = normalizeExtension(languageId) ?: return PlainTextFileType.INSTANCE
        val resolved = FileTypeManager.getInstance().getFileTypeByExtension(extension)
        return resolved ?: PlainTextFileType.INSTANCE
    }
}
