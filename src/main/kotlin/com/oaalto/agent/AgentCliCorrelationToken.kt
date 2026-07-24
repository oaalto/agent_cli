package com.oaalto.agent

import java.util.Locale
import java.util.concurrent.ThreadLocalRandom

object AgentCliCorrelationToken {
    private const val TOKEN_RANGE = 0x10000

    fun generate(): String = String.format(Locale.US, "%04x", ThreadLocalRandom.current().nextInt(TOKEN_RANGE))

    fun format(token: String): String = "[agent-cli:$token]"
}
