package com.oaalto.agent.acp.transport

import com.agentclientprotocol.client.Client
import com.agentclientprotocol.protocol.Protocol
import com.agentclientprotocol.transport.StdioTransport
import com.oaalto.agent.AgentCliLog
import com.oaalto.agent.AgentCliSessionContext
import com.oaalto.agent.acp.AcpLaunchPlan
import com.oaalto.agent.acp.AcpSessionListener
import com.oaalto.agent.acp.StructuredUpdate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.util.concurrent.TimeUnit

class ProcessStdioTransport(
    private val scope: CoroutineScope,
) : AcpProcessTransport {
    private var process: Process? = null
    private var protocol: Protocol? = null
    private var stderrJob: Job? = null
    private var exitJob: Job? = null

    // StdioTransport's Flow-based constructor is private; the deprecated Source/Sink
    // constructor is the only public option. Tracked: consider forking or upstream change.
    @Suppress("DEPRECATION")
    override suspend fun connect(
        launchPlan: AcpLaunchPlan,
        listener: AcpSessionListener,
        sessionLogContext: () -> AgentCliSessionContext,
    ): AcpProcessTransport.Connected {
        dispose()

        val startedProcess =
            ProcessBuilder(launchPlan.command)
                .directory(Path.of(launchPlan.processWorkingDirectory).toFile())
                .apply {
                    launchPlan.environmentVariables.forEach { (key, value) ->
                        environment()[key] = value
                    }
                }.redirectInput(ProcessBuilder.Redirect.PIPE)
                .redirectOutput(ProcessBuilder.Redirect.PIPE)
                .redirectError(ProcessBuilder.Redirect.PIPE)
                .start()

        val transport =
            StdioTransport(
                parentScope = scope,
                ioDispatcher = Dispatchers.IO,
                input = startedProcess.inputStream.asSource().buffered(),
                output = startedProcess.outputStream.asSink().buffered(),
                name = "acp-agent-stdio",
            )
        val protocolInstance = Protocol(scope, transport)
        val clientInstance = Client(protocolInstance)

        process = startedProcess
        protocol = protocolInstance

        stderrJob =
            scope.launch {
                monitorStderr(startedProcess, listener)
            }
        exitJob =
            scope.launch {
                val exitCode = startedProcess.waitFor()
                if (scope.isActive) {
                    log.info(
                        { "Agent process exited with code $exitCode" },
                        sessionLogContext(),
                    )
                    listener.onError("Agent process exited with code $exitCode.")
                }
            }

        return AcpProcessTransport.Connected(protocolInstance, clientInstance)
    }

    override fun dispose() {
        stderrJob?.cancel()
        exitJob?.cancel()
        runBlocking(Dispatchers.IO) {
            runCatching { protocol?.close() }
        }
        process?.let { activeProcess ->
            if (activeProcess.isAlive) {
                activeProcess.destroy()
                if (!activeProcess.waitFor(PROCESS_DESTROY_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
                    activeProcess.destroyForcibly()
                }
            }
        }
        process = null
        protocol = null
        stderrJob = null
        exitJob = null
    }

    private fun monitorStderr(
        activeProcess: Process,
        listener: AcpSessionListener,
    ) {
        BufferedReader(InputStreamReader(activeProcess.errorStream, StandardCharsets.UTF_8)).use { reader ->
            var line = reader.readLine()
            while (line != null) {
                if (line.isNotBlank()) {
                    listener.onStructuredUpdate(StructuredUpdate.AppendPlainLine("[stderr] $line"))
                }
                line = reader.readLine()
            }
        }
    }

    companion object {
        private val log = AgentCliLog.getInstance(ProcessStdioTransport::class.java)
        private const val PROCESS_DESTROY_TIMEOUT_MS = 3000L
    }
}
