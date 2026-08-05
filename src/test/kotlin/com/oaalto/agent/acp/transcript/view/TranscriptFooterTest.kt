package com.oaalto.agent.acp.transcript.view

import com.agentclientprotocol.model.Cost
import com.intellij.ui.JBColor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TranscriptFooterTest {
    @Test
    fun `footer labels are empty initially`() {
        val footer = TranscriptFooter()

        assertEquals("", footer.usageLabel.text)
        assertEquals("", footer.costLabel.text)
    }

    @Test
    fun `footer displays formatted token usage`() {
        val footer = TranscriptFooter()

        footer.updateUsage(used = 1234, size = 128000, cost = null)

        // Usage label should show formatted numbers with thousand separators
        // (actual format depends on locale, but should contain the numbers)
        assertTrue(footer.usageLabel.text.contains("1,234"))
        assertTrue(footer.usageLabel.text.contains("128,000"))
        assertTrue(footer.usageLabel.text.contains("tokens"))
    }

    @Test
    fun `footer hides cost label when cost is null`() {
        val footer = TranscriptFooter()

        footer.updateUsage(used = 1000, size = 100000, cost = null)

        assertFalse(footer.costLabel.isVisible)
        assertEquals("", footer.costLabel.text)
    }

    @Test
    fun `footer displays USD cost with dollar sign`() {
        val footer = TranscriptFooter()

        footer.updateUsage(used = 1000, size = 100000, cost = Cost(amount = 0.0234, currency = "USD"))

        assertTrue(footer.costLabel.isVisible)
        assertEquals("$0.0234", footer.costLabel.text)
    }

    @Test
    fun `footer displays EUR cost with euro sign`() {
        val footer = TranscriptFooter()

        footer.updateUsage(used = 1000, size = 100000, cost = Cost(amount = 0.0156, currency = "EUR"))

        assertTrue(footer.costLabel.isVisible)
        assertEquals("€0.0156", footer.costLabel.text)
    }

    @Test
    fun `footer displays GBP cost with pound sign`() {
        val footer = TranscriptFooter()

        footer.updateUsage(used = 1000, size = 100000, cost = Cost(amount = 0.0189, currency = "GBP"))

        assertTrue(footer.costLabel.isVisible)
        assertEquals("£0.0189", footer.costLabel.text)
    }

    @Test
    fun `footer displays unknown currency with code prefix`() {
        val footer = TranscriptFooter()

        footer.updateUsage(used = 1000, size = 100000, cost = Cost(amount = 1.23, currency = "JPY"))

        assertTrue(footer.costLabel.isVisible)
        assertEquals("JPY 1.23", footer.costLabel.text)
    }

    @Test
    fun `footer handles currency case insensitively`() {
        val footer = TranscriptFooter()

        footer.updateUsage(used = 1000, size = 100000, cost = Cost(amount = 0.05, currency = "usd"))

        assertTrue(footer.costLabel.isVisible)
        assertEquals("$0.05", footer.costLabel.text)
    }

    @Test
    fun `usage label shows gray color when usage below threshold`() {
        val footer = TranscriptFooter()

        footer.updateUsage(used = 1000, size = 100000, cost = null)

        assertEquals(JBColor.GRAY, footer.usageLabel.foreground)
    }

    @Test
    fun `usage label shows orange color when usage exceeds eighty percent`() {
        val footer = TranscriptFooter()

        // 81000 / 100000 = 81% > 80%
        footer.updateUsage(used = 81000, size = 100000, cost = null)

        assertEquals(JBColor.ORANGE, footer.usageLabel.foreground)
    }

    @Test
    fun `usage label shows gray when size is zero to avoid division by zero`() {
        val footer = TranscriptFooter()

        footer.updateUsage(used = 1000, size = 0, cost = null)

        assertEquals(JBColor.GRAY, footer.usageLabel.foreground)
    }

    @Test
    fun `usage label shows gray at exactly eighty percent threshold`() {
        val footer = TranscriptFooter()

        // Exactly 80% - should NOT trigger orange (threshold is > 80%)
        footer.updateUsage(used = 80000, size = 100000, cost = null)

        assertEquals(JBColor.GRAY, footer.usageLabel.foreground)
    }

    @Test
    fun `cost label remains visible when updating from cost to no cost`() {
        val footer = TranscriptFooter()

        // First update with cost
        footer.updateUsage(used = 1000, size = 100000, cost = Cost(amount = 0.01, currency = "USD"))
        assertTrue(footer.costLabel.isVisible)

        // Second update without cost
        footer.updateUsage(used = 2000, size = 100000, cost = null)

        assertFalse(footer.costLabel.isVisible)
        assertEquals("", footer.costLabel.text)
    }

    @Test
    fun `footer has panel background color`() {
        val footer = TranscriptFooter()

        assertEquals(JBColor.PanelBackground, footer.background)
    }

    @Test
    fun `footer cost label initially uses gray foreground`() {
        val footer = TranscriptFooter()

        assertEquals(JBColor.GRAY, footer.costLabel.foreground)
    }
}
