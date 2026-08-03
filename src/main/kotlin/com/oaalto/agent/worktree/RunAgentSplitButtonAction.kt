package com.oaalto.agent.worktree

import com.intellij.icons.AllIcons
import com.intellij.ide.ActivityTracker
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
import com.oaalto.agent.AgentCliLog
import com.oaalto.agent.AgentCliSessionContext
import com.oaalto.agent.AgentVirtualFile
import com.oaalto.agent.settings.AgentConfigurationSelector
import com.oaalto.agent.settings.AgentSettingsConfigurable
import com.oaalto.agent.settings.AgentSettingsState
import com.oaalto.agent.settings.LaunchMode
import com.oaalto.agent.worktree.resume.ResumeCapability
import java.nio.file.Path

class RunAgentSplitButtonAction :
    SplitButtonAction(RunAgentSplitActionGroup()),
    DumbAware {
    override fun useDynamicSplitButton(): Boolean = false

    override fun getMainAction(e: AnActionEvent): AnAction = DEFAULT_CURRENT_PROJECT_ACTION

    internal fun defaultMainActionForTests(): AnAction = DEFAULT_CURRENT_PROJECT_ACTION

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
}

private class RunAgentSplitActionGroup :
    ActionGroup(),
    DumbAware {
    override fun getChildren(event: AnActionEvent?): Array<AnAction> {
        val project = event?.project
        val selectedConfiguration = project?.let { AgentConfigurationSelector.getSelectedConfiguration(it) }
        return when {
            project == null -> arrayOf(disabledAction("Open a project to run Agent"))
            selectedConfiguration == null ->
                arrayOf(
                    ManageAgentSettingsAction(),
                    disabledAction("No agent configuration available"),
                )
            else -> buildWorktreeChildren(project, selectedConfiguration)
        }
    }

    private fun buildWorktreeChildren(
        project: com.intellij.openapi.project.Project,
        selectedConfiguration: AgentSettingsState.AgentCliConfiguration,
    ): Array<AnAction> {
        val canResumeSessions = ResumeCapability.canResumeSessions(selectedConfiguration)
        val actions = mutableListOf<AnAction>()
        actions += DEFAULT_CURRENT_PROJECT_ACTION
        actions += RunAgentInNewWorktreeAction()
        actions += Separator.getInstance()

        val managedWorktrees = loadManagedWorktreeRecords(project, selectedConfiguration.id)
        if (managedWorktrees.isEmpty()) {
            actions += disabledAction("No agent worktrees yet")
        } else {
            managedWorktrees.forEach { managed ->
                val displayName = worktreeDisplayName(managed, selectedConfiguration)
                actions +=
                    OpenOrResumeWorktreeAction(
                        displayName = displayName,
                        worktreePath = managed.worktreePath,
                        configurationId = managed.configurationId,
                        configurationName = managed.configurationName,
                        resume = canResumeSessions,
                    )
                actions +=
                    DeleteWorktreeAction(
                        displayName = displayName,
                        recordId = managed.id,
                        configurationId = managed.configurationId,
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
        val project = event.project
        val selected = project?.let { AgentConfigurationSelector.getSelectedConfiguration(it) }
        event.presentation.text = "Run Agent"
        event.presentation.description = selected?.name?.let {
            "Run '$it' in the current project (default) or use the dropdown for worktree actions"
        } ?: "Run selected agent in the current project or in a Git worktree"
        event.presentation.icon = AllIcons.Actions.Execute
        event.presentation.isEnabledAndVisible = event.project != null
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    private fun disabledAction(label: String): AnAction =
        object : DumbAwareAction(label) {
            override fun actionPerformed(event: AnActionEvent) = Unit

            override fun update(event: AnActionEvent) {
                event.presentation.isEnabled = false
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

    private fun worktreeDisplayName(
        managed: AgentWorktreeStateService.ManagedWorktreeRecord,
        configuration: AgentSettingsState.AgentCliConfiguration,
    ): String {
        val folderName =
            kotlin
                .runCatching {
                    Path.of(managed.worktreePath).fileName?.toString()
                }.getOrNull()
                .orEmpty()
                .ifBlank { managed.worktreePath }
        val branchSuffix =
            if (managed.branchName.isBlank()) {
                ""
            } else {
                " (${managed.branchName})"
            }
        val sessionSuffix =
            if (
                LaunchMode.from(configuration.launchMode) == LaunchMode.ACP_CLIENT &&
                !managed.acpSessionId.isNullOrBlank()
            ) {
                " [ACP session]"
            } else {
                ""
            }
        return "$folderName$branchSuffix$sessionSuffix"
    }
}

private class RunAgentInCurrentProjectAction :
    DumbAwareAction(
        RUN_IN_CURRENT_PROJECT_TEXT,
        "Run the selected agent in the current project working directory",
        AllIcons.Actions.Execute,
    ) {
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val configuration = AgentConfigurationSelector.getSelectedConfiguration(project)
        when {
            configuration == null -> {
                Messages.showErrorDialog(project, "No agent configuration is available.", "Run Agent")
                ShowSettingsUtil.getInstance().showSettingsDialog(project, AgentSettingsConfigurable::class.java)
            }
            configuration.binaryPath.isBlank() -> {
                Messages.showErrorDialog(
                    project,
                    "The selected agent configuration has an empty binary path.",
                    "Run Agent",
                )
                ShowSettingsUtil.getInstance().showSettingsDialog(project, AgentSettingsConfigurable::class.java)
            }
            else ->
                FileEditorManager.getInstance(project).openFile(
                    AgentVirtualFile(configuration.id, configuration.name),
                    true,
                )
        }
    }
}

private val DEFAULT_CURRENT_PROJECT_ACTION: AnAction = RunAgentInCurrentProjectAction()
internal const val RUN_IN_CURRENT_PROJECT_TEXT: String = "Run in Current Project"

private class RunAgentInNewWorktreeAction :
    DumbAwareAction(
        "Run in New Worktree",
        "Create a new worktree, open it, and run the selected agent there",
        AllIcons.RunConfigurations.Compound,
    ) {
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val configuration = AgentConfigurationSelector.getSelectedConfiguration(project)
        when {
            configuration == null -> {
                Messages.showErrorDialog(project, "No agent configuration is available.", "Run Agent")
                ShowSettingsUtil.getInstance().showSettingsDialog(project, AgentSettingsConfigurable::class.java)
            }
            else -> launchNewWorktree(project, configuration)
        }
    }

    private fun launchNewWorktree(
        project: com.intellij.openapi.project.Project,
        configuration: AgentSettingsState.AgentCliConfiguration,
    ) {
        val worktreeService = AgentWorktreeService(project)
        val createdWorktree =
            worktreeService.createWorktree(configuration).getOrElse { throwable ->
                val reason = throwable.message ?: "Failed to create worktree."
                agentCliLog.error(
                    message = "Worktree create failed: $reason",
                    throwable = throwable,
                    context =
                        AgentCliSessionContext(
                            configId = configuration.id,
                            launchMode = LaunchMode.from(configuration.launchMode),
                        ),
                )
                Messages.showErrorDialog(
                    project,
                    reason,
                    "Run Agent",
                )
                return
            }

        val state = AgentWorktreeStateService.getInstance()
        val record =
            state.saveRecord(
                configurationId = configuration.id,
                configurationName = configuration.name,
                repositoryRootPath = createdWorktree.repositoryRootPath,
                worktreePath = createdWorktree.worktreePath,
                branchName = createdWorktree.branchName,
            )
        val openResult =
            WorktreePendingLaunchHandoff.scheduleLaunch(
                originatingProject = project,
                worktreePath = record.worktreePath,
                configurationId = record.configurationId,
                configurationName = record.configurationName,
                resume = false,
            )
        if (openResult.isFailure) {
            val throwable = openResult.exceptionOrNull()
            val reason = throwable?.message ?: "Failed to open worktree project."
            agentCliLog.error(
                message = "Worktree open failed: $reason",
                throwable = throwable,
                context =
                    AgentCliSessionContext(
                        configId = configuration.id,
                        launchMode = LaunchMode.from(configuration.launchMode),
                        worktreePath = record.worktreePath,
                    ),
            )
            Messages.showErrorDialog(
                project,
                reason,
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
        templatePresentation.description =
            if (resume) {
                "Open this worktree and continue the previous CLI session"
            } else {
                "Open this worktree and run the selected agent"
            }
        templatePresentation.icon = if (resume) AllIcons.Actions.Resume else AllIcons.Actions.Execute
    }

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val result =
            WorktreePendingLaunchHandoff.scheduleLaunch(
                originatingProject = project,
                worktreePath = worktreePath,
                configurationId = configurationId,
                configurationName = configurationName,
                resume = resume,
            )
        if (result.isFailure) {
            val throwable = result.exceptionOrNull()
            val reason = throwable?.message ?: "Failed to open worktree project."
            val launchMode =
                AgentSettingsState
                    .getInstance()
                    .getConfigurationById(configurationId)
                    ?.let { LaunchMode.from(it.launchMode) }
            agentCliLog.error(
                message = "Worktree open failed: $reason",
                throwable = throwable,
                context =
                    AgentCliSessionContext(
                        configId = configurationId,
                        launchMode = launchMode,
                        worktreePath = worktreePath,
                    ),
            )
            Messages.showErrorDialog(
                project,
                reason,
                "Run Agent",
            )
        }
    }
}

private class DeleteWorktreeAction(
    displayName: String,
    private val recordId: String,
    private val configurationId: String,
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
        val confirm =
            Messages.showYesNoDialog(
                project,
                "Delete agent worktree?\n$worktreePath",
                "Run Agent",
                Messages.getQuestionIcon(),
            )
        if (confirm != Messages.YES) return

        val service = AgentWorktreeService(project)
        val result = service.deleteWorktree(worktreePath)
        if (result.isFailure) {
            val throwable = result.exceptionOrNull()
            val reason = throwable?.message ?: "Failed to delete worktree."
            agentCliLog.error(
                message = "Worktree delete failed: $reason",
                throwable = throwable,
                context =
                    AgentCliSessionContext(
                        configId = configurationId,
                        worktreePath = worktreePath,
                    ),
            )
            Messages.showErrorDialog(
                project,
                reason,
                "Run Agent",
            )
        } else {
            val state = AgentWorktreeStateService.getInstance()
            val markedById = state.markDeletedById(recordId)
            if (!markedById) {
                log.warn(
                    "Worktree '$worktreePath' deleted successfully, but managed record " +
                        "'$recordId' was missing; marking by path.",
                    context =
                        AgentCliSessionContext(
                            configId = configurationId,
                            worktreePath = worktreePath,
                        ),
                )
                state.markDeleted(worktreePath)
            }
            // Force toolbar/action-group refresh so removed worktrees disappear immediately.
            ActivityTracker.getInstance().inc()
        }
    }

    companion object {
        private val log = AgentCliLog.getInstance(DeleteWorktreeAction::class.java)
    }
}

private val agentCliLog = AgentCliLog.getInstance(RunAgentSplitButtonAction::class.java)

private class ManageAgentSettingsAction :
    DumbAwareAction(
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
