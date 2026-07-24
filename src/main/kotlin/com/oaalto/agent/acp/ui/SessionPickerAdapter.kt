package com.oaalto.agent.acp.ui

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.oaalto.agent.worktree.resume.SessionPicker
import com.oaalto.agent.worktree.resume.SessionSummary
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Adapter that bridges [SessionPicker] to [SessionPickerDialog] on the EDT.
 *
 * The suspending [SessionPicker] interface allows Swing blocking to be encapsulated here,
 * keeping the orchestrator testable without IntelliJ UI.
 */
class SessionPickerAdapter(
    private val project: Project,
) : SessionPicker {
    override suspend fun pickSession(candidates: List<SessionSummary>): String? =
        suspendCancellableCoroutine { continuation ->
            ApplicationManager.getApplication().invokeLater {
                if (continuation.isActive) {
                    continuation.resume(SessionPickerDialog.show(project, candidates))
                }
            }
        }
}
