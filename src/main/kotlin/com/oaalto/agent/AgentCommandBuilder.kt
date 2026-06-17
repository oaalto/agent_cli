package com.oaalto.agent

data class AgentWslCommandRequest(
    val binaryPath: String,
    val arguments: List<String>,
    val wslDistribution: String,
    val wslWorkingDirectory: String,
    val useNodeShellWrapper: Boolean = false,
    val environmentVariables: Map<String, String> = emptyMap(),
)

object AgentCommandBuilder {
    fun buildLocalCommand(
        binaryPath: String,
        arguments: List<String>,
        useNodeShellWrapper: Boolean,
    ): List<String> = buildAgentCommand(binaryPath, arguments, useNodeShellWrapper)

    fun buildWslCommand(request: AgentWslCommandRequest): List<String> =
        buildList {
            add("wsl.exe")
            val distribution = request.wslDistribution.trim()
            if (distribution.isNotBlank()) {
                add("--distribution")
                add(distribution)
            }
            add("--cd")
            add(request.wslWorkingDirectory)
            add("--")
            addAll(
                withEnvironmentPrefix(
                    request.environmentVariables,
                    buildAgentCommand(request.binaryPath, request.arguments, request.useNodeShellWrapper),
                ),
            )
        }

    private fun withEnvironmentPrefix(
        environmentVariables: Map<String, String>,
        command: List<String>,
    ): List<String> {
        if (environmentVariables.isEmpty()) {
            return command
        }
        return buildList {
            add("env")
            environmentVariables.forEach { (key, value) ->
                add("$key=${quoteEnvAssignmentValue(value)}")
            }
            addAll(command)
        }
    }

    private fun quoteEnvAssignmentValue(value: String): String =
        when {
            value.isEmpty() -> "''"
            ENV_SAFE_VALUE.matches(value) -> value
            else -> "'" + value.replace("'", "'\"'\"'") + "'"
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

    private fun quoteForPosixShell(value: String): String =
        when {
            value.isEmpty() -> "''"
            POSIX_SAFE_VALUE.matches(value) -> value
            else -> "'" + value.replace("'", "'\"'\"'") + "'"
        }

    private val NODE_SHELL_WRAPPER = listOf("bash", "-ilc")

    private val POSIX_SAFE_VALUE = Regex("""[A-Za-z0-9_@%+=:,./-]+""")
    private val ENV_SAFE_VALUE = Regex("""[A-Za-z0-9_@%+=:,./-]*""")
}
