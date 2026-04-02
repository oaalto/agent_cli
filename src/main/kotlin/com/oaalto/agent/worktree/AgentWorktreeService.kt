package com.oaalto.agent.worktree

import com.oaalto.agent.settings.AgentSettingsState
import com.intellij.ide.impl.OpenProjectTask
import com.intellij.ide.impl.ProjectUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.FilePath
import com.intellij.openapi.vcs.VcsException
import com.intellij.openapi.vcs.actions.VcsContextFactory
import git4idea.commands.Git
import git4idea.commands.GitCommand
import git4idea.commands.GitLineHandler
import git4idea.repo.GitRepository
import git4idea.repo.GitRepositoryManager
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class AgentWorktreeService(
    private val project: Project,
) {
    private val git: Git = Git.getInstance()

    data class CreatedWorktree(
        val repositoryRootPath: String,
        val worktreePath: String,
        val branchName: String,
    )

    data class ManagedWorktree(
        val record: AgentWorktreeStateService.ManagedWorktreeRecord,
        val branchName: String?,
        val isMain: Boolean,
        val isCurrent: Boolean,
    )

    fun createWorktree(configuration: AgentSettingsState.AgentCliConfiguration): Result<CreatedWorktree> {
        val repository = resolveSingleRepository().getOrElse { return Result.failure(IllegalStateException(it.message)) }
        val sourceBranch = repository.currentBranch
        if (sourceBranch == null) {
            return Result.failure(IllegalStateException("Cannot create an agent worktree while repository is in detached HEAD state."))
        }

        val repoRootPath = repository.root.path
        val worktreePath = generateWorktreePath(repoRootPath, configuration.name)
            .getOrElse { return Result.failure(IllegalStateException(it.message)) }
        val branchName = buildBranchName(configuration.name)
        val filePath = createFilePath(worktreePath)
            .getOrElse { return Result.failure(IllegalStateException(it.message)) }
        val gitWorktreePath = mapHostPathToGitPath(
            hostPath = filePath.path,
            repositoryRootPath = repoRootPath,
        )

        val result = runGitWorktreeCommand(
            repository = repository,
            arguments = listOf(
                "add",
                "-b",
                branchName,
                gitWorktreePath,
                sourceBranch.fullName,
            ),
        ).getOrElse { throwable ->
            return Result.failure(IllegalStateException(throwable.message ?: "Failed to create worktree."))
        }
        if (!result.success()) {
            return Result.failure(
                IllegalStateException(
                    "Failed to create worktree at:\n$worktreePath\n" +
                        "(Git path: $gitWorktreePath)\n\n${result.getErrorOutputAsJoinedString()}",
                ),
            )
        }
        if (!Files.isDirectory(Path.of(worktreePath))) {
            return Result.failure(
                IllegalStateException(
                    "Git reported success, but the worktree directory was not found:\n$worktreePath",
                ),
            )
        }
        return Result.success(
            CreatedWorktree(
                repositoryRootPath = repoRootPath,
                worktreePath = worktreePath,
                branchName = branchName,
            ),
        )
    }

    fun listManagedWorktrees(configurationId: String): Result<List<ManagedWorktree>> {
        val repository = resolveSingleRepository().getOrElse { return Result.failure(IllegalStateException(it.message)) }
        val worktrees = listWorktrees(repository)
            .getOrElse { throwable ->
                return Result.failure(IllegalStateException(throwable.message ?: "Failed to list worktrees."))
            }
        val byPath = worktrees.associateBy { normalizePathKey(it.hostPath) }
        val records = AgentWorktreeStateService.getInstance().getActiveRecords(
            configurationId = configurationId,
            repositoryRootPath = repository.root.path,
        )
        val managed = records.mapNotNull { record ->
            val tree = byPath[normalizePathKey(record.worktreePath)] ?: return@mapNotNull null
            ManagedWorktree(
                record = record,
                branchName = tree.branchFullName,
                isMain = tree.isMain,
                isCurrent = tree.isCurrent,
            )
        }
        return Result.success(managed)
    }

    fun deleteWorktree(worktreePath: String): Result<Unit> {
        val repository = resolveSingleRepository().getOrElse { return Result.failure(IllegalStateException(it.message)) }
        val existingTrees = listWorktrees(repository)
            .getOrElse { throwable ->
                return Result.failure(IllegalStateException(throwable.message ?: "Failed to list worktrees."))
            }
        val tree = existingTrees.firstOrNull { normalizePathKey(it.hostPath) == normalizePathKey(worktreePath) }
            ?: return Result.failure(IllegalStateException("Worktree not found: $worktreePath"))
        if (tree.isMain) {
            return Result.failure(IllegalStateException("Cannot delete the main worktree."))
        }
        if (tree.isCurrent) {
            return Result.failure(IllegalStateException("Cannot delete the currently opened worktree."))
        }
        val result = runGitWorktreeCommand(
            repository = repository,
            arguments = listOf("remove", tree.gitPath),
        ).getOrElse { throwable ->
            return Result.failure(IllegalStateException(throwable.message ?: "Failed to delete worktree."))
        }
        if (!result.success()) {
            return Result.failure(IllegalStateException("Failed to delete worktree:\n${result.getErrorOutputAsJoinedString()}"))
        }
        AgentWorktreeStateService.getInstance().markDeleted(worktreePath)
        return Result.success(Unit)
    }

    fun openWorktreeProject(worktreePath: String): Result<Unit> {
        val normalizedPath = normalizePath(worktreePath)
        if (!Files.isDirectory(Path.of(normalizedPath))) {
            return Result.failure(IllegalStateException("Worktree directory does not exist:\n$normalizedPath"))
        }
        return runCatching {
            val openedProject = ProjectUtil.openOrImport(Path.of(normalizedPath), OpenProjectTask())
            if (openedProject == null) {
                throw IllegalStateException("IDE refused to open worktree:\n$normalizedPath")
            }
            Unit
        }.recoverCatching { throwable ->
            throw IllegalStateException("Failed to open worktree:\n${throwable.message ?: throwable.javaClass.simpleName}")
        }
    }

    private fun resolveSingleRepository(): Result<GitRepository> {
        val repositories = GitRepositoryManager.getInstance(project).repositories
        return when (repositories.size) {
            0 -> Result.failure(IllegalStateException("No Git repository was detected for this project."))
            1 -> Result.success(repositories.first())
            else -> Result.failure(
                IllegalStateException(
                    "Agent worktrees currently support single-repository projects only. Found ${repositories.size} repositories.",
                ),
            )
        }
    }

    private fun generateWorktreePath(repositoryRootPath: String, configurationName: String): Result<String> {
        return runCatching {
            val repositoryRoot = Path.of(repositoryRootPath).toAbsolutePath().normalize()
            val parent = repositoryRoot.parent ?: error("Repository path has no parent directory.")
            val containerName = "${repositoryRoot.fileName}-agent-worktrees"
            val container = parent.resolve(containerName)
            Files.createDirectories(container)
            val suffix = buildPathSuffix(configurationName)
            container.resolve(suffix).toAbsolutePath().normalize().toString()
        }.recoverCatching { throwable ->
            throw IllegalStateException("Failed to resolve worktree path: ${throwable.message ?: throwable.javaClass.simpleName}")
        }
    }

    private fun buildBranchName(configurationName: String): String {
        val timestamp = BRANCH_TIMESTAMP_FORMAT.format(LocalDateTime.now())
        return "agent/${slug(configurationName)}/$timestamp"
    }

    private fun buildPathSuffix(configurationName: String): String {
        val timestamp = PATH_TIMESTAMP_FORMAT.format(LocalDateTime.now())
        return "${slug(configurationName)}-$timestamp"
    }

    private fun slug(rawValue: String): String {
        val normalized = rawValue.trim().lowercase(Locale.ROOT)
        val slug = normalized.replace(SLUG_INVALID_CHARACTERS, "-")
            .replace(SLUG_SEPARATOR_RUNS, "-")
            .trim('-')
        return if (slug.isBlank()) "agent" else slug
    }

    private fun createFilePath(path: String): Result<FilePath> {
        return runCatching {
            VcsContextFactory.getInstance().createFilePath(path, true)
        }.recoverCatching { throwable ->
            throw IllegalStateException("Failed to create VCS file path: ${throwable.message ?: throwable.javaClass.simpleName}")
        }
    }

    private fun runGitWorktreeCommand(
        repository: GitRepository,
        arguments: List<String>,
    ) = runCatching {
        val handler = GitLineHandler(project, repository.root, GitCommand.WORKTREE)
        handler.addParameters(arguments)
        git.runCommand(handler)
    }.recoverCatching { throwable ->
        val message = (throwable as? VcsException)?.message ?: throwable.message ?: throwable.javaClass.simpleName
        throw IllegalStateException("Failed to execute `git worktree ${arguments.joinToString(" ")}`:\n$message")
    }

    private fun listWorktrees(repository: GitRepository): Result<List<ParsedWorktree>> {
        val result = runGitWorktreeCommand(
            repository = repository,
            arguments = listOf("list", "--porcelain"),
        ).getOrElse { throwable ->
            return Result.failure(IllegalStateException(throwable.message ?: "Failed to list worktrees."))
        }
        if (!result.success()) {
            return Result.failure(IllegalStateException("Failed to list worktrees:\n${result.getErrorOutputAsJoinedString()}"))
        }
        return Result.success(
            parseWorktreeList(
                outputLines = result.output,
                mainRepositoryPath = normalizePath(repository.root.path),
                currentProjectPath = normalizePath(project.basePath.orEmpty()),
            ),
        )
    }

    private fun parseWorktreeList(
        outputLines: List<String>,
        mainRepositoryPath: String,
        currentProjectPath: String,
    ): List<ParsedWorktree> {
        val items = mutableListOf<ParsedWorktree>()
        val mainRepositoryWslInfo = parseWslUncPath(mainRepositoryPath)
        var currentPath: String? = null
        var currentBranch: String? = null

        fun flushCurrent() {
            val gitPath = currentPath ?: return
            val hostPath = mapGitPathToHostPath(
                gitPath = gitPath,
                mainRepositoryWslInfo = mainRepositoryWslInfo,
            )
            items += ParsedWorktree(
                hostPath = normalizePath(hostPath),
                gitPath = gitPath.trim(),
                branchFullName = currentBranch,
                isMain = normalizePathKey(hostPath) == normalizePathKey(mainRepositoryPath),
                isCurrent = currentProjectPath.isNotBlank() &&
                    normalizePathKey(hostPath) == normalizePathKey(currentProjectPath),
            )
            currentPath = null
            currentBranch = null
        }

        outputLines.forEach { line ->
            when {
                line.startsWith("worktree ") -> {
                    flushCurrent()
                    currentPath = line.removePrefix("worktree ").trim()
                }
                line.startsWith("branch ") -> {
                    currentBranch = line.removePrefix("branch ").trim()
                }
                line.isBlank() -> {
                    flushCurrent()
                }
            }
        }
        flushCurrent()
        return items
    }

    private fun mapHostPathToGitPath(hostPath: String, repositoryRootPath: String): String {
        val repositoryWslInfo = parseWslUncPath(repositoryRootPath) ?: return hostPath
        val worktreeWslInfo = parseWslUncPath(hostPath) ?: return hostPath
        if (!repositoryWslInfo.distribution.equals(worktreeWslInfo.distribution, ignoreCase = true)) {
            return hostPath
        }
        return worktreeWslInfo.linuxPath
    }

    private fun mapGitPathToHostPath(gitPath: String, mainRepositoryWslInfo: WslUncPath?): String {
        val trimmedGitPath = gitPath.trim()
        if (trimmedGitPath.isBlank()) return trimmedGitPath
        if (trimmedGitPath.startsWith("/") && mainRepositoryWslInfo != null) {
            return linuxPathToWslUnc(trimmedGitPath, mainRepositoryWslInfo.distribution)
        }
        return trimmedGitPath
    }

    private fun parseWslUncPath(rawPath: String): WslUncPath? {
        val windowsStylePath = rawPath.trim().replace('/', '\\')
        if (windowsStylePath.isBlank()) return null
        val prefix = WSL_UNC_PREFIXES.firstOrNull { windowsStylePath.startsWith(it, ignoreCase = true) } ?: return null
        val withoutPrefix = windowsStylePath.substring(prefix.length)
        val segments = withoutPrefix.split('\\').filter { it.isNotBlank() }
        if (segments.isEmpty()) return null
        val distribution = segments.first()
        val linuxSegments = segments.drop(1)
        val linuxPath = if (linuxSegments.isEmpty()) "/" else "/${linuxSegments.joinToString("/")}"
        return WslUncPath(distribution = distribution, linuxPath = linuxPath)
    }

    private fun linuxPathToWslUnc(linuxPath: String, distribution: String): String {
        val normalizedLinuxPath = if (linuxPath.startsWith("/")) linuxPath else "/$linuxPath"
        val windowsTail = normalizedLinuxPath.replace('/', '\\')
        return "\\\\wsl.localhost\\$distribution$windowsTail"
    }

    private fun normalizePath(path: String): String {
        val trimmed = path.trim()
        if (trimmed.isBlank()) return trimmed
        return runCatching {
            Path.of(trimmed).toAbsolutePath().normalize().toString()
        }.getOrElse { trimmed }
    }

    private fun normalizePathKey(path: String): String {
        return normalizePath(path)
            .replace('\\', '/')
            .lowercase(Locale.ROOT)
    }

    companion object {
        private val BRANCH_TIMESTAMP_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
        private val PATH_TIMESTAMP_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
        private val SLUG_INVALID_CHARACTERS = Regex("[^a-z0-9]+")
        private val SLUG_SEPARATOR_RUNS = Regex("-{2,}")
        private val WSL_UNC_PREFIXES = listOf("\\\\wsl.localhost\\", "\\\\wsl$\\")

        fun resumeArgumentsForConfiguration(
            configuration: AgentSettingsState.AgentCliConfiguration,
        ): List<String>? {
            return when (executableName(configuration.binaryPath)) {
                "cursor-agent", "agent", "claude" -> listOf("--continue")
                "gemini" -> listOf("--resume")
                "codex" -> listOf("resume", "--last")
                else -> null
            }
        }

        private fun executableName(binaryPath: String): String {
            val normalizedPath = binaryPath.trim()
            if (normalizedPath.isBlank()) return ""
            val fileName = normalizedPath.substringAfterLast('/').substringAfterLast('\\')
            return fileName.substringBeforeLast('.').lowercase(Locale.ROOT)
        }
    }

    private data class ParsedWorktree(
        val hostPath: String,
        val gitPath: String,
        val branchFullName: String?,
        val isMain: Boolean,
        val isCurrent: Boolean,
    )

    private data class WslUncPath(
        val distribution: String,
        val linuxPath: String,
    )
}
