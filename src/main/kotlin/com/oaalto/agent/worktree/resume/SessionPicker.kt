package com.oaalto.agent.worktree.resume

/**
 * Port for the session picker UI.
 *
 * Implemented by [com.oaalto.agent.acp.ui.SessionPickerAdapter] which delegates to
 * [com.oaalto.agent.acp.ui.SessionPickerDialog]. Returns the selected session ID, or null
 * when the user chooses "Start fresh".
 *
 * The suspending signature allows EDT bridging to be encapsulated in the adapter.
 */
interface SessionPicker {
    suspend fun pickSession(candidates: List<SessionSummary>): String?
}
