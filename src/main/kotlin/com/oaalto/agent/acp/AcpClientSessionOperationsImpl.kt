package com.oaalto.agent.acp

import com.agentclientprotocol.common.ClientSessionOperations
import com.agentclientprotocol.model.CreateTerminalResponse
import com.agentclientprotocol.model.EnvVariable
import com.agentclientprotocol.model.KillTerminalCommandResponse
import com.agentclientprotocol.model.PermissionOption
import com.agentclientprotocol.model.ReadTextFileResponse
import com.agentclientprotocol.model.ReleaseTerminalResponse
import com.agentclientprotocol.model.RequestPermissionResponse
import com.agentclientprotocol.model.SessionUpdate
import com.agentclientprotocol.model.TerminalExitStatus
import com.agentclientprotocol.model.TerminalOutputResponse
import com.agentclientprotocol.model.WaitForTerminalExitResponse
import com.agentclientprotocol.model.WriteTextFileResponse
import com.agentclientprotocol.protocol.JsonRpcException
import com.agentclientprotocol.rpc.JsonRpcErrorCode
import com.oaalto.agent.acp.filesystem.SessionFilesystemOperations
import com.oaalto.agent.acp.filesystem.SessionFilesystemResult
import com.oaalto.agent.acp.permission.PermissionCoordinator
import com.oaalto.agent.acp.terminal.TerminalSessionRegistry
import com.oaalto.agent.acp.transcript.model.StructuredUpdate
import com.oaalto.agent.acp.transcript.model.TranscriptEventIngestion
import com.oaalto.agent.acp.transcript.render.TranscriptRenderer
import com.oaalto.agent.acp.ui.ShellPaneHost
import kotlinx.serialization.json.JsonElement
import org.jetbrains.plugins.terminal.ShellTerminalWidget
import org.jetbrains.plugins.terminal.TerminalUtil

class AcpClientSessionOperationsImpl(
    private val editorContext: AcpEditorContext,
    private val sessionFilesystemOperations: SessionFilesystemOperations,
    private val permissionCoordinator: PermissionCoordinator,
    private val terminalSessionRegistry: TerminalSessionRegistry,
) : ClientSessionOperations {
    private val listener: AcpSessionListener
        get() = editorContext.listener

    override suspend fun requestPermissions(
        toolCall: SessionUpdate.ToolCallUpdate,
        permissions: List<PermissionOption>,
        _meta: JsonElement?,
    ): RequestPermissionResponse = permissionCoordinator.requestSessionPermission(toolCall, permissions)

    override suspend fun notify(
        notification: SessionUpdate,
        _meta: JsonElement?,
    ) {
        TranscriptEventIngestion.ingest(notification).forEach(listener::onStructuredUpdate)
    }

    override suspend fun fsReadTextFile(
        path: String,
        line: UInt?,
        limit: UInt?,
        _meta: JsonElement?,
    ): ReadTextFileResponse =
        when (val result = sessionFilesystemOperations.readText(path, line, limit)) {
            is SessionFilesystemResult.Success -> ReadTextFileResponse(content = result.content)
            is SessionFilesystemResult.Failure -> throw fsError(result.message)
        }

    override suspend fun fsWriteTextFile(
        path: String,
        content: String,
        _meta: JsonElement?,
    ): WriteTextFileResponse =
        when (val result = sessionFilesystemOperations.writeText(path, content)) {
            is SessionFilesystemResult.Success -> WriteTextFileResponse()
            is SessionFilesystemResult.Failure -> throw fsError(result.message)
        }

    override suspend fun terminalCreate(
        command: String,
        args: List<String>,
        cwd: String?,
        env: List<EnvVariable>,
        outputByteLimit: ULong?,
        _meta: JsonElement?,
    ): CreateTerminalResponse {
        val envPairs = env.map { it.name to it.value }
        val widget =
            editorContext.shellPaneHost.startCommand(
                command = command,
                args = args,
                cwd = cwd,
                env = envPairs,
            )
        val session = terminalSessionRegistry.register(widget)
        listener.onStructuredUpdate(
            StructuredUpdate.AppendPlainLine(
                TranscriptRenderer.formatTerminalCreate(
                    ShellPaneHost.formatCommandLabel(command, args),
                    session.terminalId,
                ),
            ),
        )
        return CreateTerminalResponse(terminalId = session.terminalId)
    }

    override suspend fun terminalOutput(
        terminalId: String,
        _meta: JsonElement?,
    ): TerminalOutputResponse {
        val session =
            terminalSessionRegistry.get(terminalId)
                ?: throw fsError("Unknown terminal: $terminalId")
        val fullText = readTerminalText(session.widget)
        val cursor = session.outputCursor.coerceAtMost(fullText.length)
        val delta = fullText.substring(cursor)
        session.outputCursor = fullText.length
        val exitStatus = terminalExitStatus(session.widget)
        return TerminalOutputResponse(
            output = delta,
            truncated = false,
            exitStatus = exitStatus,
        )
    }

    override suspend fun terminalRelease(
        terminalId: String,
        _meta: JsonElement?,
    ): ReleaseTerminalResponse {
        terminalSessionRegistry.release(terminalId)
        return ReleaseTerminalResponse()
    }

    override suspend fun terminalWaitForExit(
        terminalId: String,
        _meta: JsonElement?,
    ): WaitForTerminalExitResponse {
        val session =
            terminalSessionRegistry.get(terminalId)
                ?: throw fsError("Unknown terminal: $terminalId")
        val exitStatus = awaitTerminalExit(session.widget)
        return WaitForTerminalExitResponse(
            exitCode = exitStatus?.exitCode,
            signal = exitStatus?.signal,
        )
    }

    override suspend fun terminalKill(
        terminalId: String,
        _meta: JsonElement?,
    ): KillTerminalCommandResponse {
        val session =
            terminalSessionRegistry.get(terminalId)
                ?: throw fsError("Unknown terminal: $terminalId")
        runCatching { session.widget.getProcessTtyConnector()?.close() }
        return KillTerminalCommandResponse()
    }

    private fun readTerminalText(widget: ShellTerminalWidget): String = runCatching { widget.text }.getOrDefault("")

    private fun terminalExitStatus(widget: ShellTerminalWidget): TerminalExitStatus? {
        if (TerminalUtil.hasRunningCommands(widget.ttyConnector)) return null
        return awaitTerminalExit(widget)
    }

    private fun awaitTerminalExit(widget: ShellTerminalWidget): TerminalExitStatus? {
        val connector = widget.processTtyConnector ?: return null
        return runCatching {
            val exitCode = connector.waitFor()
            TerminalExitStatus(exitCode = exitCode.toUInt())
        }.getOrNull()
    }

    private fun fsError(message: String): JsonRpcException =
        JsonRpcException(
            code = JsonRpcErrorCode.RESOURCE_NOT_FOUND.code,
            message = message,
        )
}
