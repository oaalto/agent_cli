package com.oaalto.agent.worktree

import com.oaalto.agent.settings.AgentSettingsConfigurable
import com.oaalto.agent.settings.AgentSettingsState
import com.intellij.ide.ActivityTracker
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Separator
import com.intellij.openapi.actionSystem.SplitButtonAction
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.Messages
import com.oaalto.agent.AgentVirtualFile
import java.nio.file.Path

class RunAgentSplitButtonAction : SplitButtonAction(RunAgentSplitActionGroup()), DumbAware {
    override fun useDynamicSplitButton(): Boolean = false

    override fun getMainAction(e: AnActionEvent): AnAction = DEFAULT_CURRENT_PROJECT_ACTION

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
}

private class RunAgentSplitActionGroup : ActionGroup(), DumbAware {
    override fun getChildren(event: AnActionEvent?): Array<AnAction> {
        val project = event?.project
        if (project == null) {
            return arrayOf(disabledAction("Open a project to run Agent"))
        }

        val settings = AgentSettingsState.getInstance()
        val selectedConfiguration = settings.getSelectedConfiguration()
        if (selectedConfiguration == null) {
            return arrayOf(
                ManageAgentSettingsAction(),
                disabledAction("No agent configuration available"),
            )
        }

        val canResumeSessions = AgentWorktreeService.resumeArgumentsForConfiguration(selectedConfiguration) != null
        val actions = mutableListOf<AnAction>()
        actions += DEFAULT_CURRENT_PROJECT_ACTION
        actions += RunAgentInNewWorktreeAction()
        actions += Separator.getInstance()

        val managedWorktrees = loadManagedWorktreeRecords(project, selectedConfiguration.id)
        if (managedWorktrees.isEmpty()) {
            actions += disabledAction("No agent worktrees yet")
        } else {
            managedWorktrees.forEach { managed ->
                val displayName = worktreeDisplayName(managed.worktreePath, managed.branchName)
                actions += OpenOrResumeWorktreeAction(
                    displayName = displayName,
                    worktreePath = managed.worktreePath,
                    configurationId = managed.configurationId,
                    configurationName = managed.configurationName,
                    resume = canResumeSessions,
                )
                actions += DeleteWorktreeAction(
                    displayName = displayName,
                    recordId = managed.id,
                    worktreePath = managed.worktreePath,
                )
                actions += Separator.getInstance()
            }
            if (actions.lastOrNull() is Separator) {
                actions.removeLast()
            }
        }

        actions += Separator.getInstance()
        actions += ManageAgentSettingsAction()
        return actions.toTypedArray()
    }

    override fun update(event: AnActionEvent) {
        val settings = AgentSettingsState.getInstance()
        val selected = settings.getSelectedConfiguration()
        event.presentation.text = "Run Agent"
        event.presentation.description = selected?.name?.let {
            "Run '$it' in the current project (default) or use the dropdown for worktree actions"
        } ?: "Run selected agent in the current project or in a Git worktree"
        event.presentation.icon = AllIcons.Actions.Execute
        event.presentation.isEnabledAndVisible = event.project != null
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    private fun disabledAction(label: String): AnAction {
        return object : DumbAwareAction(label) {
            override fun actionPerformed(event: AnActionEvent) = Unit

            override fun update(event: AnActionEvent) {
                event.presentation.isEnabled = false
            }
        }
    }

    private fun loadManagedWorktreeRecords(
        project: com.intellij.openapi.project.Project,
        configurationId: String,
    ): List<AgentWorktreeStateService.ManagedWorktreeRecord> {
        val state = AgentWorktreeStateService.getInstance()
        state.pruneMissingWorktreesForConfiguration(configurationId)
        val basePath = project.basePath?.trim().orEmpty()
        if (basePath.isBlank()) {
            return state.getActiveRecordsForConfiguration(configurationId)
        }
        val scoped = state.getActiveRecords(configurationId = configurationId, repositoryRootPath = basePath)
        return if (scoped.isNotEmpty()) scoped else state.getActiveRecordsForConfiguration(configurationId)
    }

    private fun worktreeDisplayName(worktreePath: String, branchName: String): String {
        val folderName = kotlin.runCatching {
            Path.of(worktreePath).fileName?.toString()
        }.getOrNull().orEmpty().ifBlank { worktreePath }
        return if (branchName.isBlank()) folderName else "$folderName ($branchName)"
    }
}

private class RunAgentInCurrentProjectAction : DumbAwareAction(
    "Run in Current Project",
    "Run the selected agent in the current project working directory",
    AllIcons.Actions.Execute,
) {
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val settings = AgentSettingsState.getInstance()
        val configuration = settings.getSelectedConfiguration()
        if (configuration == null) {
            Messages.showErrorDialog(project, "No agent configuration is available.", "Run Agent")
            ShowSettingsUtil.getInstance().showSettingsDialog(project, AgentSettingsConfigurable::class.java)
            return
        }
        if (configuration.binaryPath.isBlank()) {
            Messages.showErrorDialog(project, "The selected agent configuration has an empty binary path.", "Run Agent")
            ShowSettingsUtil.getInstance().showSettingsDialog(project, AgentSettingsConfigurable::class.java)
            return
        }
        FileEditorManager.getInstance(project).openFile(
            AgentVirtualFile(configuration.id, configuration.name),
            true,
        )
    }
}
private val DEFAULT_CURRENT_PROJECT_ACTION: AnAction = RunAgentInCurrentProjectAction()

private class RunAgentInNewWorktreeAction : DumbAwareAction(
    "Run in New Worktree",
    "Create a new worktree, open it, and run the selected agent there",
    AllIcons.Nodes.Folder,
) {
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val settings = AgentSettingsState.getInstance()
        val configuration = settings.getSelectedConfiguration()
        if (configuration == null) {
            Messages.showErrorDialog(project, "No agent configuration is available.", "Run Agent")
            ShowSettingsUtil.getInstance().showSettingsDialog(project, AgentSettingsConfigurable::class.java)
            return
        }

        val worktreeService = AgentWorktreeService(project)
        val created = worktreeService.createWorktree(configuration)
        if (created.isFailure) {
            Messages.showErrorDialog(
                project,
                created.exceptionOrNull()?.message ?: "Failed to create worktree.",
                "Run Agent",
            )
            return
        }
        val createdWorktree = created.getOrNull() ?: return

        val state = AgentWorktreeStateService.getInstance()
        val record = state.saveRecord(
            configurationId = configuration.id,
            configurationName = configuration.name,
            repositoryRootPath = createdWorktree.repositoryRootPath,
            worktreePath = createdWorktree.worktreePath,
            branchName = createdWorktree.branchName,
        )
        state.enqueuePendingLaunch(
            worktreePath = record.worktreePath,
            configurationId = record.configurationId,
            configurationName = record.configurationName,
            resume = false,
        )

        val openResult = worktreeService.openWorktreeProject(record.worktreePath)
        if (openResult.isFailure) {
            state.consumePendingLaunch(record.worktreePath)
            Messages.showErrorDialog(
                project,
                openResult.exceptionOrNull()?.message ?: "Failed to open worktree project.",
                "Run Agent",
            )
        }
    }
}

private class OpenOrResumeWorktreeAction(
    displayName: String,
    private val worktreePath: String,
    private val configurationId: String,
    private val configurationName: String,
    private val resume: Boolean,
) : DumbAwareAction(
    if (resume) "Resume $displayName" else "Open $displayName",
) {
    init {
        templatePresentation.description = if (resume) {
            "Open this worktree and continue the previous CLI session"
        } else {
            "Open this worktree and run the selected agent"
        }
        templatePresentation.icon = if (resume) AllIcons.Actions.Resume else AllIcons.Actions.Execute
    }

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val worktreeService = AgentWorktreeService(project)
        val state = AgentWorktreeStateService.getInstance()
        state.touch(worktreePath)
        state.enqueuePendingLaunch(
            worktreePath = worktreePath,
            configurationId = configurationId,
            configurationName = configurationName,
            resume = resume,
        )
        val result = worktreeService.openWorktreeProject(worktreePath)
        if (result.isFailure) {
            state.consumePendingLaunch(worktreePath)
            Messages.showErrorDialog(
                project,
                result.exceptionOrNull()?.message ?: "Failed to open worktree project.",
                "Run Agent",
            )
        }
    }
}

private class DeleteWorktreeAction(
    displayName: String,
    private val recordId: String,
    private val worktreePath: String,
) : DumbAwareAction(
    "Delete $displayName",
) {
    init {
        templatePresentation.description = "Delete this agent worktree"
        templatePresentation.icon = AllIcons.General.Remove
    }

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val confirm = Messages.showYesNoDialog(
            project,
            "Delete agent worktree?\n$worktreePath",
            "Run Agent",
            Messages.getQuestionIcon(),
        )
        if (confirm != Messages.YES) return

        val service = AgentWorktreeService(project)
        val result = service.deleteWorktree(worktreePath)
        if (result.isFailure) {
            Messages.showErrorDialog(
                project,
                result.exceptionOrNull()?.message ?: "Failed to delete worktree.",
                "Run Agent",
            )
        } else {
            AgentWorktreeStateService.getInstance().markDeletedById(recordId)
            // Force toolbar/action-group refresh so removed worktrees disappear immediately.
            ActivityTracker.getInstance().inc()
        }
    }
}

private class ManageAgentSettingsAction : DumbAwareAction(
    "Manage Agents...",
) {
    init {
        templatePresentation.description = "Open Agent CLI settings"
        templatePresentation.icon = AllIcons.General.Settings
    }

    override fun actionPerformed(event: AnActionEvent) {
        ShowSettingsUtil.getInstance().showSettingsDialog(event.project, AgentSettingsConfigurable::class.java)
    }
}
