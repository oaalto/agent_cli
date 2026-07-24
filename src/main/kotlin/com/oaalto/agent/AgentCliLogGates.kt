package com.oaalto.agent

const val AGENT_CLI_LOG_ENV = "AGENT_CLI_LOG"
const val AGENT_CLI_DEBUG_ENV = "AGENT_CLI_DEBUG"

fun isAgentCliLogEnabled(
    env: Map<String, String>,
    registryLog: Boolean,
    registryDebug: Boolean,
): Boolean =
    isAgentCliDebugEnabled(env, registryDebug) ||
        registryLog ||
        isAgentCliEnvFlagTrue(env, AGENT_CLI_LOG_ENV)

fun isAgentCliDebugEnabled(
    env: Map<String, String>,
    registryDebug: Boolean,
): Boolean = registryDebug || isAgentCliEnvFlagTrue(env, AGENT_CLI_DEBUG_ENV)

internal fun isAgentCliEnvFlagTrue(
    env: Map<String, String>,
    key: String,
): Boolean = env[key]?.equals("true", ignoreCase = true) == true
