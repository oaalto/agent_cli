package com.oaalto.agent.acp.permission

enum class PermissionMemoryOutcome {
    ALLOW_ALWAYS,
    REJECT_ALWAYS,
    ;

    companion object {
        fun fromRaw(raw: String?): PermissionMemoryOutcome? =
            entries.firstOrNull { it.name == raw?.trim()?.uppercase() }
    }
}
