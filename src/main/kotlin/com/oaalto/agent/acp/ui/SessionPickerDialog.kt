package com.oaalto.agent.acp.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import com.oaalto.agent.worktree.resume.SessionSummary
import java.awt.BorderLayout
import javax.swing.DefaultListModel
import javax.swing.JComponent
import javax.swing.JPanel

class SessionPickerDialog(
    project: Project,
    private val sessions: List<SessionSummary>,
) : DialogWrapper(project) {
    private val listModel = DefaultListModel<SessionListEntry>()
    private val sessionList = JBList(listModel)

    init {
        title = "Resume ACP Session"
        sessions.forEach { session ->
            listModel.addElement(SessionListEntry.Session(session))
        }
        listModel.addElement(SessionListEntry.StartFresh)
        sessionList.selectedIndex = 0
        sessionList.cellRenderer =
            javax.swing.DefaultListCellRenderer().apply {
                // use default rendering with toString on entries
            }
        init()
    }

    val selectedSessionId: String?
        get() {
            val selected = sessionList.selectedValue ?: return null
            return when (selected) {
                is SessionListEntry.Session -> selected.summary.sessionId
                SessionListEntry.StartFresh -> null
            }
        }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel(BorderLayout())
        panel.border = JBUI.Borders.empty(AcpUiMetrics.HORIZONTAL_INSET)
        panel.add(JBScrollPane(sessionList), BorderLayout.CENTER)
        return panel
    }

    private sealed class SessionListEntry {
        data class Session(
            val summary: SessionSummary,
        ) : SessionListEntry() {
            override fun toString(): String {
                val title = summary.title?.trim().orEmpty()
                val cwd = summary.cwd.trim()
                return when {
                    title.isNotBlank() && cwd.isNotBlank() -> "$title ($cwd)"
                    title.isNotBlank() -> title
                    cwd.isNotBlank() -> cwd
                    else -> summary.sessionId
                }
            }
        }

        data object StartFresh : SessionListEntry() {
            private const val LABEL = "Start a new session"

            override fun toString(): String = LABEL
        }
    }

    companion object {
        fun show(
            project: Project,
            sessions: List<SessionSummary>,
        ): String? {
            if (sessions.isEmpty()) {
                return null
            }
            val dialog = SessionPickerDialog(project, sessions)
            return when {
                !dialog.showAndGet() -> null
                else -> dialog.selectedSessionId
            }
        }
    }
}
