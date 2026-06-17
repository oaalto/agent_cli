package com.oaalto.agent

import java.nio.file.Files
import java.nio.file.Path
import java.util.Locale

data class ResolvedWslPath(
    val linuxPath: String,
    val inferredDistribution: String?,
)

object WslPathResolver {
    private val uncWslPrefixes = listOf("\\\\wsl.localhost\\", "\\\\wsl$\\")
    private val windowsDrivePathRegex = Regex("""^([A-Za-z]):\\(.*)$""")

    fun mapToWslPath(rawPath: String): ResolvedWslPath? {
        val trimmed = rawPath.trim()
        return when {
            trimmed.isBlank() -> null
            else -> {
                val windowsStylePath = trimmed.replace('/', '\\')
                parseUncWslPath(windowsStylePath) ?: mapNonUncWslPath(trimmed, windowsStylePath)
            }
        }
    }

    private fun parseUncWslPath(windowsStylePath: String): ResolvedWslPath? {
        val uncPrefix =
            uncWslPrefixes.firstOrNull { prefix ->
                windowsStylePath.startsWith(prefix, ignoreCase = true)
            } ?: return null
        val withoutPrefix = windowsStylePath.substring(uncPrefix.length)
        val segments = withoutPrefix.split('\\').filter { it.isNotBlank() }
        return when {
            segments.isEmpty() -> null
            else -> {
                val inferredDistribution = segments.first()
                val linuxSegments = segments.drop(1)
                val linuxPath = if (linuxSegments.isEmpty()) "/" else "/${linuxSegments.joinToString("/")}"
                ResolvedWslPath(
                    linuxPath = linuxPath,
                    inferredDistribution = inferredDistribution,
                )
            }
        }
    }

    private fun mapNonUncWslPath(
        trimmed: String,
        windowsStylePath: String,
    ): ResolvedWslPath? =
        when {
            trimmed.startsWith("/") || trimmed.startsWith("~") ->
                ResolvedWslPath(linuxPath = trimmed, inferredDistribution = null)
            else -> {
                val match = windowsDrivePathRegex.matchEntire(windowsStylePath)
                when {
                    match != null -> {
                        val drive = match.groupValues[1].lowercase(Locale.ROOT)
                        val rest = match.groupValues[2].replace('\\', '/').trim('/')
                        ResolvedWslPath(
                            linuxPath = if (rest.isBlank()) "/mnt/$drive" else "/mnt/$drive/$rest",
                            inferredDistribution = null,
                        )
                    }
                    !windowsStylePath.contains('\\') ->
                        ResolvedWslPath(linuxPath = trimmed, inferredDistribution = null)
                    else -> null
                }
            }
        }

    fun resolveWslWorkingDirectory(
        configuredWorkingDirectory: String,
        overrideWorkingDirectory: String?,
        projectBasePath: String?,
    ): ResolvedWslPath {
        val candidatePaths =
            listOf(
                overrideWorkingDirectory?.trim().orEmpty(),
                configuredWorkingDirectory.trim(),
                projectBasePath?.trim().orEmpty(),
            )
        val mapped = candidatePaths.firstOrNull { it.isNotBlank() }?.let(::mapToWslPath)
        return mapped ?: ResolvedWslPath(linuxPath = "/home", inferredDistribution = null)
    }

    fun resolveHostWorkingDirectory(vararg candidates: String?): String =
        candidates
            .asSequence()
            .mapNotNull { it?.trim()?.takeIf(String::isNotBlank) }
            .firstOrNull { path ->
                kotlin.runCatching { Files.isDirectory(Path.of(path)) }.getOrDefault(false)
            }
            ?: System.getProperty("user.home")
}

object WorkingDirectoryResolver {
    fun resolve(
        configuredWorkingDirectory: String,
        overrideWorkingDirectory: String?,
        projectBasePath: String?,
    ): String {
        val overrideValue = overrideWorkingDirectory?.trim().orEmpty()
        if (overrideValue.isNotBlank()) return overrideValue
        val configured = configuredWorkingDirectory.trim()
        return when {
            configured.isNotBlank() -> configured
            !projectBasePath.isNullOrBlank() -> projectBasePath
            else -> System.getProperty("user.home")
        }
    }
}
