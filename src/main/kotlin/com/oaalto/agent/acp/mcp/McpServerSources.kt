package com.oaalto.agent.acp.mcp

import com.agentclientprotocol.model.EnvVariable
import com.agentclientprotocol.model.McpServer
import com.intellij.openapi.diagnostic.Logger
import com.oaalto.agent.settings.AiAssistantPresence
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.nio.file.Files
import java.nio.file.Path

fun interface IdeaMcpServerSource {
    fun resolve(): McpServer?
}

fun interface UserMcpServerSource {
    fun resolve(): List<McpServer>
}

object ReflectiveIdeaMcpServerSource : IdeaMcpServerSource {
    private val logger = Logger.getInstance(ReflectiveIdeaMcpServerSource::class.java)

    override fun resolve(): McpServer? {
        if (!AiAssistantPresence.default.isAvailable()) {
            return null
        }
        return runCatching { resolveViaReflection() }
            .onFailure { throwable ->
                logger.warn("Failed to resolve IntelliJ MCP server", throwable)
            }.getOrNull()
    }

    private fun resolveViaReflection(): McpServer? {
        val settingsClass = Class.forName("com.intellij.mcpserver.settings.McpServerSettings")
        val settings = settingsClass.getMethod("getInstance").invoke(null)
        val state = settingsClass.getMethod("getState").invoke(settings)
        val enabled = state.javaClass.getMethod("getEnableMcpServer").invoke(state) as Boolean
        if (!enabled) {
            return null
        }

        val serviceClass = Class.forName("com.intellij.mcpserver.impl.McpServerService")
        val companion = serviceClass.getDeclaredField("Companion").get(null)
        val service = companion.javaClass.getMethod("getInstance").invoke(companion)
        val isRunning = service.javaClass.getMethod("isRunning").invoke(service) as Boolean
        if (!isRunning) {
            logger.info(
                "IntelliJ MCP server is not running; enable it in Tools | MCP Server " +
                    "before launching with IntelliJ MCP.",
            )
            return null
        }
        val port = service.javaClass.getMethod("getPort").invoke(service) as Int

        val utilClass = Class.forName("com.intellij.mcpserver.StdioRunnerUtilKt")
        val commandLine =
            utilClass
                .getDeclaredMethod(
                    "createStdioMcpServerCommandLine",
                    Int::class.javaPrimitiveType,
                    String::class.java,
                    Pair::class.java,
                ).invoke(null, port, "jetbrains", null)

        val commandLineClass = Class.forName("com.intellij.execution.configurations.GeneralCommandLine")
        val executable = commandLineClass.getMethod("getExePath").invoke(commandLine) as String
        val parameters = commandLineClass.getMethod("getParametersList").invoke(commandLine)
        val parametersClass = parameters.javaClass
        val parameterCount = parametersClass.getMethod("size").invoke(parameters) as Int
        val args =
            (0 until parameterCount).map { index ->
                parametersClass
                    .getMethod("get", Int::class.javaPrimitiveType)
                    .invoke(parameters, index) as String
            }
        val envMap = commandLineClass.getMethod("getEnvironment").invoke(commandLine) as Map<*, *>
        val env =
            envMap.entries.mapNotNull { (key, value) ->
                val name = key?.toString()?.trim().orEmpty()
                if (name.isBlank()) return@mapNotNull null
                EnvVariable(name = name, value = value?.toString().orEmpty())
            }

        return McpServer.Stdio(
            name = "jetbrains",
            command = executable,
            args = args,
            env = env,
        )
    }
}

object UserMcpConfigServerSource : UserMcpServerSource {
    private val logger = Logger.getInstance(UserMcpConfigServerSource::class.java)

    override fun resolve(): List<McpServer> =
        UserMcpConfigPaths
            .candidateConfigPaths()
            .asSequence()
            .mapNotNull { path ->
                runCatching { UserMcpConfigParser.readServersFromFile(path) }
                    .onFailure { throwable ->
                        logger.warn("Failed to read user MCP config from $path", throwable)
                    }.getOrNull()
            }.flatten()
            .distinctBy { server -> UserMcpConfigParser.serverKey(server) }
            .toList()
}

private object UserMcpConfigPaths {
    fun candidateConfigPaths(): List<Path> {
        val home = Path.of(System.getProperty("user.home"))
        val configRoot =
            Path.of(
                com.intellij.openapi.application.PathManager
                    .getConfigPath(),
            )
        return listOf(
            home.resolve(".junie/mcp/mcp.json"),
            configRoot.resolve("../AIAssistant/mcp.json").normalize(),
            configRoot.resolve("options/llm.mcpServers.xml").normalize(),
            home.resolve("Library/Application Support/JetBrains/AIAssistant/mcp.json"),
            home.resolve(".config/JetBrains/AIAssistant/mcp.json"),
            Path.of(System.getenv("APPDATA") ?: "").resolve("JetBrains/AIAssistant/mcp.json"),
        ).filter { path -> path.toString().isNotBlank() && Files.isRegularFile(path) }
    }
}

internal object UserMcpConfigParser {
    private val logger = Logger.getInstance(UserMcpConfigParser::class.java)
    private val json = Json { ignoreUnknownKeys = true }

    fun readServersFromFile(path: Path): List<McpServer> {
        val text = Files.readString(path)
        return if (path.fileName.toString().endsWith(".xml")) {
            readServersFromXml(text)
        } else {
            readServersFromJson(text)
        }
    }

    fun readServersFromJson(text: String): List<McpServer> {
        val root = json.parseToJsonElement(text).jsonObject
        val serversObject =
            root["mcpServers"]?.jsonObject
                ?: root["servers"]?.jsonObject
                ?: return emptyList()
        return serversObject.mapNotNull { (name, value) -> parseServerEntry(name, value.jsonObject) }
    }

    fun serverKey(server: McpServer): String =
        when (server) {
            is McpServer.Stdio -> "stdio:${server.name}:${server.command}:${server.args.joinToString()}"
            is McpServer.Http -> "http:${server.name}:${server.url}"
            is McpServer.Sse -> "sse:${server.name}:${server.url}"
        }

    private fun readServersFromXml(text: String): List<McpServer> {
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
                toStdioServer(name, command, args, env)
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

    private fun parseServerEntry(
        name: String,
        entry: JsonObject,
    ): McpServer? {
        if (name.isBlank()) {
            return null
        }
        val type =
            entry["type"]
                ?.jsonPrimitive
                ?.content
                ?.trim()
                ?.lowercase()
                .orEmpty()
        return when (type) {
            "sse" -> {
                val url =
                    entry["url"]
                        ?.jsonPrimitive
                        ?.content
                        ?.trim()
                        .orEmpty()
                if (url.isBlank()) {
                    logger.warn("Skipping MCP server '$name': SSE url is blank.")
                    null
                } else {
                    McpServer.Sse(name = name, url = url, headers = emptyList())
                }
            }
            "http", "streamable-http" -> {
                val url =
                    entry["url"]
                        ?.jsonPrimitive
                        ?.content
                        ?.trim()
                        .orEmpty()
                if (url.isBlank()) {
                    logger.warn("Skipping MCP server '$name': HTTP url is blank.")
                    null
                } else {
                    McpServer.Http(name = name, url = url, headers = emptyList())
                }
            }
            else -> {
                val command =
                    entry["command"]
                        ?.jsonPrimitive
                        ?.content
                        ?.trim()
                        .orEmpty()
                if (command.isBlank()) {
                    logger.warn("Skipping MCP server '$name': stdio command is blank.")
                    return null
                }
                val args = entry["args"]?.jsonArray?.map { it.jsonPrimitive.content }.orEmpty()
                val env =
                    entry["env"]?.jsonObject?.mapValues { it.value.jsonPrimitive.content }.orEmpty()
                toStdioServer(name, command, args, env)
            }
        }
    }

    private fun toStdioServer(
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
}
