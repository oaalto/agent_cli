package com.oaalto.agent.acp.ui

import com.oaalto.agent.acp.SlashCommand

internal object SlashCommandMatcher {
    fun commandPrefix(input: String): String? {
        if (!input.startsWith("/")) return null
        val withoutSlash = input.drop(1)
        if (withoutSlash.contains(' ')) return null
        return withoutSlash
    }

    fun filter(
        commands: List<SlashCommand>,
        input: String,
    ): List<SlashCommand> {
        val prefix = commandPrefix(input) ?: return emptyList()
        return commands.filter { it.name.startsWith(prefix, ignoreCase = true) }
    }
}
