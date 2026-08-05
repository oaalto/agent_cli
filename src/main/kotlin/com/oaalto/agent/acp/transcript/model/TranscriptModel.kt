package com.oaalto.agent.acp.transcript.model

import com.agentclientprotocol.model.Cost
import com.oaalto.agent.acp.AccumulatedUsage
import com.oaalto.agent.acp.plan.PlanTranscriptRegistry

/**
 * Ordered transcript blocks with tool deduplication, plan tracking, and per-card expansion state.
 */
internal class TranscriptModel {
    private val blocks = mutableListOf<TranscriptBlock>()
    private val toolBlockIndexById = mutableMapOf<String, Int>()
    private val planRegistry = PlanTranscriptRegistry()
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
            is StructuredUpdate.AppendPlainLine -> handlePlainLine(update)
            is StructuredUpdate.StartOrUpdateToolCall -> startOrUpdateToolCall(update)
            is StructuredUpdate.StartOrUpdatePlan -> applyPlanUpdate(update)
            is StructuredUpdate.RemovePlan -> applyPlanRemoval(update)
            is StructuredUpdate.Usage -> handleUsageUpdate(update)
            else -> handleSimpleAppend(update)
        }
    }

    private fun handleSimpleAppend(update: StructuredUpdate) {
        when (update) {
            is StructuredUpdate.AppendUserEcho ->
                blocks.add(TranscriptBlock.UserEcho(nextBlockId(), update.text))
            is StructuredUpdate.AppendThought ->
                blocks.add(TranscriptBlock.Thought(nextBlockId(), update.text))
            is StructuredUpdate.AppendError ->
                blocks.add(TranscriptBlock.ErrorLine(nextBlockId(), update.message))
            is StructuredUpdate.AppendAuthFailure ->
                blocks.add(TranscriptBlock.AuthFailureLine(nextBlockId(), update.message))
            else -> Unit
        }
    }

    private fun handlePlainLine(update: StructuredUpdate.AppendPlainLine) {
        blocks.add(
            TranscriptBlock.PlainLine(
                blockId = nextBlockId(),
                text = update.line,
                isUserPrompt = update.isUserPrompt,
            ),
        )
    }

    private fun handleUsageUpdate(update: StructuredUpdate.Usage) {
        val snapshot = accumulateUsage(update)
        accumulated = snapshot
        usageListener?.invoke(snapshot)
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
        blocks.add(TranscriptBlock.StreamingAgentText(nextBlockId(), text))
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
                    title = update.title.takeIf { it.isNotBlank() } ?: existing.title,
                    kind = update.kind ?: existing.kind,
                    status = update.status ?: existing.status,
                    bodyParts = mergedBodyParts,
                )
            return
        }
        val block =
            TranscriptBlock.ToolCallBlock(
                blockId = nextBlockId(),
                toolCallId = update.toolCallId,
                title = update.title,
                kind = update.kind,
                status = update.status,
                bodyParts = update.bodyParts,
            )
        toolBlockIndexById[update.toolCallId] = blocks.size
        blocks.add(block)
    }

    private fun applyPlanUpdate(update: StructuredUpdate.StartOrUpdatePlan) {
        val result =
            planRegistry.handlePlanUpdate(
                update = update,
                existingBlock = { idx -> blocks.getOrNull(idx) as? TranscriptBlock.PlanBlock },
                nextBlockId = ::nextBlockId,
                currentBlockCount = { blocks.size },
            )

        when (result) {
            is PlanTranscriptRegistry.PlanUpdateResult.Update -> {
                blocks[result.index] = result.block
            }
            is PlanTranscriptRegistry.PlanUpdateResult.Create -> {
                blocks.add(result.block)
            }
        }
    }

    private fun applyPlanRemoval(update: StructuredUpdate.RemovePlan) {
        val existingIndex = planRegistry.findExistingIndex(update.planId) ?: return
        if (existingIndex in blocks.indices) {
            val existingBlock = blocks[existingIndex] as? TranscriptBlock.PlanBlock ?: return
            // Mark the plan as dismissed instead of removing it
            blocks[existingIndex] = existingBlock.copy(dismissed = true)
        }
    }

    private fun nextBlockId(): String {
        nextBlockId += 1
        return "block-$nextBlockId"
    }

    private fun accumulateUsage(update: StructuredUpdate.Usage): AccumulatedUsage {
        val current = accumulated
        val newUsed = (current?.totalUsed ?: 0) + update.used

        val currentCost = current?.totalCost
        val updateCost = update.cost
        val newCost: Cost? =
            when {
                updateCost == null -> null
                currentCost == null -> Cost(updateCost.amount, updateCost.currency.uppercase())
                currentCost.currency.equals(updateCost.currency, ignoreCase = true) ->
                    Cost(currentCost.amount + updateCost.amount, updateCost.currency.uppercase())
                else -> Cost(updateCost.amount, updateCost.currency.uppercase())
            }

        return AccumulatedUsage(
            totalUsed = newUsed,
            contextSize = update.size,
            totalCost = newCost,
        )
    }
}
