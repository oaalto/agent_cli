package com.oaalto.agent.settings

object EnvironmentVariableText {
    fun format(environmentVariables: Map<String, String>): String =
        environmentVariables.entries.joinToString("\n") { (key, value) -> "$key=$value" }

    fun parse(raw: String): Map<String, String> {
        val result = linkedMapOf<String, String>()
        raw
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") }
            .forEach { line ->
                val separator = line.indexOf('=')
                if (separator <= 0) return@forEach
                val key = line.substring(0, separator).trim()
                if (key.isBlank()) return@forEach
                val value = line.substring(separator + 1)
                result[key] = value
            }
        return result
    }
}
