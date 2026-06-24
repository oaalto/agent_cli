package com.oaalto.agent.acp.ui

import com.oaalto.agent.acp.SlashCommand
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SlashCommandMatcherTest {
    private val commands =
        listOf(
            SlashCommand(name = "web", description = "Search the web"),
            SlashCommand(name = "plan", description = "Create a plan"),
            SlashCommand(name = "test", description = "Run tests"),
        )

    @Test
    fun `returns null prefix when input does not start with slash`() {
        assertNull(SlashCommandMatcher.commandPrefix("hello"))
    }

    @Test
    fun `returns null prefix after command arguments begin`() {
        assertNull(SlashCommandMatcher.commandPrefix("/web query"))
    }

    @Test
    fun `filters commands by typed prefix`() {
        assertEquals(
            listOf(commands[0]),
            SlashCommandMatcher.filter(commands, "/we"),
        )
    }

    @Test
    fun `lists all commands for bare slash`() {
        assertEquals(commands, SlashCommandMatcher.filter(commands, "/"))
    }
}
