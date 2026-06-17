package com.oaalto.agent.acp

import com.intellij.util.execution.ParametersListUtil
import com.oaalto.agent.AgentLaunchContext
import com.oaalto.agent.settings.AgentSettingsState
import java.util.Locale

object AcpLaunchArguments {
    private val ptyResumeFlags = setOf("--continue", "--resume")
    private val ptyResumeSequences = listOf(listOf("resume", "--last"))

    fun resolve(
        configuration: AgentSettingsState.AgentCliConfiguration,
        launchContext: AgentLaunchContext,
    ): List<String> {
        val merged =
            buildList {
                addAll(ParametersListUtil.parse(configuration.arguments))
                addAll(launchContext.additionalArguments)
            }
        val stripped = stripPtyResumeArguments(merged)
        return injectEntryArguments(configuration.binaryPath, stripped)
    }

    fun entryArgumentsForExecutable(binaryPath: String): List<String>? =
        when (executableName(binaryPath)) {
            "cursor-agent", "agent" -> listOf("acp")
            else -> null
        }

    internal fun stripPtyResumeArguments(arguments: List<String>): List<String> {
        val result = arguments.toMutableList()
        result.removeAll { it in ptyResumeFlags }
        ptyResumeSequences.forEach { sequence ->
            var index = 0
            while (index <= result.size - sequence.size) {
                if (result.subList(index, index + sequence.size) == sequence) {
                    repeat(sequence.size) { result.removeAt(index) }
                } else {
                    index++
                }
            }
        }
        return result
    }

    private fun injectEntryArguments(
        binaryPath: String,
        arguments: List<String>,
    ): List<String> {
        val entryArguments = entryArgumentsForExecutable(binaryPath)
        return when {
            entryArguments == null || entryArguments.all { it in arguments } -> arguments
            else -> entryArguments.filterNot { it in arguments } + arguments
        }
    }

    private fun executableName(binaryPath: String): String {
        val normalizedPath = binaryPath.trim()
        if (normalizedPath.isBlank()) return ""
        val fileName = normalizedPath.substringAfterLast('/').substringAfterLast('\\')
        return fileName.substringBeforeLast('.').lowercase(Locale.ROOT)
    }
}
