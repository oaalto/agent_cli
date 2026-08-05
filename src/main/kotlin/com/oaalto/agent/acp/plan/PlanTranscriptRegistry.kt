package com.oaalto.agent.acp.plan

import com.oaalto.agent.acp.transcript.model.StructuredUpdate
import com.oaalto.agent.acp.transcript.model.TranscriptBlock

/**
 * Tracks plan blocks by ID for in-place updates and removal.
 * Manages the plan ID to block index mapping.
 */
internal class PlanTranscriptRegistry {
    private val planBlockIndexById = mutableMapOf<String, Int>()

    /**
     * Returns the existing block index for a plan ID, or null if not found.
     */
    fun findExistingIndex(planId: String): Int? = planBlockIndexById[planId]

    /**
     * Registers a new plan block at the given index.
     */
    fun registerPlanBlock(
        planId: String,
        index: Int,
    ) {
        planBlockIndexById[planId] = index
    }

    /**
     * Updates the block index for an existing plan.
     */
    fun updateIndex(
        planId: String,
        index: Int,
    ) {
        planBlockIndexById[planId] = index
    }

    /**
     * Removes a plan from the registry and returns its previous index.
     */
    fun removePlan(planId: String): Int? = planBlockIndexById.remove(planId)

    /**
     * Adjusts all indices after a block removal.
     * Decrements indices greater than the removed index.
     */
    fun adjustIndicesAfterRemoval(removedIndex: Int) {
        val iterator = planBlockIndexById.iterator()
        while (iterator.hasNext()) {
            val (id, idx) = iterator.next()
            if (idx > removedIndex) {
                planBlockIndexById[id] = idx - 1
            }
        }
    }

    /**
     * Handles a plan update, either updating an existing plan or creating a new one.
     * Returns the plan block and its index (existing or new).
     */
    fun handlePlanUpdate(
        update: StructuredUpdate.StartOrUpdatePlan,
        existingBlock: (Int) -> TranscriptBlock.PlanBlock?,
        nextBlockId: () -> String,
        currentBlockCount: () -> Int,
    ): PlanUpdateResult {
        val existingIndex = findExistingIndex(update.planId)

        if (existingIndex != null) {
            val existing = existingBlock(existingIndex)
            if (existing != null) {
                val updated =
                    existing.copy(
                        entries = update.entries,
                        variant = update.variant,
                        dismissed = update.dismissed || existing.dismissed,
                    )
                return PlanUpdateResult.Update(existingIndex, updated)
            }
        }

        val newBlock =
            TranscriptBlock.PlanBlock(
                blockId = nextBlockId(),
                planId = update.planId,
                entries = update.entries,
                variant = update.variant,
                dismissed = update.dismissed,
            )
        val newIndex = currentBlockCount()
        registerPlanBlock(update.planId, newIndex)
        return PlanUpdateResult.Create(newBlock)
    }

    /**
     * Result of handling a plan update.
     */
    sealed class PlanUpdateResult {
        data class Update(
            val index: Int,
            val block: TranscriptBlock.PlanBlock,
        ) : PlanUpdateResult()

        data class Create(
            val block: TranscriptBlock.PlanBlock,
        ) : PlanUpdateResult()
    }
}
