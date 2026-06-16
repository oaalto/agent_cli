@file:Suppress("OPT_IN_USAGE")

package com.oaalto.agent.acp

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AcpClientCapabilitiesTest {
    @Test
    fun `advertises full support when enabled`() {
        val capabilities = AcpClientCapabilities.build(AcpClientCapabilities.fullSupport)

        assertTrue(capabilities.fs?.readTextFile == true)
        assertTrue(capabilities.fs?.writeTextFile == true)
        assertTrue(capabilities.terminal)
        assertTrue(capabilities.auth != null)
    }

    @Test
    fun `omits unsupported capabilities`() {
        val capabilities =
            AcpClientCapabilities.build(
                AcpClientCapabilities.Support(filesystemRead = true),
            )

        assertTrue(capabilities.fs?.readTextFile == true)
        assertFalse(capabilities.fs?.writeTextFile == true)
        assertFalse(capabilities.terminal)
        assertTrue(capabilities.auth == null)
    }
}
