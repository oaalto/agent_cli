package com.oaalto.agent.worktree

import com.intellij.ide.impl.OpenProjectTask
import com.intellij.ide.impl.ProjectUtil
import com.intellij.openapi.project.Project
import com.oaalto.agent.settings.AgentSettingsState
import com.oaalto.agent.worktree.resume.PtyResumeStrategy
import git4idea.repo.GitRepository
import java.nio.file.Files
import java.nio.file.Path

class AgentWorktreeService(
    private val project: Project,
) {
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

    fun createWorktree(configuration: AgentSettingsState.AgentCliConfiguration): Result<CreatedWorktree> =
        AgentWorktreeGitSupport.resolveSingleRepository(project).bind { repository ->
            repository.currentBranch
                ?.let { branch -> createWorktreeAtBranch(repository, configuration, branch.fullName) }
                ?: Result.failure(
                    IllegalStateException(
                        "Cannot create an agent worktree while repository is in detached HEAD state.",
                    ),
                )
        }

    fun listManagedWorktrees(configurationId: String): Result<List<ManagedWorktree>> =
        AgentWorktreeGitSupport.resolveSingleRepository(project).bind { repository ->
            AgentWorktreeGitSupport.listWorktrees(project, repository).map { worktrees ->
                val byPath = worktrees.associateBy { AgentWorktreePathMapper.normalizePathKey(it.hostPath) }
                val records =
                    AgentWorktreeStateService.getInstance().getActiveRecords(
                        configurationId = configurationId,
                        repositoryRootPath = repository.root.path,
                    )
                records.mapNotNull { record ->
                    val tree =
                        byPath[AgentWorktreePathMapper.normalizePathKey(record.worktreePath)]
                            ?: return@mapNotNull null
                    ManagedWorktree(
                        record = record,
                        branchName = tree.branchFullName,
                        isMain = tree.isMain,
                        isCurrent = tree.isCurrent,
                    )
                }
            }
        }

    fun deleteWorktree(worktreePath: String): Result<Unit> =
        AgentWorktreeGitSupport.resolveSingleRepository(project).bind { repository ->
            AgentWorktreeGitSupport.listWorktrees(project, repository).bind { existingTrees ->
                val tree =
                    existingTrees.firstOrNull {
                        AgentWorktreePathMapper.normalizePathKey(it.hostPath) ==
                            AgentWorktreePathMapper.normalizePathKey(worktreePath)
                    }
                when {
                    tree == null -> {
                        AgentWorktreeStateService.getInstance().markDeleted(worktreePath)
                        Result.success(Unit)
                    }
                    tree.isMain -> Result.failure(IllegalStateException("Cannot delete the main worktree."))
                    tree.isCurrent ->
                        Result.failure(
                            IllegalStateException("Cannot delete the currently opened worktree."),
                        )
                    else ->
                        AgentWorktreeGitSupport
                            .runGitWorktreeCommand(
                                project = project,
                                repository = repository,
                                arguments = listOf("remove", tree.gitPath),
                            ).bind { result ->
                                when {
                                    !result.success() ->
                                        Result.failure(
                                            IllegalStateException(
                                                "Failed to delete worktree:\n${result.getErrorOutputAsJoinedString()}",
                                            ),
                                        )
                                    else -> {
                                        AgentWorktreeStateService.getInstance().markDeleted(worktreePath)
                                        Result.success(Unit)
                                    }
                                }
                            }
                }
            }
        }

    fun openWorktreeProject(worktreePath: String): Result<Unit> {
        val normalizedPath = AgentWorktreePathMapper.normalizePath(worktreePath)
        if (!Files.isDirectory(Path.of(normalizedPath))) {
            return Result.failure(IllegalStateException("Worktree directory does not exist:\n$normalizedPath"))
        }
        return runCatching {
            val openedProject = ProjectUtil.openOrImport(Path.of(normalizedPath), OpenProjectTask())
            if (openedProject == null) {
                error("IDE refused to open worktree:\n$normalizedPath")
            }
        }.recoverCatching { throwable ->
            error("Failed to open worktree:\n${throwable.message ?: throwable.javaClass.simpleName}")
        }
    }

    private fun createWorktreeAtBranch(
        repository: GitRepository,
        configuration: AgentSettingsState.AgentCliConfiguration,
        sourceBranchFullName: String,
    ): Result<CreatedWorktree> {
        val repoRootPath = repository.root.path
        return AgentWorktreeGitSupport.generateWorktreePath(repoRootPath, configuration.name).bind { worktreePath ->
            val branchName = AgentWorktreeGitSupport.buildBranchName(configuration.name)
            AgentWorktreeGitSupport.createFilePath(worktreePath).bind { filePath ->
                val gitWorktreePath =
                    AgentWorktreePathMapper.mapHostPathToGitPath(
                        hostPath = filePath.path,
                        repositoryRootPath = repoRootPath,
                    )
                AgentWorktreeGitSupport
                    .runGitWorktreeCommand(
                        project = project,
                        repository = repository,
                        arguments =
                            listOf(
                                "add",
                                "-b",
                                branchName,
                                gitWorktreePath,
                                sourceBranchFullName,
                            ),
                    ).bind { result ->
                        when {
                            !result.success() ->
                                Result.failure(
                                    IllegalStateException(
                                        "Failed to create worktree at:\n$worktreePath\n" +
                                            "(Git path: $gitWorktreePath)\n\n${result.getErrorOutputAsJoinedString()}",
                                    ),
                                )
                            !Files.isDirectory(Path.of(worktreePath)) ->
                                Result.failure(
                                    IllegalStateException(
                                        "Git reported success, but the worktree directory was not found:\n$worktreePath",
                                    ),
                                )
                            else ->
                                Result.success(
                                    CreatedWorktree(
                                        repositoryRootPath = repoRootPath,
                                        worktreePath = worktreePath,
                                        branchName = branchName,
                                    ),
                                )
                        }
                    }
            }
        }
    }

    companion object {
        fun resumeArgumentsForConfiguration(configuration: AgentSettingsState.AgentCliConfiguration): List<String>? =
            PtyResumeStrategy.baseResumeArguments(configuration)
    }
}

internal data class ParsedWorktree(
    val hostPath: String,
    val gitPath: String,
    val branchFullName: String?,
    val isMain: Boolean,
    val isCurrent: Boolean,
)

internal fun <T, R> Result<T>.bind(transform: (T) -> Result<R>): Result<R> =
    fold(
        onSuccess = transform,
        onFailure = { Result.failure(it) },
    )
