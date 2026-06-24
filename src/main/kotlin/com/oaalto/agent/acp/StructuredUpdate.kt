package com.oaalto.agent.acp

import com.agentclientprotocol.model.Cost
import com.agentclientprotocol.model.ToolCallStatus
import com.agentclientprotocol.model.ToolKind

/** Normalized transcript delta produced from ACP session events. */
sealed class StructuredUpdate {
    data class AppendAgentText(
        val text: String,
    ) : StructuredUpdate()

    data object FinalizeAgentStream : StructuredUpdate()

    data class AppendUserEcho(
        val text: String,
    ) : StructuredUpdate()

    data class AppendThought(
        val text: String,
    ) : StructuredUpdate()

    data class AppendPlainLine(
        val line: String,
        val isUserPrompt: Boolean = false,
    ) : StructuredUpdate()

    data class AppendError(
        val message: String,
    ) : StructuredUpdate()

    data class AppendAuthFailure(
        val message: String,
    ) : StructuredUpdate()

    data class StartOrUpdateToolCall(
        val toolCallId: String,
        val title: String,
        val kind: ToolKind?,
        val status: ToolCallStatus?,
        val bodyParts: List<TranscriptBodyPart> = emptyList(),
    ) : StructuredUpdate()

    /** Token usage and optional cost from an ACP UsageUpdate event. */
    data class Usage(
        val used: Long,
        val size: Long,
        val cost: Cost?,
    ) : StructuredUpdate()

    /**
     * Start or update a plan panel with the given ID and entries.
     * If a plan with this ID exists, it is replaced in-place.
     */
    data class StartOrUpdatePlan(
        val planId: String,
        val entries: List<PlanEntry>,
        val variant: PlanVariant = PlanVariant.Items,
        val dismissed: Boolean = false,
    ) : StructuredUpdate()

    /**
     * Remove a plan panel with the given ID.
     * The plan panel is marked as dismissed in the transcript.
     */
    data class RemovePlan(
        val planId: String,
        val dismissed: Boolean = true,
    ) : StructuredUpdate()

    /** Agent slash commands advertised for the current session (not rendered in transcript). */
    data class AvailableCommands(
        val commands: List<SlashCommand>,
    ) : StructuredUpdate()
}

/** Plan entry with content, status, and priority. */
data class PlanEntry(
    val content: String,
    val status: PlanEntryStatus,
    val priority: PlanEntryPriority,
)

/** Status of a plan entry. */
enum class PlanEntryStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
}

/** Priority of a plan entry. */
enum class PlanEntryPriority {
    HIGH,
    MEDIUM,
    LOW,
}

/** Variant types for plan updates (mirrors PlanUpdateV2 variants). */
sealed class PlanVariant {
    /** Standard plan with list of entries. */
    data object Items : PlanVariant()

    /** External plan file reference. */
    data class File(
        val uri: String,
    ) : PlanVariant()

    /** Markdown content for the plan. */
    data class Markdown(
        val content: String,
    ) : PlanVariant()
}
