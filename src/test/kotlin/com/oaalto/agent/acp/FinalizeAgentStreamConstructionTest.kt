package com.oaalto.agent.acp

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.name
import kotlin.io.path.readText
import kotlin.io.path.relativeTo
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * CI guard: production code must not construct [StructuredUpdate.FinalizeAgentStream]
 * outside documented allowlist sites (see [TranscriptFinalizePolicy] KDoc and acp-client wiki).
 */
class FinalizeAgentStreamConstructionTest {
    @Test
    fun `production sources only reference FinalizeAgentStream in allowlisted files`() {
        val repoRoot = Path.of("").toAbsolutePath()
        val mainRoot = repoRoot.resolve("src/main/kotlin")
        val marker = "StructuredUpdate.FinalizeAgentStream"
        val allowlisted =
            setOf(
                "TranscriptFinalizePolicy.kt",
                "TranscriptModel.kt",
                "TranscriptViewController.kt",
            )

        val violations = mutableListOf<String>()
        Files.walk(mainRoot).use { paths ->
            paths
                .filter { it.name.endsWith(".kt") }
                .forEach { file ->
                    file.readText().lineSequence().forEachIndexed { index, line ->
                        if (marker in line && file.name !in allowlisted) {
                            violations +=
                                "${file.relativeTo(repoRoot)}:${index + 1}: ${line.trim()}"
                        }
                    }
                }
        }

        assertTrue(
            violations.isEmpty(),
            "Disallowed $marker construction outside policy/model/view-controller apply paths:\n" +
                violations.joinToString("\n"),
        )
    }
}
