package com.oaalto.agent

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AgentCliLogGatesTest {
    @Test
    fun `log disabled when no env or registry flags`() {
        assertFalse(
            isAgentCliLogEnabled(
                env = emptyMap(),
                registryLog = false,
                registryDebug = false,
            ),
        )
        assertFalse(
            isAgentCliDebugEnabled(
                env = emptyMap(),
                registryDebug = false,
            ),
        )
    }

    @Test
    fun `log enabled by AGENT_CLI_LOG env`() {
        assertTrue(
            isAgentCliLogEnabled(
                env = mapOf(AGENT_CLI_LOG_ENV to "true"),
                registryLog = false,
                registryDebug = false,
            ),
        )
    }

    @Test
    fun `log enabled by registry flag`() {
        assertTrue(
            isAgentCliLogEnabled(
                env = emptyMap(),
                registryLog = true,
                registryDebug = false,
            ),
        )
    }

    @Test
    fun `debug enabled by AGENT_CLI_DEBUG env`() {
        assertTrue(
            isAgentCliDebugEnabled(
                env = mapOf(AGENT_CLI_DEBUG_ENV to "true"),
                registryDebug = false,
            ),
        )
    }

    @Test
    fun `debug enabled by registry flag`() {
        assertTrue(
            isAgentCliDebugEnabled(
                env = emptyMap(),
                registryDebug = true,
            ),
        )
    }

    @Test
    fun `debug implies log`() {
        assertTrue(
            isAgentCliLogEnabled(
                env = mapOf(AGENT_CLI_DEBUG_ENV to "true"),
                registryLog = false,
                registryDebug = false,
            ),
        )
        assertTrue(
            isAgentCliLogEnabled(
                env = emptyMap(),
                registryLog = false,
                registryDebug = true,
            ),
        )
    }

    @Test
    fun `env flags are case insensitive`() {
        assertTrue(
            isAgentCliLogEnabled(
                env = mapOf(AGENT_CLI_LOG_ENV to "TRUE"),
                registryLog = false,
                registryDebug = false,
            ),
        )
        assertTrue(
            isAgentCliDebugEnabled(
                env = mapOf(AGENT_CLI_DEBUG_ENV to "True"),
                registryDebug = false,
            ),
        )
    }

    @Test
    fun `non-true env values do not enable tiers`() {
        assertFalse(
            isAgentCliLogEnabled(
                env = mapOf(AGENT_CLI_LOG_ENV to "1"),
                registryLog = false,
                registryDebug = false,
            ),
        )
        assertFalse(
            isAgentCliDebugEnabled(
                env = mapOf(AGENT_CLI_DEBUG_ENV to "yes"),
                registryDebug = false,
            ),
        )
    }
}
