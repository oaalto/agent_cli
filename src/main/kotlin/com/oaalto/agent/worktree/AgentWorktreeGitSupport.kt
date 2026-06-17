package com.oaalto.agent.worktree

import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.FilePath
import com.intellij.openapi.vcs.VcsException
import com.intellij.openapi.vcs.actions.VcsContextFactory
import git4idea.commands.Git
import git4idea.commands.GitCommand
import git4idea.commands.GitCommandResult
import git4idea.commands.GitLineHandler
import git4idea.repo.GitRepository
import git4idea.repo.GitRepositoryManager
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

internal object AgentWorktreeGitSupport {
    private val git: Git = Git.getInstance()
    private val branchTimestampFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
    private val pathTimestampFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
    private val slugInvalidCharacters = Regex("[^a-z0-9]+")
    private val slugSeparatorRuns = Regex("-{2,}")

    fun resolveSingleRepository(project: Project): Result<GitRepository> {
        val repositories = GitRepositoryManager.getInstance(project).repositories
        return when (repositories.size) {
            0 -> Result.failure(IllegalStateException("No Git repository was detected for this project."))
            1 -> Result.success(repositories.first())
            else ->
                Result.failure(
                    IllegalStateException(
                        "Agent worktrees currently support single-repository projects only. " +
                            "Found ${repositories.size} repositories.",
                    ),
                )
        }
    }

    fun generateWorktreePath(
        repositoryRootPath: String,
        configurationName: String,
    ): Result<String> =
        runCatching {
            val repositoryRoot = Path.of(repositoryRootPath).toAbsolutePath().normalize()
            val parent = repositoryRoot.parent ?: error("Repository path has no parent directory.")
            val containerName = "${repositoryRoot.fileName}-agent-worktrees"
            val container = parent.resolve(containerName)
            Files.createDirectories(container)
            val repositoryName =
                repositoryRoot.fileName
                    ?.toString()
                    .orEmpty()
                    .ifBlank { "project" }
            val suffix = buildPathSuffix(repositoryName, configurationName)
            container
                .resolve(suffix)
                .toAbsolutePath()
                .normalize()
                .toString()
        }.recoverCatching { throwable ->
            error("Failed to resolve worktree path: ${throwable.message ?: throwable.javaClass.simpleName}")
        }

    fun buildBranchName(configurationName: String): String {
        val timestamp = branchTimestampFormat.format(LocalDateTime.now())
        return "agent/${slug(configurationName)}/$timestamp"
    }

    fun createFilePath(path: String): Result<FilePath> =
        runCatching {
            VcsContextFactory.getInstance().createFilePath(path, true)
        }.recoverCatching { throwable ->
            error("Failed to create VCS file path: ${throwable.message ?: throwable.javaClass.simpleName}")
        }

    fun runGitWorktreeCommand(
        project: Project,
        repository: GitRepository,
        arguments: List<String>,
    ): Result<GitCommandResult> =
        runCatching {
            val handler = GitLineHandler(project, repository.root, GitCommand.WORKTREE)
            handler.addParameters(arguments)
            git.runCommand(handler)
        }.recoverCatching { throwable ->
            val message = (throwable as? VcsException)?.message ?: throwable.message ?: throwable.javaClass.simpleName
            error("Failed to execute `git worktree ${arguments.joinToString(" ")}`:\n$message")
        }

    fun listWorktrees(
        project: Project,
        repository: GitRepository,
    ): Result<List<ParsedWorktree>> =
        runGitWorktreeCommand(project, repository, listOf("list", "--porcelain")).bind { result ->
            when {
                !result.success() ->
                    Result.failure(
                        IllegalStateException("Failed to list worktrees:\n${result.getErrorOutputAsJoinedString()}"),
                    )
                else ->
                    Result.success(
                        parseWorktreeList(
                            outputLines = result.output,
                            mainRepositoryPath = AgentWorktreePathMapper.normalizePath(repository.root.path),
                            currentProjectPath = AgentWorktreePathMapper.normalizePath(project.basePath.orEmpty()),
                        ),
                    )
            }
        }

    private fun buildPathSuffix(
        repositoryName: String,
        configurationName: String,
    ): String {
        val timestamp = pathTimestampFormat.format(LocalDateTime.now())
        return "${slug(repositoryName)}-${slug(configurationName)}-$timestamp"
    }

    private fun slug(rawValue: String): String {
        val normalized = rawValue.trim().lowercase(Locale.ROOT)
        val slug =
            normalized
                .replace(slugInvalidCharacters, "-")
                .replace(slugSeparatorRuns, "-")
                .trim('-')
        return slug.ifBlank { "agent" }
    }

    private fun parseWorktreeList(
        outputLines: List<String>,
        mainRepositoryPath: String,
        currentProjectPath: String,
    ): List<ParsedWorktree> {
        val items = mutableListOf<ParsedWorktree>()
        var currentPath: String? = null
        var currentBranch: String? = null

        fun flushCurrent() {
            val gitPath = currentPath ?: return
            val hostPath =
                AgentWorktreePathMapper.mapGitPathToHostPath(
                    gitPath = gitPath,
                    mainRepositoryPath = mainRepositoryPath,
                )
            items +=
                ParsedWorktree(
                    hostPath = AgentWorktreePathMapper.normalizePath(hostPath),
                    gitPath = gitPath.trim(),
                    branchFullName = currentBranch,
                    isMain =
                        AgentWorktreePathMapper.normalizePathKey(hostPath) ==
                            AgentWorktreePathMapper.normalizePathKey(mainRepositoryPath),
                    isCurrent =
                        currentProjectPath.isNotBlank() &&
                            AgentWorktreePathMapper.normalizePathKey(hostPath) ==
                            AgentWorktreePathMapper.normalizePathKey(currentProjectPath),
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
}
