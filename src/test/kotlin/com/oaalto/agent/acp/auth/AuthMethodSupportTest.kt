package com.oaalto.agent.acp.auth

import com.agentclientprotocol.model.AuthMethod
import com.agentclientprotocol.model.AuthMethodId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AuthMethodSupportTest {
    @Test
    fun `cursor login agent auth does not require api key prompt`() {
        val method =
            AuthMethod.AgentAuth(
                id = AuthMethodId("cursor-login"),
                name = "Cursor login",
                description =
                    "Authenticate using existing Cursor login credentials. " +
                        "Run 'agent login' first if not logged in.",
            )

        assertFalse(AuthMethodSupport.agentAuthRequiresApiKey(method))
    }

    @Test
    fun `api key agent auth requires api key prompt`() {
        val method =
            AuthMethod.AgentAuth(
                id = AuthMethodId("api-key"),
                name = "API key",
                description = "Enter your API key to authenticate.",
            )

        assertTrue(AuthMethodSupport.agentAuthRequiresApiKey(method))
    }

    @Test
    fun `formats auth messages with line breaks`() {
        val message =
            AuthMethodSupport.formatAuthMessage(
                methodName = "Cursor login",
                description = "Authenticate using existing Cursor login credentials.",
                actionLine = "Complete login in the Shell pane, then continue.",
            )

        assertEquals(
            """
            [auth] Cursor login
            Authenticate using existing Cursor login credentials.
            Complete login in the Shell pane, then continue.
            """.trimIndent(),
            message,
        )
    }
}
