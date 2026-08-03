package com.oaalto.agent.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.Messages
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.components.JBLabel
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.JBUI
import com.oaalto.agent.AgentCliLog
import java.awt.Cursor
import java.awt.Desktop
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID
import javax.swing.DefaultCellEditor
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.ListSelectionModel

private fun createSettingsHintLabel(text: String): JBLabel =
    JBLabel("<html>$text</html>").apply {
        foreground = JBUI.CurrentTheme.Label.disabledForeground()
        setAllowAutoWrapping(true)
    }

internal object AgentSettingsUiFactory {
    fun createConfigurationTable(model: AgentConfigsTableModel): JBTable =
        JBTable(model).apply {
            selectionModel.selectionMode = ListSelectionModel.SINGLE_SELECTION
            fillsViewportHeight = true
            columnModel.getColumn(AgentConfigsTableColumns.CONFIG_TABLE_LAUNCH_MODE_COLUMN).cellEditor =
                DefaultCellEditor(
                    ComboBox(
                        LaunchMode.displayLabels().toTypedArray(),
                    ),
                )
            columnModel.getColumn(AgentConfigsTableColumns.CONFIG_TABLE_EXECUTION_TARGET_COLUMN).cellEditor =
                DefaultCellEditor(
                    ComboBox(
                        AgentSettingsState.ExecutionTarget.entries
                            .map { it.name }
                            .toTypedArray(),
                    ),
                )
        }

    fun createEnvironmentVariablesEditor(): EnvironmentVariablesEditor {
        val envModel = EnvironmentVariablesTableModel()
        val envTable =
            JBTable(envModel).apply {
                selectionModel.selectionMode = ListSelectionModel.SINGLE_SELECTION
                fillsViewportHeight = true
                rowHeight = JBUI.scale(AgentConfigsTableColumns.ENV_TABLE_ROW_HEIGHT)
            }
        val envToolbar =
            ToolbarDecorator
                .createDecorator(envTable)
                .setAddAction { _ ->
                    envModel.addRow("", "")
                    val index = envModel.rowCount - 1
                    if (index >= 0) {
                        envTable.selectionModel.setSelectionInterval(index, index)
                    }
                }.setRemoveAction { _ ->
                    val selected = envTable.selectedRow
                    if (selected >= 0) {
                        envModel.removeRow(selected)
                    }
                }
        return EnvironmentVariablesEditor(
            model = envModel,
            table = envTable,
            panel = envToolbar.createPanel(),
        )
    }

    fun createDetailPanelHints(): DetailPanelHints {
        val mcpScopeHint =
            createSettingsHintLabel("MCP toggles apply only when Launch Mode is ACP. PTY Passthrough ignores them.")
        val mcpPluginsHint =
            createSettingsHintLabel(
                "IntelliJ MCP requires JetBrains AI Assistant or the MCP Server plugin (Tools | MCP Server).",
            )
        val envHint =
            createSettingsHintLabel(
                "Environment variables apply only when Launch Mode is ACP. PTY Passthrough ignores them.",
            )
        return DetailPanelHints(
            mcpScopeHint = mcpScopeHint,
            mcpPluginsHint = mcpPluginsHint,
            envHint = envHint,
        )
    }

    fun createDetailPanel(
        ideaMcp: JCheckBox,
        customMcp: JCheckBox,
        environmentPanel: JComponent,
        hints: DetailPanelHints,
    ): JPanel =
        JPanel(GridBagLayout()).apply {
            border = JBUI.Borders.emptyTop(AgentConfigsTableColumns.DETAIL_PANEL_TOP_INSET)
            var row = 0

            fun addRow(
                component: JComponent,
                gridFill: Int = GridBagConstraints.HORIZONTAL,
            ) {
                add(
                    component,
                    GridBagConstraints().apply {
                        gridx = 0
                        gridy = row++
                        weightx = 1.0
                        fill = gridFill
                        anchor = GridBagConstraints.WEST
                    },
                )
            }
            addRow(hints.mcpScopeHint)
            addRow(hints.mcpPluginsHint)
            addRow(ideaMcp)
            addRow(customMcp)
            addRow(hints.envHint)
            addRow(JBLabel("Environment variables"))
            addRow(environmentPanel, GridBagConstraints.BOTH)
        }

    fun createObservabilityPanel(): ObservationHelpPanel {
        val projectManager = ProjectManager.getInstance()
        val focusedProject = projectManager.openProjects.firstOrNull()
        val hasBasePath = focusedProject?.basePath?.isNotBlank() == true

        val sessionDiagnosticsHint = createSessionDiagnosticsHint()
        val transcriptHint = createTranscriptHint(focusedProject, hasBasePath)
        val openButton = createOpenTranscriptButton(focusedProject, hasBasePath)
        val panel = buildObservabilityPanel(sessionDiagnosticsHint, transcriptHint, openButton)

        return ObservationHelpPanel(
            panel = panel,
            hasBasePath = hasBasePath,
        )
    }

    private fun createSessionDiagnosticsHint(): JBLabel =
        createSettingsHintLabel(
            "Session diagnostics are written to the IDE log file idea.log. " +
                "Open the log folder via Help > Show Log in Explorer " +
                "(macOS: Show Log in Finder). " +
                "Grep idea.log for [agent-cli:...] to match transcript errors to log detail.",
        )

    private fun createTranscriptHint(
        focusedProject: com.intellij.openapi.project.Project?,
        hasBasePath: Boolean,
    ): JBLabel {
        val projectDisplayName = focusedProject?.name
        return createSettingsHintLabel(
            buildString {
                append("Session transcript file is ACP Client only. ")
                append("Files live at .idea/agent-cli/transcripts/ (workspace-local, not VCS). ")
                append("Terminal (PTY Passthrough) does not create plugin transcript files — ")
                append("use terminal scrollback or the external agent CLI. ")
                if (hasBasePath) {
                    append("For the focused project ").append(projectDisplayName).append(".")
                } else {
                    append("Open a project to view the transcript folder.")
                }
            },
        )
    }

    private fun createOpenTranscriptButton(
        focusedProject: com.intellij.openapi.project.Project?,
        hasBasePath: Boolean,
    ): JBLabel {
        val transcriptDirectory =
            if (hasBasePath && focusedProject != null) {
                val basePath = focusedProject.basePath.orEmpty()
                com.oaalto.agent.acp.TranscriptFileStore
                    .resolveTranscriptsDirectory(basePath)
            } else {
                null
            }

        return JBLabel("Open transcript folder").apply {
            cursor =
                if (hasBasePath) {
                    Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                } else {
                    Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR)
                }
            addMouseListener(
                object : java.awt.event.MouseAdapter() {
                    override fun mouseClicked(e: java.awt.event.MouseEvent) {
                        if (!hasBasePath || transcriptDirectory == null) return
                        openTranscriptFolder(
                            transcriptDirectory = transcriptDirectory,
                            project = focusedProject,
                        )
                    }
                },
            )
        }
    }

    private fun buildObservabilityPanel(
        sessionDiagnosticsHint: JBLabel,
        transcriptHint: JBLabel,
        openButton: JBLabel,
    ): JPanel =
        JPanel(GridBagLayout()).apply {
            border = JBUI.Borders.emptyTop(AgentConfigsTableColumns.DETAIL_PANEL_TOP_INSET)
            var row = 0

            fun addRow(
                component: JComponent,
                gridFill: Int = GridBagConstraints.HORIZONTAL,
            ) {
                add(
                    component,
                    GridBagConstraints().apply {
                        gridx = 0
                        gridy = row++
                        weightx = 1.0
                        fill = gridFill
                        anchor = GridBagConstraints.WEST
                    },
                )
            }

            addRow(sessionDiagnosticsHint)
            addRow(transcriptHint)
            addRow(openButton, GridBagConstraints.HORIZONTAL)
        }

    private fun openTranscriptFolder(
        transcriptDirectory: Path,
        project: com.intellij.openapi.project.Project?,
    ) {
        ApplicationManager.getApplication().invokeLater {
            try {
                Files.createDirectories(transcriptDirectory)
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().open(transcriptDirectory.toFile())
                }
            } catch (e: java.io.IOException) {
                AgentCliLog
                    .getInstance(AgentSettingsUiFactory::class.java)
                    .error(message = "Failed to open transcript folder: ${e.message}")
                Messages.showErrorDialog(
                    null as java.awt.Component?,
                    e.message ?: "Failed to open transcript folder",
                    "Open transcript folder failed",
                )
            }
        }
    }

    fun createConfigurationsToolbar(
        model: AgentConfigsTableModel,
        table: JBTable,
        onImport: () -> Unit,
        onExport: () -> Unit,
    ) = ToolbarDecorator
        .createDecorator(table)
        .setAddAction { _ ->
            model.addRow(
                AgentConfigRow(
                    id = UUID.randomUUID().toString(),
                    name = "Agent ${model.rowCount + 1}",
                    launchMode = LaunchMode.PTY_PASSTHROUGH.name,
                    executionTarget = AgentSettingsState.ExecutionTarget.LOCAL.name,
                    wslDistribution = "",
                    binaryPath = "",
                    useNodeShellWrapper = false,
                    arguments = "",
                    workingDirectory = "",
                    useIdeaMcp = false,
                    useCustomMcp = false,
                    environmentVariables = emptyMap(),
                    isDefault = model.rowCount == 0,
                ),
            )
            val index = model.rowCount - 1
            if (index >= 0) {
                table.selectionModel.setSelectionInterval(index, index)
            }
        }.setRemoveAction { _ ->
            val selected = table.selectedRow
            if (selected >= 0) {
                model.removeRow(selected)
            }
        }.addExtraAction(
            object : com.intellij.openapi.actionSystem.AnAction("Import from acp.json") {
                override fun actionPerformed(e: com.intellij.openapi.actionSystem.AnActionEvent) {
                    onImport()
                }
            },
        ).addExtraAction(
            object : com.intellij.openapi.actionSystem.AnAction("Export to acp.json") {
                override fun actionPerformed(e: com.intellij.openapi.actionSystem.AnActionEvent) {
                    onExport()
                }
            },
        )
}
