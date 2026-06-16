package com.oaalto.agent.settings

enum class LaunchMode {
    PTY_PASSTHROUGH,
    ACP_CLIENT,
    ;

    val displayLabel: String
        get() =
            when (this) {
                PTY_PASSTHROUGH -> "Terminal"
                ACP_CLIENT -> "ACP"
            }

    companion object {
        fun from(raw: String?): LaunchMode {
            val normalized = raw?.trim()?.uppercase().orEmpty()
            return entries.firstOrNull { it.name == normalized } ?: PTY_PASSTHROUGH
        }

        fun displayLabels(): List<String> = entries.map { it.displayLabel }

        fun fromDisplayLabel(label: String): LaunchMode {
            val trimmed = label.trim()
            return entries.firstOrNull { it.displayLabel == trimmed } ?: PTY_PASSTHROUGH
        }
    }
}
