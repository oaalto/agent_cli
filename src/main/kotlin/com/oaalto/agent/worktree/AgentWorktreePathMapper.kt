package com.oaalto.agent.worktree

import java.nio.file.Path
import java.util.Locale

internal object AgentWorktreePathMapper {
    private val wslUncPrefixes = listOf("\\\\wsl.localhost\\", "\\\\wsl$\\")

    fun mapHostPathToGitPath(
        hostPath: String,
        repositoryRootPath: String,
    ): String {
        val repositoryWslInfo = parseWslUncPath(repositoryRootPath)
        val worktreeWslInfo = parseWslUncPath(hostPath)
        return when {
            repositoryWslInfo == null || worktreeWslInfo == null -> hostPath
            !repositoryWslInfo.distribution.equals(worktreeWslInfo.distribution, ignoreCase = true) -> hostPath
            else -> worktreeWslInfo.linuxPath
        }
    }

    fun mapGitPathToHostPath(
        gitPath: String,
        mainRepositoryPath: String,
    ): String {
        val mainRepositoryWslInfo = parseWslUncPath(mainRepositoryPath)
        val trimmedGitPath = gitPath.trim()
        return when {
            trimmedGitPath.isBlank() -> trimmedGitPath
            trimmedGitPath.startsWith("/") && mainRepositoryWslInfo != null ->
                linuxPathToWslUnc(trimmedGitPath, mainRepositoryWslInfo.distribution)
            else -> trimmedGitPath
        }
    }

    fun parseWslUncPath(rawPath: String): WslUncPath? {
        val windowsStylePath = rawPath.trim().replace('/', '\\')
        val prefix =
            wslUncPrefixes.firstOrNull { windowsStylePath.startsWith(it, ignoreCase = true) }
                ?: return null
        val withoutPrefix = windowsStylePath.substring(prefix.length)
        val segments = withoutPrefix.split('\\').filter { it.isNotBlank() }
        return when {
            windowsStylePath.isBlank() || segments.isEmpty() -> null
            else -> {
                val distribution = segments.first()
                val linuxSegments = segments.drop(1)
                val linuxPath = if (linuxSegments.isEmpty()) "/" else "/${linuxSegments.joinToString("/")}"
                WslUncPath(distribution = distribution, linuxPath = linuxPath)
            }
        }
    }

    fun normalizePath(path: String): String {
        val trimmed = path.trim()
        if (trimmed.isBlank()) return trimmed
        return runCatching {
            Path
                .of(trimmed)
                .toAbsolutePath()
                .normalize()
                .toString()
        }.getOrElse { trimmed }
    }

    fun normalizePathKey(path: String): String {
        var value =
            normalizePath(path)
                .replace('\\', '/')
                .lowercase(Locale.ROOT)
        if (value.startsWith("//wsl$/")) {
            value = value.replaceFirst("//wsl$/", "//wsl.localhost/")
        }
        return value
    }

    private fun linuxPathToWslUnc(
        linuxPath: String,
        distribution: String,
    ): String {
        val normalizedLinuxPath = if (linuxPath.startsWith("/")) linuxPath else "/$linuxPath"
        val windowsTail = normalizedLinuxPath.replace('/', '\\')
        return "\\\\wsl.localhost\\$distribution$windowsTail"
    }
}

internal data class WslUncPath(
    val distribution: String,
    val linuxPath: String,
)
