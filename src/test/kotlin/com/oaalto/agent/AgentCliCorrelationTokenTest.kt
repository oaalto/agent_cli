package com.oaalto.agent

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AgentCliCorrelationTokenTest {
    @Test
    fun `token format is four hex chars in agent-cli bracket`() {
        val token = AgentCliCorrelationToken.generate()
        val formatted = AgentCliCorrelationToken.format(token)

        assertEquals(4, token.length)
        assertTrue(token.all { it in '0'..'9' || it in 'a'..'f' })
        assertEquals("[agent-cli:$token]", formatted)
    }
}
