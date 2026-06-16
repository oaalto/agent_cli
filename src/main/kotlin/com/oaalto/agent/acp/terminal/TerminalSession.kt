package com.oaalto.agent.acp.terminal

import org.jetbrains.plugins.terminal.ShellTerminalWidget

data class TerminalSession(
    val terminalId: String,
    val widget: ShellTerminalWidget,
    var outputCursor: Int = 0,
)
