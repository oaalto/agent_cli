package com.oaalto.agent.acp.permission

import com.agentclientprotocol.model.PermissionOption
import com.agentclientprotocol.model.PermissionOptionId
import com.agentclientprotocol.model.PermissionOptionKind
import com.agentclientprotocol.model.RequestPermissionOutcome
import com.agentclientprotocol.model.SessionUpdate
import com.agentclientprotocol.model.ToolCallId
import com.agentclientprotocol.model.ToolKind
import com.oaalto.agent.settings.AgentSettingsState
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class PermissionCoordinatorTest {
    @Test
    fun `skips UI when allow always is remembered`() {
        runBlocking {
            val settings = AgentSettingsState()
            val memory = PermissionMemoryStore(settings)
            memory.set("config-1", ToolKind.READ, PermissionMemoryOutcome.ALLOW_ALWAYS)
            var prompts = 0
            val coordinator =
                PermissionCoordinator(
                    configurationId = "config-1",
                    memoryStore = memory,
                    promptUi =
                        PermissionPromptUi { _, _ ->
                            prompts++
                            RequestPermissionOutcome.Cancelled
                        },
                )
            val response =
                coordinator.requestSessionPermission(
                    toolCall =
                        SessionUpdate.ToolCallUpdate(
                            toolCallId = ToolCallId("tool-1"),
                            title = "Read file",
                            kind = ToolKind.READ,
                        ),
                    permissions =
                        listOf(
                            PermissionOption(
                                PermissionOptionId("allow_always"),
                                "Allow always",
                                PermissionOptionKind.ALLOW_ALWAYS,
                            ),
                        ),
                )

            assertEquals(0, prompts)
            assertIs<RequestPermissionOutcome.Selected>(response.outcome)
        }
    }
}
