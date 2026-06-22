package com.oaalto.agent.acp

import com.agentclientprotocol.model.Cost

/**
 * Ordered transcript blocks with tool deduplication and per-card expansion state.
 */
internal class TranscriptModel {
    private val blocks = mutableListOf<TranscriptBlock>()
    private val toolBlockIndexById = mutableMapOf<String, Int>()
    private var nextBlockId = 0
    private var accumulated: AccumulatedUsage? = null
    private var usageListener: ((AccumulatedUsage) -> Unit)? = null

    fun setUsageListener(listener: (AccumulatedUsage) -> Unit) {
        usageListener = listener
    }

    fun blocks(): List<TranscriptBlock> = blocks.toList()

    fun apply(update: StructuredUpdate) {
        when (update) {
            is StructuredUpdate.AppendAgentText -> appendAgentText(update.text)
            StructuredUpdate.FinalizeAgentStream -> finalizeAgentStream()
            is StructuredUpdate.AppendPlainLine -> appendPlainLine(update)
            is StructuredUpdate.StartOrUpdateToolCall -> startOrUpdateToolCall(update)
            is StructuredUpdate.Usage -> handleUsageUpdate(update)
            else -> handleSimpleAppend(update)
        }
    }

    private fun handleSimpleAppend(update: StructuredUpdate) {
        when (update) {
            is StructuredUpdate.AppendUserEcho ->
                appendBlock(TranscriptBlock.UserEcho(allocBlockId(), update.text))
            is StructuredUpdate.AppendThought ->
                appendBlock(TranscriptBlock.Thought(allocBlockId(), update.text))
            is StructuredUpdate.AppendError ->
                appendBlock(TranscriptBlock.ErrorLine(allocBlockId(), update.message))
            is StructuredUpdate.AppendAuthFailure ->
                appendBlock(TranscriptBlock.AuthFailureLine(allocBlockId(), update.message))
            else -> Unit
        }
    }

    private fun appendPlainLine(update: StructuredUpdate.AppendPlainLine) {
        appendBlock(
            TranscriptBlock.PlainLine(
                blockId = allocBlockId(),
                text = update.line,
                isUserPrompt = update.isUserPrompt,
            ),
        )
    }

    private fun handleUsageUpdate(update: StructuredUpdate.Usage) {
        accumulated = calculateAccumulatedUsage(update)
        notifyUsageUpdate(accumulated!!)
    }

    fun toggleToolExpansion(toolCallId: String) {
        val index = toolBlockIndexById[toolCallId] ?: return
        val block = blocks.getOrNull(index) as? TranscriptBlock.ToolCallBlock ?: return
        if (!block.hasBodyContent) return
        blocks[index] = block.copy(expanded = !block.expanded)
    }

    private fun appendAgentText(text: String) {
        if (text.isBlank()) return
        val last = blocks.lastOrNull()
        if (last is TranscriptBlock.StreamingAgentText) {
            blocks[blocks.lastIndex] = last.copy(text = last.text + text)
            return
        }
        appendBlock(TranscriptBlock.StreamingAgentText(allocBlockId(), text))
    }

    private fun finalizeAgentStream() {
        val lastIndex = blocks.lastIndex
        if (lastIndex < 0) return
        val last = blocks[lastIndex]
        if (last !is TranscriptBlock.StreamingAgentText) return
        blocks[lastIndex] =
            TranscriptBlock.FinalAgentText(
                blockId = last.blockId,
                text = last.text,
            )
    }

    private fun startOrUpdateToolCall(update: StructuredUpdate.StartOrUpdateToolCall) {
        val existingIndex = toolBlockIndexById[update.toolCallId]
        if (existingIndex != null) {
            val existing = blocks.getOrNull(existingIndex) as? TranscriptBlock.ToolCallBlock ?: return
            val mergedBodyParts =
                if (update.bodyParts.isEmpty()) {
                    existing.bodyParts
                } else {
                    existing.bodyParts + update.bodyParts
                }
            blocks[existingIndex] =
                existing.copy(
                    title = update.title,
                    kind = update.kind ?: existing.kind,
                    status = update.status ?: existing.status,
                    bodyParts = mergedBodyParts,
                )
            return
        }
        val block =
            TranscriptBlock.ToolCallBlock(
                blockId = allocBlockId(),
                toolCallId = update.toolCallId,
                title = update.title,
                kind = update.kind,
                status = update.status,
                bodyParts = update.bodyParts,
            )
        toolBlockIndexById[update.toolCallId] = blocks.size
        appendBlock(block)
    }

    private fun appendBlock(block: TranscriptBlock) {
        blocks.add(block)
    }

    private fun allocBlockId(): String {
        nextBlockId += 1
        return "block-$nextBlockId"
    }

    private fun calculateAccumulatedUsage(update: StructuredUpdate.Usage): AccumulatedUsage {
        val current = accumulated
        val newUsed = (current?.totalUsed ?: 0) + update.used
        val newCost = calculateAccumulatedCost(current?.totalCost, update.cost)
        return AccumulatedUsage(
            totalUsed = newUsed,
            contextSize = update.size,
            totalCost = newCost,
        )
    }

    private fun calculateAccumulatedCost(
        currentCost: Cost?,
        updateCost: Cost?,
    ): Cost? {
        if (updateCost == null) {
            // Cost becomes null when update has no cost
            return null
        }
        if (currentCost == null) {
            // Use new cost when there was no previous cost (normalized to uppercase)
            return Cost(
                amount = updateCost.amount,
                currency = updateCost.currency.uppercase(),
            )
        }
        // Currencies match case-insensitively: accumulate and use normalized currency
        // Currencies differ: use new cost with normalized currency
        return if (currentCost.currency.equals(updateCost.currency, ignoreCase = true)) {
            Cost(
                amount = currentCost.amount + updateCost.amount,
                currency = updateCost.currency.uppercase(),
            )
        } else {
            Cost(
                amount = updateCost.amount,
                currency = updateCost.currency.uppercase(),
            )
        }
    }

    private fun notifyUsageUpdate(usage: AccumulatedUsage) {
        usageListener?.invoke(usage)
    }
}
