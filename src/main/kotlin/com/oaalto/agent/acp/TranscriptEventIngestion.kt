package com.oaalto.agent.acp

import com.agentclientprotocol.model.AvailableCommand
import com.agentclientprotocol.model.AvailableCommandInput
import com.agentclientprotocol.model.ContentBlock
import com.agentclientprotocol.model.SessionUpdate
import com.agentclientprotocol.model.ToolCallContent
import com.agentclientprotocol.model.ToolCallStatus
import com.oaalto.agent.acp.plan.PlanUpdateMapper
import com.oaalto.agent.acp.transcript.model.StructuredUpdate
import com.oaalto.agent.acp.transcript.model.TranscriptBodyPart
import com.oaalto.agent.acp.transcript.model.TranscriptFinalizePolicy

/**
 * Consolidated ACP transcript event ingestion.
 *
 * Single ingestion seam: ACP [SessionUpdate] events enter via [ingest],
 * ordered [StructuredUpdate] deltas exit. Prompt-edge finalize hooks live on
 * [TranscriptFinalizePolicy].
 *
 * - finalize-before-non-chunk policy via [TranscriptFinalizePolicy.finalizePrelude]
 *   (non-streaming events close any active agent stream before mapping),
 * - every `SessionUpdate` → `StructuredUpdate` variant mapping,
 * - text extraction and tool-call body rendering (absorbed from the former
 *   renderer/dispatcher split).
 */
@Suppress("TooManyFunctions")
internal object TranscriptEventIngestion {
    /**
     * Ingest a [SessionUpdate] and return the ordered [StructuredUpdate] list
     * to apply. Non-chunk updates finalize any active agent stream first
     * (finalize-before-non-chunk policy). Agent message chunks stream in
     * place without finalization.
     */
    fun ingest(update: SessionUpdate): List<StructuredUpdate> {
        if (update is SessionUpdate.AgentMessageChunk) {
            return listOfNotNull(mapAgentChunk(update))
        }
        return TranscriptFinalizePolicy.finalizePrelude(update) + mapUpdate(update)
    }

    // -- Mapping ------------------------------------------------------------

    private fun mapAgentChunk(update: SessionUpdate.AgentMessageChunk): StructuredUpdate? =
        extractText(update.content)
            ?.takeIf { it.isNotBlank() }
            ?.let { StructuredUpdate.AppendAgentText(it) }

    @Suppress("CyclomaticComplexMethod")
    fun mapUpdate(update: SessionUpdate): List<StructuredUpdate> {
        val result: List<StructuredUpdate> =
            when (update) {
                is SessionUpdate.AgentMessageChunk -> listOfNotNull(mapAgentChunk(update))
                is SessionUpdate.AgentThoughtChunk -> mapThoughtChunk(update)
                is SessionUpdate.UserMessageChunk -> mapUserChunk(update)
                is SessionUpdate.ToolCall -> listOf(mapToolCall(update))
                is SessionUpdate.ToolCallUpdate -> listOf(mapToolCallUpdate(update))
                is SessionUpdate.UsageUpdate -> listOf(mapUsageUpdate(update))
                is SessionUpdate.AvailableCommandsUpdate -> listOf(mapAvailableCommands(update))
                else -> emptyList()
            }
        if (result.isNotEmpty()) return result

        return handlePlanUpdates(update)
    }

    private fun handlePlanUpdates(update: SessionUpdate): List<StructuredUpdate> {
        val simpleName = update::class.java.simpleName
        return when (simpleName) {
            "PlanUpdate" -> listOfNotNull(PlanUpdateMapper.mapPlanUpdate(update))
            "PlanUpdateV2" -> listOfNotNull(PlanUpdateMapper.mapPlanUpdateV2(update))
            "PlanRemoved" -> listOfNotNull(PlanUpdateMapper.mapPlanRemoved(update))
            else -> emptyList()
        }
    }

    private fun mapAvailableCommands(
        update: SessionUpdate.AvailableCommandsUpdate,
    ): StructuredUpdate.AvailableCommands =
        StructuredUpdate.AvailableCommands(
            commands = update.availableCommands.map(::toSlashCommand),
        )

    private fun toSlashCommand(command: AvailableCommand): SlashCommand =
        SlashCommand(
            name = command.name,
            description = command.description,
            inputHint =
                when (val input = command.input) {
                    is AvailableCommandInput.Unstructured -> input.hint.takeIf { it.isNotBlank() }
                    else -> null
                },
        )

    private fun mapUsageUpdate(update: SessionUpdate.UsageUpdate): StructuredUpdate.Usage =
        StructuredUpdate.Usage(
            used = update.used,
            size = update.size,
            cost = update.cost,
        )

    private fun mapThoughtChunk(update: SessionUpdate.AgentThoughtChunk): List<StructuredUpdate> =
        listOfNotNull(
            extractText(update.content)?.takeIf { it.isNotBlank() }?.let {
                StructuredUpdate.AppendThought(it)
            },
        )

    private fun mapUserChunk(update: SessionUpdate.UserMessageChunk): List<StructuredUpdate> =
        listOfNotNull(
            extractText(update.content)?.takeIf { it.isNotBlank() }?.let {
                StructuredUpdate.AppendUserEcho(it)
            },
        )

    private fun mapToolCall(update: SessionUpdate.ToolCall): StructuredUpdate.StartOrUpdateToolCall =
        StructuredUpdate.StartOrUpdateToolCall(
            toolCallId = update.toolCallId.value,
            title = TranscriptRenderer.displayToolTitle(update.title, update.toolCallId.value),
            kind = update.kind,
            status = update.status,
            bodyParts = renderToolCallBodyParts(update.content, update.status),
        )

    private fun mapToolCallUpdate(update: SessionUpdate.ToolCallUpdate): StructuredUpdate.StartOrUpdateToolCall =
        StructuredUpdate.StartOrUpdateToolCall(
            toolCallId = update.toolCallId.value,
            title = TranscriptRenderer.displayToolTitle(update.title, update.toolCallId.value),
            kind = update.kind,
            status = update.status,
            bodyParts = renderToolCallBodyParts(update.content, update.status),
        )

    // -- Text extraction helpers (absorbed from TranscriptRenderer) -----------

    internal fun renderToolCallBodyParts(
        content: List<ToolCallContent>?,
        status: ToolCallStatus?,
    ): List<TranscriptBodyPart> = TranscriptToolCallContentRenderer.renderBodyParts(content, status)

    internal fun extractText(content: ContentBlock): String? =
        when (content) {
            is ContentBlock.Text -> content.text
            else -> null
        }
}
