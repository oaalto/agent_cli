package com.oaalto.agent.settings

import kotlin.test.Test
import kotlin.test.assertEquals

class EnvironmentVariableTextTest {
    @Test
    fun `parses key value lines`() {
        val parsed =
            EnvironmentVariableText.parse(
                """
                API_KEY=secret
                # comment
                EMPTY=

                FOO=bar=baz
                """.trimIndent(),
            )

        assertEquals(
            mapOf(
                "API_KEY" to "secret",
                "EMPTY" to "",
                "FOO" to "bar=baz",
            ),
            parsed,
        )
    }

    @Test
    fun `formats environment variables`() {
        val formatted =
            EnvironmentVariableText.format(
                mapOf(
                    "API_KEY" to "secret",
                    "FOO" to "bar",
                ),
            )

        assertEquals("API_KEY=secret\nFOO=bar", formatted)
    }
}
