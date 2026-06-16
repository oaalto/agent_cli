package com.oaalto.agent.settings

import kotlin.test.Test
import kotlin.test.assertEquals

class LaunchModeTest {
    @Test
    fun `from defaults unknown values to PTY_PASSTHROUGH`() {
        assertEquals(LaunchMode.PTY_PASSTHROUGH, LaunchMode.from(null))
        assertEquals(LaunchMode.PTY_PASSTHROUGH, LaunchMode.from(""))
        assertEquals(LaunchMode.PTY_PASSTHROUGH, LaunchMode.from("invalid"))
    }

    @Test
    fun `from parses stored enum names`() {
        assertEquals(LaunchMode.PTY_PASSTHROUGH, LaunchMode.from("PTY_PASSTHROUGH"))
        assertEquals(LaunchMode.ACP_CLIENT, LaunchMode.from("ACP_CLIENT"))
    }

    @Test
    fun `fromDisplayLabel maps UI labels`() {
        assertEquals(LaunchMode.PTY_PASSTHROUGH, LaunchMode.fromDisplayLabel("Terminal"))
        assertEquals(LaunchMode.ACP_CLIENT, LaunchMode.fromDisplayLabel("ACP"))
    }

    @Test
    fun `display labels match acceptance criteria`() {
        assertEquals("Terminal", LaunchMode.PTY_PASSTHROUGH.displayLabel)
        assertEquals("ACP", LaunchMode.ACP_CLIENT.displayLabel)
    }
}
