package com.oaalto.agent.acp

import com.oaalto.agent.worktree.resume.LaunchResumePlan
import com.oaalto.agent.worktree.resume.SessionPicker

/**
 * Deep editor-facing seam for ACP session lifecycle.
 *
 * Hides transport, auth, session-resume orchestration, and prompt job management
 * behind four operations that model what the editor actually wants to do:
 * start, prompt, cancel, dispose.
 *
 * `start()` composes connect → bootstrap → resume orchestration in one call,
 * eliminating the race between connect and session open that the previous
 * eight-method surface exposed.
 */
interface AcpSessionController {
    suspend fun start(request: AcpSessionStartRequest): AcpSessionStartResult

    suspend fun prompt(text: String)

    suspend fun cancelPrompt()

    fun dispose()
}

data class AcpSessionStartRequest(
    val launchPlan: AcpLaunchPlan,
    val editorContext: AcpEditorContext,
    val resumePlan: LaunchResumePlan,
    val sessionPicker: SessionPicker,
    val worktreeRecordId: String? = null,
)

data class AcpSessionStartResult(
    val sessionId: String?,
    val statusMessage: String,
    val restoreTranscript: Boolean = false,
)
