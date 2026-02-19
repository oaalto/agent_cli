package com.oaalto.agent

import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.testFramework.LightVirtualFile

class AgentVirtualFile(
    val configurationId: String,
    configurationName: String,
) : LightVirtualFile(tabName(configurationName), PlainTextFileType.INSTANCE, "") {
    init {
        isWritable = false
    }

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
