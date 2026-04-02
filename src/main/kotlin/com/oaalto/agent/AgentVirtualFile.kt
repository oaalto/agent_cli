package com.oaalto.agent

import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.testFramework.LightVirtualFile
import java.util.UUID

class AgentVirtualFile(
    val configurationId: String,
    configurationName: String,
    val launchContext: AgentLaunchContext = AgentLaunchContext(),
) : LightVirtualFile(tabName(configurationName), PlainTextFileType.INSTANCE, "") {
    private val sessionId: String = UUID.randomUUID().toString()

    init {
        isWritable = false
    }

    override fun getPath(): String = "agent-cli/$sessionId/${getName()}"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AgentVirtualFile) return false
        return sessionId == other.sessionId
    }

    override fun hashCode(): Int = sessionId.hashCode()

    companion object {
        private const val TAB_PREFIX: String = "Agent"

        private fun tabName(configurationName: String): String {
            val suffix = configurationName.trim()
            return if (suffix.isBlank()) {
                "$TAB_PREFIX Session"
            } else {
                "$TAB_PREFIX: $suffix"
            }
        }
    }
}
