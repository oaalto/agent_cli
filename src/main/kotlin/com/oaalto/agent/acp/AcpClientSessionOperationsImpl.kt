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
import com.oaalto.agent.acp.filesystem.IdeScopedFileSystemAccess
import com.oaalto.agent.acp.filesystem.ScopedFileSystemOperations
import com.oaalto.agent.acp.permission.PermissionCoordinator
import com.oaalto.agent.acp.terminal.TerminalSessionRegistry
import com.oaalto.agent.acp.ui.ShellPaneHost
import kotlinx.serialization.json.JsonElement
import org.jetbrains.plugins.terminal.ShellTerminalWidget
import org.jetbrains.plugins.terminal.TerminalUtil

class AcpClientSessionOperationsImpl(
    private val editorContext: AcpEditorContext,
    private val scopedFileSystem: ScopedFileSystemOperations,
    private val fileSystemAccess: IdeScopedFileSystemAccess,
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
        AcpPromptEventDispatcher.dispatchSessionUpdate(notification, listener)
    }

    override suspend fun fsReadTextFile(
        path: String,
        line: UInt?,
        limit: UInt?,
        _meta: JsonElement?,
    ): ReadTextFileResponse {
        when (val scope = scopedFileSystem.resolveForRead(path)) {
            is ScopedFileSystemOperations.ScopeResult.OutOfScope ->
                throw fsError(scope.message)
            is ScopedFileSystemOperations.ScopeResult.InScope -> {
                val readResult = fileSystemAccess.readText(scope.resolved)
                return when (readResult) {
                    is IdeScopedFileSystemAccess.AccessResult.Failure ->
                        throw fsError(readResult.message)
                    is IdeScopedFileSystemAccess.AccessResult.Success -> {
                        val content = sliceLines(readResult.content, line, limit)
                        ReadTextFileResponse(content = content)
                    }
                }
            }
        }
    }

    override suspend fun fsWriteTextFile(
        path: String,
        content: String,
        _meta: JsonElement?,
    ): WriteTextFileResponse {
        when (val scope = scopedFileSystem.resolveForWrite(path)) {
            is ScopedFileSystemOperations.ScopeResult.OutOfScope ->
                throw fsError(scope.message)
            is ScopedFileSystemOperations.ScopeResult.InScope -> {
                fileSystemAccess.isBlockedForWrite(scope.resolved)?.let { message ->
                    throw fsError(message)
                }
                if (!permissionCoordinator.requestWritePermission(path)) {
                    throw fsError("Write permission denied for $path")
                }
                return when (val writeResult = fileSystemAccess.writeText(scope.resolved, content)) {
                    is IdeScopedFileSystemAccess.AccessResult.Failure ->
                        throw fsError(writeResult.message)
                    is IdeScopedFileSystemAccess.AccessResult.Success ->
                        WriteTextFileResponse()
                }
            }
        }
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
        listener.onTranscriptPlainLine(
            TranscriptRenderer.formatTerminalCreate(
                ShellPaneHost.formatCommandLabel(command, args),
                session.terminalId,
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

    private fun sliceLines(
        content: String,
        line: UInt?,
        limit: UInt?,
    ): String {
        if (line == null && limit == null) return content
        val lines = content.lines()
        val start = line?.toInt() ?: 0
        val end =
            if (limit != null) {
                (start + limit.toInt()).coerceAtMost(lines.size)
            } else {
                lines.size
            }
        return lines.drop(start).take(end - start).joinToString("\n")
    }

    private fun fsError(message: String): JsonRpcException =
        JsonRpcException(
            code = JsonRpcErrorCode.RESOURCE_NOT_FOUND.code,
            message = message,
        )

    companion object {
        fun create(editorContext: AcpEditorContext): AcpClientSessionOperationsImpl {
            val scoped =
                ScopedFileSystemOperations.create(
                    scopeRoot = editorContext.scopeRoot,
                    projectBasePath = editorContext.project.basePath,
                )
            return AcpClientSessionOperationsImpl(
                editorContext = editorContext,
                scopedFileSystem = scoped,
                fileSystemAccess = IdeScopedFileSystemAccess(editorContext.project),
                permissionCoordinator = editorContext.permissionCoordinator(),
                terminalSessionRegistry = editorContext.terminalSessionRegistry,
            )
        }
    }
}
