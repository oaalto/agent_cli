package com.oaalto.agent.acp

import com.agentclientprotocol.model.Cost
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TranscriptModelUsageAccumulationTest {
    @Test
    fun `single usage update accumulates to itself`() {
        val model = TranscriptModel()
        var capturedUsage: AccumulatedUsage? = null
        model.setUsageListener { capturedUsage = it }

        model.apply(StructuredUpdate.Usage(used = 1000, size = 128000, cost = null))

        assertTrue(capturedUsage != null)
        assertEquals(1000, capturedUsage?.totalUsed)
        assertEquals(128000, capturedUsage?.contextSize)
        assertNull(capturedUsage?.totalCost)
    }

    @Test
    fun `multiple usage updates accumulate used tokens`() {
        val model = TranscriptModel()
        val usages = mutableListOf<AccumulatedUsage>()
        model.setUsageListener { usages.add(it) }

        model.apply(StructuredUpdate.Usage(used = 1000, size = 128000, cost = null))
        model.apply(StructuredUpdate.Usage(used = 2000, size = 128000, cost = null))
        model.apply(StructuredUpdate.Usage(used = 1500, size = 128000, cost = null))

        assertEquals(3, usages.size)
        assertEquals(1000, usages[0].totalUsed)
        assertEquals(3000, usages[1].totalUsed) // 1000 + 2000
        assertEquals(4500, usages[2].totalUsed) // 1000 + 2000 + 1500
    }

    @Test
    fun `usage updates use latest context size`() {
        val model = TranscriptModel()
        val usages = mutableListOf<AccumulatedUsage>()
        model.setUsageListener { usages.add(it) }

        model.apply(StructuredUpdate.Usage(used = 1000, size = 128000, cost = null))
        model.apply(StructuredUpdate.Usage(used = 2000, size = 200000, cost = null))

        assertEquals(128000, usages[0].contextSize)
        assertEquals(200000, usages[1].contextSize)
    }

    @Test
    fun `single usage with cost accumulates cost`() {
        val model = TranscriptModel()
        var capturedUsage: AccumulatedUsage? = null
        model.setUsageListener { capturedUsage = it }

        model.apply(
            StructuredUpdate.Usage(
                used = 1000,
                size = 128000,
                cost = Cost(amount = 0.0123, currency = "USD"),
            ),
        )

        assertEquals(0.0123, capturedUsage?.totalCost?.amount)
        assertEquals("USD", capturedUsage?.totalCost?.currency)
    }

    @Test
    fun `multiple usages with same currency accumulate cost`() {
        val model = TranscriptModel()
        val usages = mutableListOf<AccumulatedUsage>()
        model.setUsageListener { usages.add(it) }

        model.apply(
            StructuredUpdate.Usage(
                used = 1000,
                size = 128000,
                cost = Cost(amount = 0.01, currency = "USD"),
            ),
        )
        model.apply(
            StructuredUpdate.Usage(
                used = 2000,
                size = 128000,
                cost = Cost(amount = 0.02, currency = "USD"),
            ),
        )

        assertEquals(2, usages.size)
        assertEquals(0.01, usages[0].totalCost?.amount)
        assertEquals(0.03, usages[1].totalCost?.amount) // 0.01 + 0.02
        assertEquals("USD", usages[1].totalCost?.currency)
    }

    @Test
    fun `cost accumulation is case insensitive for currency`() {
        val model = TranscriptModel()
        val usages = mutableListOf<AccumulatedUsage>()
        model.setUsageListener { usages.add(it) }

        model.apply(
            StructuredUpdate.Usage(
                used = 1000,
                size = 128000,
                cost = Cost(amount = 0.01, currency = "USD"),
            ),
        )
        model.apply(
            StructuredUpdate.Usage(
                used = 2000,
                size = 128000,
                cost = Cost(amount = 0.02, currency = "usd"),
            ),
        )

        assertEquals(0.03, usages[1].totalCost?.amount)
        // Currency should be normalized to uppercase
        assertEquals("USD", usages[1].totalCost?.currency)
    }

    @Test
    fun `lowercase currency is normalized to uppercase in accumulated cost`() {
        val model = TranscriptModel()
        val usages = mutableListOf<AccumulatedUsage>()
        model.setUsageListener { usages.add(it) }

        model.apply(
            StructuredUpdate.Usage(
                used = 1000,
                size = 128000,
                cost = Cost(amount = 0.01, currency = "usd"),
            ),
        )

        assertEquals(0.01, usages[0].totalCost?.amount)
        assertEquals("USD", usages[0].totalCost?.currency)
    }

    @Test
    fun `different currencies reset cost to new currency`() {
        val model = TranscriptModel()
        val usages = mutableListOf<AccumulatedUsage>()
        model.setUsageListener { usages.add(it) }

        model.apply(
            StructuredUpdate.Usage(
                used = 1000,
                size = 128000,
                cost = Cost(amount = 0.01, currency = "USD"),
            ),
        )
        model.apply(
            StructuredUpdate.Usage(
                used = 2000,
                size = 128000,
                cost = Cost(amount = 0.015, currency = "eur"), // lowercase in input
            ),
        )

        assertEquals(2, usages.size)
        // First cost in USD (normalized)
        assertEquals(0.01, usages[0].totalCost?.amount)
        assertEquals("USD", usages[0].totalCost?.currency)
        // Second cost resets to EUR (currencies differ) - normalized to uppercase
        assertEquals(0.015, usages[1].totalCost?.amount)
        assertEquals("EUR", usages[1].totalCost?.currency)
    }

    @Test
    fun `null cost in update resets accumulated cost to null`() {
        val model = TranscriptModel()
        val usages = mutableListOf<AccumulatedUsage>()
        model.setUsageListener { usages.add(it) }

        model.apply(
            StructuredUpdate.Usage(
                used = 1000,
                size = 128000,
                cost = Cost(amount = 0.01, currency = "USD"),
            ),
        )
        model.apply(StructuredUpdate.Usage(used = 2000, size = 128000, cost = null))

        assertEquals(2, usages.size)
        assertEquals(0.01, usages[0].totalCost?.amount)
        assertNull(usages[1].totalCost)
    }

    @Test
    fun `cost starts accumulating from null when first cost arrives`() {
        val model = TranscriptModel()
        val usages = mutableListOf<AccumulatedUsage>()
        model.setUsageListener { usages.add(it) }

        model.apply(StructuredUpdate.Usage(used = 1000, size = 128000, cost = null))
        model.apply(
            StructuredUpdate.Usage(
                used = 2000,
                size = 128000,
                cost = Cost(amount = 0.02, currency = "USD"),
            ),
        )

        assertEquals(2, usages.size)
        assertNull(usages[0].totalCost)
        assertEquals(0.02, usages[1].totalCost?.amount)
        assertEquals("USD", usages[1].totalCost?.currency)
    }

    @Test
    fun `listener not called for non-usage updates`() {
        val model = TranscriptModel()
        var callCount = 0
        model.setUsageListener { callCount++ }

        model.apply(StructuredUpdate.AppendAgentText("Hello"))
        model.apply(StructuredUpdate.AppendPlainLine("Line", isUserPrompt = false))
        model.apply(StructuredUpdate.FinalizeAgentStream)

        assertEquals(0, callCount)
    }

    @Test
    fun `listener called for each usage update`() {
        val model = TranscriptModel()
        var callCount = 0
        model.setUsageListener { callCount++ }

        model.apply(StructuredUpdate.Usage(used = 1000, size = 128000, cost = null))
        model.apply(StructuredUpdate.Usage(used = 2000, size = 128000, cost = null))
        model.apply(StructuredUpdate.Usage(used = 1500, size = 128000, cost = null))

        assertEquals(3, callCount)
    }

    @Test
    fun `accumulated usage tracks total independently of blocks`() {
        val model = TranscriptModel()
        val usages = mutableListOf<AccumulatedUsage>()
        model.setUsageListener { usages.add(it) }

        // Mix of transcript updates and usage updates
        model.apply(StructuredUpdate.AppendAgentText("Hello"))
        model.apply(StructuredUpdate.Usage(used = 1000, size = 128000, cost = null))
        model.apply(StructuredUpdate.AppendPlainLine("Line", isUserPrompt = false))
        model.apply(StructuredUpdate.Usage(used = 2000, size = 128000, cost = null))

        // Usage should accumulate across mixed updates
        assertEquals(2, usages.size)
        assertEquals(1000, usages[0].totalUsed)
        assertEquals(3000, usages[1].totalUsed)
    }
}
