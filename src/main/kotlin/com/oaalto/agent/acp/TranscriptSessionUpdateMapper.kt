package com.oaalto.agent.acp

import com.agentclientprotocol.model.AvailableCommand
import com.agentclientprotocol.model.AvailableCommandInput
import com.agentclientprotocol.model.SessionUpdate
import com.oaalto.agent.acp.plan.PlanUpdateMapper

/** Maps ACP [SessionUpdate] events to normalized [StructuredUpdate] values. */
internal object TranscriptSessionUpdateMapper {
    fun mapAgentChunk(update: SessionUpdate.AgentMessageChunk): StructuredUpdate? =
        TranscriptRenderer
            .renderEventText(update)
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
            TranscriptRenderer.extractText(update.content)?.takeIf { it.isNotBlank() }?.let {
                StructuredUpdate.AppendThought(it)
            },
        )

    private fun mapUserChunk(update: SessionUpdate.UserMessageChunk): List<StructuredUpdate> =
        listOfNotNull(
            TranscriptRenderer.extractText(update.content)?.takeIf { it.isNotBlank() }?.let {
                StructuredUpdate.AppendUserEcho(it)
            },
        )

    private fun mapToolCall(update: SessionUpdate.ToolCall): StructuredUpdate.StartOrUpdateToolCall =
        StructuredUpdate.StartOrUpdateToolCall(
            toolCallId = update.toolCallId.value,
            title = update.title,
            kind = update.kind,
            status = update.status,
            bodyParts =
                TranscriptRenderer.renderToolCallBodyParts(
                    content = update.content,
                    status = update.status,
                ),
        )

    private fun mapToolCallUpdate(update: SessionUpdate.ToolCallUpdate): StructuredUpdate.StartOrUpdateToolCall =
        StructuredUpdate.StartOrUpdateToolCall(
            toolCallId = update.toolCallId.value,
            title = update.title ?: update.toolCallId.value,
            kind = update.kind,
            status = update.status,
            bodyParts =
                TranscriptRenderer.renderToolCallBodyParts(
                    content = update.content,
                    status = update.status,
                ),
        )
}
