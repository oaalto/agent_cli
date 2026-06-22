package com.oaalto.agent.acp

import com.agentclientprotocol.model.ToolCallStatus
import java.awt.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TranscriptColorProviderTest {
    @Test
    fun `provider returns non-null colors`() {
        val provider = DefaultTranscriptColorProvider()

        assertNotNull(provider.getPanelBackground())
        assertNotNull(provider.getTextForeground())
        assertNotNull(provider.getErrorForeground())
        assertNotNull(provider.getLinkForeground())
        assertNotNull(provider.getUserEchoColor())
        assertNotNull(provider.getThoughtColor())
    }

    @Test
    fun `badge colors return non-null for all statuses`() {
        val provider = DefaultTranscriptColorProvider()

        val statuses =
            listOf(
                ToolCallStatus.IN_PROGRESS,
                ToolCallStatus.COMPLETED,
                ToolCallStatus.FAILED,
                null,
            )

        statuses.forEach { status ->
            assertNotNull(provider.getBadgeBackground(status), "badge background for $status")
            assertNotNull(provider.getBadgeForeground(status), "badge foreground for $status")
        }
    }

    @Test
    fun `badge colors differ by status`() {
        val provider = DefaultTranscriptColorProvider()

        val inProgressBg = provider.getBadgeBackground(ToolCallStatus.IN_PROGRESS)
        val completedBg = provider.getBadgeBackground(ToolCallStatus.COMPLETED)
        val failedBg = provider.getBadgeBackground(ToolCallStatus.FAILED)
        val nullBg = provider.getBadgeBackground(null)

        // Each status should have a distinct background color
        val backgrounds = setOf(inProgressBg, completedBg, failedBg, nullBg)
        assertTrue(
            backgrounds.size >= 3,
            "Expected at least 3 distinct badge background colors, got ${backgrounds.size}",
        )
    }

    @Test
    fun `toHtml produces valid lowercase hex string`() {
        val provider = DefaultTranscriptColorProvider()

        // Test with known colors
        val red = Color(255, 0, 0)
        val green = Color(0, 255, 0)
        val blue = Color(0, 0, 255)
        val white = Color(255, 255, 255)
        val black = Color(0, 0, 0)
        val custom = Color(212, 160, 23) // #d4a017

        assertEquals("#ff0000", provider.toHtml(red))
        assertEquals("#00ff00", provider.toHtml(green))
        assertEquals("#0000ff", provider.toHtml(blue))
        assertEquals("#ffffff", provider.toHtml(white))
        assertEquals("#000000", provider.toHtml(black))
        assertEquals("#d4a017", provider.toHtml(custom))
    }

    @Test
    fun `toHtml handles edge cases`() {
        val provider = DefaultTranscriptColorProvider()

        // Single digit components
        val singleDigit = Color(10, 20, 30)
        assertEquals("#0a141e", provider.toHtml(singleDigit))

        // Mixed components
        val mixed = Color(128, 64, 200)
        assertEquals("#8040c8", provider.toHtml(mixed))
    }
}
