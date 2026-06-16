package com.oaalto.agent.acp.terminal

import org.jetbrains.plugins.terminal.ShellTerminalWidget
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Maps ACP terminal IDs to Shell pane widget instances.
 * 3.0 policy: a second [register] replaces the existing terminal.
 */
class TerminalSessionRegistry {
    private val sessions = ConcurrentHashMap<String, TerminalSession>()

    fun register(widget: ShellTerminalWidget): TerminalSession {
        sessions.values.forEach { existing ->
            runCatching { existing.widget.close() }
        }
        sessions.clear()
        val terminalId = UUID.randomUUID().toString()
        val session = TerminalSession(terminalId = terminalId, widget = widget)
        sessions[terminalId] = session
        return session
    }

    fun get(terminalId: String): TerminalSession? = sessions[terminalId]

    fun release(terminalId: String) {
        sessions.remove(terminalId)?.let { session ->
            runCatching { session.widget.close() }
        }
    }

    fun clear() {
        sessions.values.forEach { session ->
            runCatching { session.widget.close() }
        }
        sessions.clear()
    }
}
