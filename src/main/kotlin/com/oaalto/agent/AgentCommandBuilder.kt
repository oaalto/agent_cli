package com.oaalto.agent

object AgentCommandBuilder {
    fun buildLocalCommand(
        binaryPath: String,
        arguments: List<String>,
        useNodeShellWrapper: Boolean,
    ): List<String> = buildAgentCommand(binaryPath, arguments, useNodeShellWrapper)

    fun buildWslCommand(
        binaryPath: String,
        arguments: List<String>,
        wslDistribution: String,
        wslWorkingDirectory: String,
        useNodeShellWrapper: Boolean = false,
    ): List<String> =
        buildList {
            add("wsl.exe")
            val distribution = wslDistribution.trim()
            if (distribution.isNotBlank()) {
                add("--distribution")
                add(distribution)
            }
            add("--cd")
            add(wslWorkingDirectory)
            add("--")
            addAll(buildAgentCommand(binaryPath, arguments, useNodeShellWrapper))
        }

    private fun buildAgentCommand(
        binaryPath: String,
        arguments: List<String>,
        useNodeShellWrapper: Boolean,
    ): List<String> {
        if (!useNodeShellWrapper) {
            return listOf(binaryPath) + arguments
        }
        return NODE_SHELL_WRAPPER + shellPayload(binaryPath, arguments)
    }

    private fun shellPayload(
        binaryPath: String,
        arguments: List<String>,
    ): String =
        (listOf("exec", quoteForPosixShell(binaryPath)) + arguments.map(::quoteForPosixShell))
            .joinToString(" ")

    private fun quoteForPosixShell(value: String): String {
        if (value.isEmpty()) return "''"
        if (POSIX_SAFE_VALUE.matches(value)) return value
        return "'" + value.replace("'", "'\"'\"'") + "'"
    }

    private val NODE_SHELL_WRAPPER = listOf("bash", "-ilc")

    private val POSIX_SAFE_VALUE = Regex("""[A-Za-z0-9_@%+=:,./-]+""")
}
