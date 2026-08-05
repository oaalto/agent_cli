package com.oaalto.agent.acp

import com.agentclientprotocol.model.SessionUpdate

/**
 * Orchestration-layer gate for [StructuredUpdate.FinalizeAgentStream] emission.
 *
 * **Allowed direct construction** (enforced by [FinalizeAgentStreamConstructionTest]):
 * this module; [StructuredUpdate] definition; [TranscriptModel.apply] when-branch;
 * [TranscriptViewController.finalizeAgentStream] thin `apply` passthrough; test/harness fixtures.
 * Production ingestion, executor, and editor paths must delegate here.
 *
 * **Invariants**
 * 1. Non-chunk [SessionUpdate] finalizes before the mapped update applies.
 * 2. New prompt finalizes before dispatch (closes prior agent stream).
 * 3. Prompt flow end finalizes at least once even without `PromptResponseEvent`.
 * 4. Failure/cancel/interrupt finalizes before idle/error.
 * 5. Redundant finalize is acceptable — [TranscriptModel.finalizeAgentStream] no-ops
 *    when no streaming block exists.
 */
internal object TranscriptFinalizePolicy {
    /** Emit finalize before mapping a non-chunk [SessionUpdate]. */
    fun finalizePrelude(update: SessionUpdate): List<StructuredUpdate> =
        if (update is SessionUpdate.AgentMessageChunk) {
            emptyList()
        } else {
            listOf(StructuredUpdate.FinalizeAgentStream)
        }

    /** Finalize before dispatching user echo / starting a new prompt job. */
    fun onPromptStarting(): List<StructuredUpdate> = listOf(StructuredUpdate.FinalizeAgentStream)

    /** Finalize on transport `PromptResponseEvent`. */
    fun onPromptResponse(): List<StructuredUpdate> = listOf(StructuredUpdate.FinalizeAgentStream)

    /**
     * Finalize when prompt flow completes after event collection.
     *
     * Covers transports that omit `PromptResponseEvent`; redundant finalize with
     * [onPromptResponse] is intentional and idempotent at the model layer.
     */
    fun onPromptFlowCompleted(): List<StructuredUpdate> = listOf(StructuredUpdate.FinalizeAgentStream)

    /** Finalize before surfacing prompt failure to the user. */
    fun onPromptFailed(): List<StructuredUpdate> = listOf(StructuredUpdate.FinalizeAgentStream)

    /** Finalize before cancel/dispose returns the prompt to idle. */
    fun onPromptInterrupted(): List<StructuredUpdate> = listOf(StructuredUpdate.FinalizeAgentStream)
}
