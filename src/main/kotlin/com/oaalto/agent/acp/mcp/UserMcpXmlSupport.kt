package com.oaalto.agent.acp.mcp

import com.agentclientprotocol.model.EnvVariable
import com.agentclientprotocol.model.McpServer

internal fun toStdioMcpServer(
    name: String,
    command: String,
    args: List<String>,
    env: Map<String, String>,
): McpServer.Stdio =
    McpServer.Stdio(
        name = name,
        command = command,
        args = args,
        env = env.map { (key, value) -> EnvVariable(name = key, value = value) },
    )

internal object UserMcpXmlSupport {
    fun readServersFromXml(text: String): List<McpServer> {
        val pattern =
            Regex(
                """<server\b[^>]*\bname="([^"]+)"[^>]*>(.*?)</server>""",
                setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE),
            )
        return pattern
            .findAll(text)
            .mapNotNull { match ->
                val name = match.groupValues[1].trim()
                val body = match.groupValues[2]
                if (name.isBlank()) return@mapNotNull null
                val command = xmlValue(body, "command") ?: return@mapNotNull null
                val args = xmlListValues(body, "arg")
                val env = xmlMapValues(body, "env")
                toStdioMcpServer(name, command, args, env)
            }.toList()
    }

    private fun xmlValue(
        body: String,
        tag: String,
    ): String? =
        Regex("""<$tag>(.*?)</$tag>""", RegexOption.DOT_MATCHES_ALL)
            .find(body)
            ?.groupValues
            ?.get(1)
            ?.trim()
            ?.takeIf { it.isNotEmpty() }

    private fun xmlListValues(
        body: String,
        tag: String,
    ): List<String> =
        Regex("""<$tag>(.*?)</$tag>""", RegexOption.DOT_MATCHES_ALL)
            .findAll(body)
            .map { it.groupValues[1].trim() }
            .filter { it.isNotEmpty() }
            .toList()

    private fun xmlMapValues(
        body: String,
        tag: String,
    ): Map<String, String> {
        val envBlock =
            Regex("""<$tag>(.*?)</$tag>""", RegexOption.DOT_MATCHES_ALL)
                .find(body)
                ?.groupValues
                ?.get(1)
                .orEmpty()
        return Regex("""<entry\b[^>]*\bkey="([^"]+)"[^>]*\bvalue="([^"]*)"""")
            .findAll(envBlock)
            .associate { match ->
                match.groupValues[1] to match.groupValues[2]
            }
    }
}
