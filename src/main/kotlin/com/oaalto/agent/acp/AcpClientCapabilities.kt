@file:Suppress("OPT_IN_USAGE")

package com.oaalto.agent.acp

import com.agentclientprotocol.model.AuthCapabilities
import com.agentclientprotocol.model.ClientCapabilities
import com.agentclientprotocol.model.FileSystemCapability

object AcpClientCapabilities {
    data class Support(
        val filesystemRead: Boolean = false,
        val filesystemWrite: Boolean = false,
        val terminal: Boolean = false,
        val terminalAuth: Boolean = false,
        val agentAuth: Boolean = false,
    )

    fun build(support: Support): ClientCapabilities =
        ClientCapabilities(
            fs =
                if (support.filesystemRead || support.filesystemWrite) {
                    FileSystemCapability(
                        readTextFile = support.filesystemRead,
                        writeTextFile = support.filesystemWrite,
                    )
                } else {
                    null
                },
            terminal = support.terminal,
            auth = if (support.terminalAuth || support.agentAuth) AuthCapabilities() else null,
        )

    val fullSupport: Support =
        Support(
            filesystemRead = true,
            filesystemWrite = true,
            terminal = true,
            terminalAuth = true,
            agentAuth = true,
        )
}
