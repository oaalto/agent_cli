package com.oaalto.agent

import com.intellij.openapi.diagnostic.Logger

interface AgentCliLogTierProbe {
    fun isLogEnabled(): Boolean

    fun isDebugEnabled(): Boolean

    companion object {
        val default: AgentCliLogTierProbe =
            object : AgentCliLogTierProbe {
                override fun isLogEnabled(): Boolean =
                    isAgentCliLogEnabled(
                        env = System.getenv(),
                        registryLog = AgentCliRegistry.readBoolean(AgentCliRegistry.LOG_KEY),
                        registryDebug = AgentCliRegistry.readBoolean(AgentCliRegistry.DEBUG_KEY),
                    )

                override fun isDebugEnabled(): Boolean =
                    isAgentCliDebugEnabled(
                        env = System.getenv(),
                        registryDebug = AgentCliRegistry.readBoolean(AgentCliRegistry.DEBUG_KEY),
                    )
            }

        operator fun invoke(
            isLogEnabled: Boolean,
            isDebugEnabled: Boolean,
        ): AgentCliLogTierProbe =
            object : AgentCliLogTierProbe {
                override fun isLogEnabled(): Boolean = isLogEnabled

                override fun isDebugEnabled(): Boolean = isDebugEnabled
            }
    }
}

class AgentCliLog private constructor(
    private val logger: Logger,
    private val tierProbe: AgentCliLogTierProbe,
) {
    fun error(
        message: String,
        throwable: Throwable? = null,
        context: AgentCliSessionContext? = null,
    ) {
        val formatted = formatAgentCliLogMessage(message, context)
        if (throwable == null) {
            logger.error(formatted)
        } else {
            logger.error(formatted, throwable)
        }
    }

    fun warn(
        message: String,
        throwable: Throwable? = null,
        context: AgentCliSessionContext? = null,
    ) {
        val formatted = formatAgentCliLogMessage(message, context)
        if (throwable == null) {
            logger.warn(formatted)
        } else {
            logger.warn(formatted, throwable)
        }
    }

    fun info(
        message: String,
        context: AgentCliSessionContext? = null,
    ) {
        info({ message }, context)
    }

    fun info(
        message: () -> String,
        context: AgentCliSessionContext? = null,
    ) {
        if (!tierProbe.isLogEnabled()) return
        logger.info(formatAgentCliLogMessage(message(), context))
    }

    fun debug(
        message: String,
        throwable: Throwable? = null,
        context: AgentCliSessionContext? = null,
    ) {
        debug({ message }, throwable, context)
    }

    fun debug(
        message: () -> String,
        throwable: Throwable? = null,
        context: AgentCliSessionContext? = null,
    ) {
        if (!tierProbe.isDebugEnabled()) return
        val formatted = formatAgentCliLogMessage(message(), context)
        if (throwable == null) {
            logger.debug(formatted)
        } else {
            logger.debug(formatted, throwable)
        }
    }

    companion object {
        fun getInstance(owner: Class<*>): AgentCliLog =
            AgentCliLog(
                logger = Logger.getInstance(owner),
                tierProbe = AgentCliLogTierProbe.default,
            )
    }
}
