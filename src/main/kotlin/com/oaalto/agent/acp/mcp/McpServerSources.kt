package com.oaalto.agent.acp.mcp

import com.agentclientprotocol.model.EnvVariable
import com.agentclientprotocol.model.McpServer
import com.oaalto.agent.AgentCliLog
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
    private val log = AgentCliLog.getInstance(ReflectiveIdeaMcpServerSource::class.java)

    override fun resolve(): McpServer? {
        if (!AiAssistantPresence.default.isAvailable()) {
            return null
        }
        return runCatching { resolveViaReflection() }
            .onFailure { throwable ->
                log.warn("Failed to resolve IntelliJ MCP server", throwable)
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
        return when {
            !isRunning -> {
                log.info(
                    "IntelliJ MCP server is not running; enable it in Tools | MCP Server " +
                        "before launching with IntelliJ MCP.",
                )
                null
            }
            else ->
                buildReflectiveMcpServer(service).also { server ->
                    log.info({ "Resolved IntelliJ MCP server '${server.name}'" })
                }
        }
    }

    private fun buildReflectiveMcpServer(service: Any): McpServer {
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
    private val log = AgentCliLog.getInstance(UserMcpConfigServerSource::class.java)

    override fun resolve(): List<McpServer> {
        val servers =
            UserMcpConfigPaths
                .candidateConfigPaths()
                .asSequence()
                .mapNotNull { path ->
                    runCatching { UserMcpConfigParser.readServersFromFile(path) }
                        .onFailure { throwable ->
                            log.warn("Failed to read user MCP config from $path", throwable)
                        }.getOrNull()
                }.flatten()
                .distinctBy { server -> UserMcpConfigParser.serverKey(server) }
                .toList()
        log.info({ "Resolved ${servers.size} MCP server(s) from user config" })
        return servers
    }
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
            Path.of(System.getenv("APPDATA").orEmpty()).resolve("JetBrains/AIAssistant/mcp.json"),
        ).filter { path -> path.toString().isNotBlank() && Files.isRegularFile(path) }
    }
}

internal object UserMcpConfigParser {
    private val log = AgentCliLog.getInstance(UserMcpConfigParser::class.java)
    private val json = Json { ignoreUnknownKeys = true }

    fun readServersFromFile(path: Path): List<McpServer> {
        val text = Files.readString(path)
        return if (path.fileName.toString().endsWith(".xml")) {
            UserMcpXmlSupport.readServersFromXml(text)
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
            "sse" -> parseSseServer(name, entry)
            "http", "streamable-http" -> parseHttpServer(name, entry)
            else -> parseStdioServer(name, entry)
        }
    }

    private fun parseSseServer(
        name: String,
        entry: JsonObject,
    ): McpServer? {
        val url =
            entry["url"]
                ?.jsonPrimitive
                ?.content
                ?.trim()
                .orEmpty()
        return when {
            url.isBlank() -> {
                log.warn("Skipping MCP server '$name': SSE url is blank.")
                null
            }
            else -> McpServer.Sse(name = name, url = url, headers = emptyList())
        }
    }

    private fun parseHttpServer(
        name: String,
        entry: JsonObject,
    ): McpServer? {
        val url =
            entry["url"]
                ?.jsonPrimitive
                ?.content
                ?.trim()
                .orEmpty()
        return when {
            url.isBlank() -> {
                log.warn("Skipping MCP server '$name': HTTP url is blank.")
                null
            }
            else -> McpServer.Http(name = name, url = url, headers = emptyList())
        }
    }

    private fun parseStdioServer(
        name: String,
        entry: JsonObject,
    ): McpServer? {
        val command =
            entry["command"]
                ?.jsonPrimitive
                ?.content
                ?.trim()
                .orEmpty()
        if (command.isBlank()) {
            log.warn("Skipping MCP server '$name': stdio command is blank.")
            return null
        }
        val args = entry["args"]?.jsonArray?.map { it.jsonPrimitive.content }.orEmpty()
        val env =
            entry["env"]?.jsonObject?.mapValues { it.value.jsonPrimitive.content }.orEmpty()
        return toStdioMcpServer(name, command, args, env)
    }
}
