package com.oaalto.agent.acp.permission

import com.agentclientprotocol.model.PermissionOption
import com.agentclientprotocol.model.PermissionOptionId
import com.agentclientprotocol.model.PermissionOptionKind
import com.agentclientprotocol.model.RequestPermissionOutcome
import com.agentclientprotocol.model.RequestPermissionResponse
import com.agentclientprotocol.model.SessionUpdate
import com.agentclientprotocol.model.ToolKind

class PermissionCoordinator(
    private val configurationId: String,
    private val memoryStore: PermissionMemoryStore,
    private val promptUi: PermissionPromptUi,
) {
    suspend fun requestSessionPermission(
        toolCall: SessionUpdate.ToolCallUpdate,
        permissions: List<PermissionOption>,
    ): RequestPermissionResponse {
        val toolKind = toolCall.kind ?: ToolKind.OTHER
        rememberedOutcome(toolKind)?.let { remembered ->
            return RequestPermissionResponse(mapRememberedToOutcome(remembered, permissions))
        }
        val title = toolCall.title ?: toolCall.toolCallId.value
        val options = permissions.ifEmpty { defaultPermissionOptions() }
        val outcome = promptUi.prompt(title, options)
        persistIfNeeded(toolKind, outcome, options)
        return RequestPermissionResponse(outcome)
    }

    suspend fun requestWritePermission(path: String): Boolean {
        val toolKind = ToolKind.EDIT
        rememberedOutcome(toolKind)?.let { remembered ->
            return remembered == PermissionMemoryOutcome.ALLOW_ALWAYS
        }
        val options = defaultPermissionOptions()
        val outcome = promptUi.prompt("Write file: $path", options)
        persistIfNeeded(toolKind, outcome, options)
        return outcome is RequestPermissionOutcome.Selected &&
            options.firstOrNull { it.optionId == outcome.optionId }?.kind?.isAllow() == true
    }

    private fun rememberedOutcome(toolKind: ToolKind): PermissionMemoryOutcome? =
        memoryStore.get(configurationId, toolKind)

    private fun persistIfNeeded(
        toolKind: ToolKind,
        outcome: RequestPermissionOutcome,
        options: List<PermissionOption>,
    ) {
        if (outcome !is RequestPermissionOutcome.Selected) return
        val selected = options.firstOrNull { it.optionId == outcome.optionId } ?: return
        when (selected.kind) {
            PermissionOptionKind.ALLOW_ALWAYS ->
                memoryStore.set(configurationId, toolKind, PermissionMemoryOutcome.ALLOW_ALWAYS)
            PermissionOptionKind.REJECT_ALWAYS ->
                memoryStore.set(configurationId, toolKind, PermissionMemoryOutcome.REJECT_ALWAYS)
            PermissionOptionKind.ALLOW_ONCE,
            PermissionOptionKind.REJECT_ONCE,
            -> Unit
        }
    }

    private fun mapRememberedToOutcome(
        remembered: PermissionMemoryOutcome,
        permissions: List<PermissionOption>,
    ): RequestPermissionOutcome {
        val targetKind =
            when (remembered) {
                PermissionMemoryOutcome.ALLOW_ALWAYS -> PermissionOptionKind.ALLOW_ALWAYS
                PermissionMemoryOutcome.REJECT_ALWAYS -> PermissionOptionKind.REJECT_ALWAYS
            }
        val option =
            permissions.firstOrNull { it.kind == targetKind }
                ?: permissions.firstOrNull { it.kind.name.contains(targetKind.name.substringBefore('_')) }
        return if (option != null) {
            RequestPermissionOutcome.Selected(option.optionId)
        } else if (remembered == PermissionMemoryOutcome.ALLOW_ALWAYS) {
            RequestPermissionOutcome.Selected(PermissionOptionId("allow_always"))
        } else {
            RequestPermissionOutcome.Selected(PermissionOptionId("reject_always"))
        }
    }

    companion object {
        fun defaultPermissionOptions(): List<PermissionOption> =
            listOf(
                PermissionOption(PermissionOptionId("allow_once"), "Allow once", PermissionOptionKind.ALLOW_ONCE),
                PermissionOption(PermissionOptionId("allow_always"), "Allow always", PermissionOptionKind.ALLOW_ALWAYS),
                PermissionOption(PermissionOptionId("reject_once"), "Reject once", PermissionOptionKind.REJECT_ONCE),
                PermissionOption(
                    PermissionOptionId("reject_always"),
                    "Reject always",
                    PermissionOptionKind.REJECT_ALWAYS,
                ),
            )

        private fun PermissionOptionKind.isAllow(): Boolean =
            this == PermissionOptionKind.ALLOW_ONCE || this == PermissionOptionKind.ALLOW_ALWAYS
    }
}
