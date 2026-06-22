package com.oaalto.agent.acp.plan

import com.oaalto.agent.acp.PlanEntry
import com.oaalto.agent.acp.PlanEntryPriority
import com.oaalto.agent.acp.PlanEntryStatus
import com.oaalto.agent.acp.StructuredUpdate
import com.oaalto.agent.acp.TranscriptBlock
import com.oaalto.agent.acp.TranscriptModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class TranscriptModelPlanTest {
    @Test
    fun `creates new plan block on first StartOrUpdatePlan`() {
        val model = TranscriptModel()
        val entries =
            listOf(
                PlanEntry("Task 1", PlanEntryStatus.PENDING, PlanEntryPriority.MEDIUM),
            )

        model.apply(StructuredUpdate.StartOrUpdatePlan("plan-1", entries))

        val blocks = model.blocks()
        assertEquals(1, blocks.size)
        assertIs<TranscriptBlock.PlanBlock>(blocks[0])
        assertEquals("plan-1", (blocks[0] as TranscriptBlock.PlanBlock).planId)
    }

    @Test
    fun `updates existing plan block in place on subsequent StartOrUpdatePlan with same ID`() {
        val model = TranscriptModel()
        val initialEntries =
            listOf(
                PlanEntry("Task 1", PlanEntryStatus.PENDING, PlanEntryPriority.MEDIUM),
            )
        val updatedEntries =
            listOf(
                PlanEntry("Task 1", PlanEntryStatus.COMPLETED, PlanEntryPriority.MEDIUM),
                PlanEntry("Task 2", PlanEntryStatus.PENDING, PlanEntryPriority.MEDIUM),
            )

        model.apply(StructuredUpdate.StartOrUpdatePlan("plan-1", initialEntries))
        val initialBlocks = model.blocks()
        val initialBlockId = initialBlocks[0].blockId

        model.apply(StructuredUpdate.StartOrUpdatePlan("plan-1", updatedEntries))
        val updatedBlocks = model.blocks()

        assertEquals(1, updatedBlocks.size)
        assertEquals(initialBlockId, updatedBlocks[0].blockId)
        assertEquals(2, (updatedBlocks[0] as TranscriptBlock.PlanBlock).entries.size)
    }

    @Test
    fun `creates separate blocks for different plan IDs`() {
        val model = TranscriptModel()

        model.apply(
            StructuredUpdate.StartOrUpdatePlan(
                "plan-1",
                listOf(
                    PlanEntry("Task A", PlanEntryStatus.PENDING, PlanEntryPriority.MEDIUM),
                ),
            ),
        )
        model.apply(
            StructuredUpdate.StartOrUpdatePlan(
                "plan-2",
                listOf(
                    PlanEntry("Task B", PlanEntryStatus.PENDING, PlanEntryPriority.MEDIUM),
                ),
            ),
        )

        val blocks = model.blocks()
        assertEquals(2, blocks.size)
        val planIds = blocks.map { (it as TranscriptBlock.PlanBlock).planId }.toSet()
        assertEquals(setOf("plan-1", "plan-2"), planIds)
    }

    @Test
    fun `marks plan block as dismissed on RemovePlan`() {
        val model = TranscriptModel()
        model.apply(
            StructuredUpdate.StartOrUpdatePlan(
                "plan-1",
                listOf(
                    PlanEntry("Task 1", PlanEntryStatus.PENDING, PlanEntryPriority.MEDIUM),
                ),
            ),
        )

        model.apply(StructuredUpdate.RemovePlan("plan-1"))

        val blocks = model.blocks()
        assertEquals(1, blocks.size)
        assertIs<TranscriptBlock.PlanBlock>(blocks[0])
        val planBlock = blocks[0] as TranscriptBlock.PlanBlock
        assertEquals(true, planBlock.dismissed)
        assertEquals("plan-1", planBlock.planId)
    }

    @Test
    fun `removing non-existent plan does nothing`() {
        val model = TranscriptModel()
        model.apply(
            StructuredUpdate.StartOrUpdatePlan(
                "plan-1",
                listOf(
                    PlanEntry("Task 1", PlanEntryStatus.PENDING, PlanEntryPriority.MEDIUM),
                ),
            ),
        )

        model.apply(StructuredUpdate.RemovePlan("non-existent"))

        val blocks = model.blocks()
        assertEquals(1, blocks.size)
    }

    @Test
    fun `plan block tracks completion count correctly`() {
        val model = TranscriptModel()
        val entries =
            listOf(
                PlanEntry("Task 1", PlanEntryStatus.COMPLETED, PlanEntryPriority.MEDIUM),
                PlanEntry("Task 2", PlanEntryStatus.IN_PROGRESS, PlanEntryPriority.MEDIUM),
                PlanEntry("Task 3", PlanEntryStatus.PENDING, PlanEntryPriority.MEDIUM),
            )

        model.apply(StructuredUpdate.StartOrUpdatePlan("plan-1", entries))

        val planBlock = model.blocks()[0] as TranscriptBlock.PlanBlock
        assertEquals(1, planBlock.completedCount)
        assertEquals(3, planBlock.entries.size)
        assertEquals(false, planBlock.isFullyComplete)
    }

    @Test
    fun `plan block reports fully complete when all entries completed`() {
        val model = TranscriptModel()
        val entries =
            listOf(
                PlanEntry("Task 1", PlanEntryStatus.COMPLETED, PlanEntryPriority.MEDIUM),
                PlanEntry("Task 2", PlanEntryStatus.COMPLETED, PlanEntryPriority.MEDIUM),
            )

        model.apply(StructuredUpdate.StartOrUpdatePlan("plan-1", entries))

        val planBlock = model.blocks()[0] as TranscriptBlock.PlanBlock
        assertEquals(2, planBlock.completedCount)
        assertEquals(true, planBlock.isFullyComplete)
    }

    @Test
    fun `empty plan is not fully complete`() {
        val model = TranscriptModel()

        model.apply(StructuredUpdate.StartOrUpdatePlan("plan-1", emptyList()))

        val planBlock = model.blocks()[0] as TranscriptBlock.PlanBlock
        assertEquals(0, planBlock.completedCount)
        assertEquals(false, planBlock.isFullyComplete)
    }

    @Test
    fun `plan maintains chronological order with other blocks`() {
        val model = TranscriptModel()

        model.apply(StructuredUpdate.AppendPlainLine("Before plan", false))
        model.apply(
            StructuredUpdate.StartOrUpdatePlan(
                "plan-1",
                listOf(
                    PlanEntry("Task", PlanEntryStatus.PENDING, PlanEntryPriority.MEDIUM),
                ),
            ),
        )
        model.apply(StructuredUpdate.AppendPlainLine("After plan", false))

        val blocks = model.blocks()
        assertEquals(3, blocks.size)
        assertIs<TranscriptBlock.PlainLine>(blocks[0])
        assertIs<TranscriptBlock.PlanBlock>(blocks[1])
        assertIs<TranscriptBlock.PlainLine>(blocks[2])
    }

    @Test
    fun `dismissing plan marks it dismissed but preserves other plans`() {
        val model = TranscriptModel()

        model.apply(
            StructuredUpdate.StartOrUpdatePlan(
                "plan-1",
                listOf(
                    PlanEntry("Task 1", PlanEntryStatus.PENDING, PlanEntryPriority.MEDIUM),
                ),
            ),
        )
        model.apply(
            StructuredUpdate.StartOrUpdatePlan(
                "plan-2",
                listOf(
                    PlanEntry("Task 2", PlanEntryStatus.PENDING, PlanEntryPriority.MEDIUM),
                ),
            ),
        )
        model.apply(
            StructuredUpdate.StartOrUpdatePlan(
                "plan-3",
                listOf(
                    PlanEntry("Task 3", PlanEntryStatus.PENDING, PlanEntryPriority.MEDIUM),
                ),
            ),
        )

        model.apply(StructuredUpdate.RemovePlan("plan-2"))

        val blocks = model.blocks()
        assertEquals(3, blocks.size)
        val planBlocks = blocks.map { it as TranscriptBlock.PlanBlock }
        assertEquals(listOf("plan-1", "plan-2", "plan-3"), planBlocks.map { it.planId })
        assertEquals(false, planBlocks[0].dismissed)
        assertEquals(true, planBlocks[1].dismissed)
        assertEquals(false, planBlocks[2].dismissed)
    }
}
