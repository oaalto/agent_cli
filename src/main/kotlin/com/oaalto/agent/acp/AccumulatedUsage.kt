package com.oaalto.agent.acp

import com.agentclientprotocol.model.Cost

/**
 * Cumulative token and cost totals accumulated across ACP session turns.
 *
 * [totalUsed] tracks the running sum of consumed tokens from all [UsageUpdate] events.
 * [contextSize] reflects the context window size reported in the most recent update.
 * [totalCost] is the accumulated cost (null if no cost data has been received).
 */
data class AccumulatedUsage(
    val totalUsed: Long,
    val contextSize: Long,
    val totalCost: Cost?,
)
