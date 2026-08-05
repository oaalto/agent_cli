package com.oaalto.agent.acp.transcript

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.isRegularFile
import kotlin.io.path.name
import kotlin.io.path.readText
import kotlin.io.path.relativeTo
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * CI guard: production transcript package imports must follow the dependency matrix
 * (see acp-client wiki and package-restructure PRD).
 */
class TranscriptPackageDependencyTest {
    private val repoRoot = Path.of("").toAbsolutePath()
    private val mainRoot = repoRoot.resolve("src/main/kotlin")
    private val viewPrefix = "com.oaalto.agent.acp.transcript.view"

    @Test
    fun `model except ingestion forbids view and javax swing`() {
        assertNoForbiddenImports(
            relativeDir = "com/oaalto/agent/acp/transcript/model",
            forbiddenPrefixes = listOf(viewPrefix),
            forbiddenExact = listOf("javax.swing"),
            rule = "model (except TranscriptEventIngestion) must not import view or javax.swing",
        ) { it.name != "TranscriptEventIngestion.kt" }
    }

    @Test
    fun `ingestion forbids view and javax swing`() {
        assertNoForbiddenImports(
            relativeDir = "com/oaalto/agent/acp/transcript/model",
            forbiddenPrefixes = listOf(viewPrefix),
            forbiddenExact = listOf("javax.swing"),
            rule = "TranscriptEventIngestion must not import view or javax.swing",
        ) { it.name == "TranscriptEventIngestion.kt" }
    }

    @Test
    fun `render forbids view and javax swing`() {
        assertNoForbiddenImports(
            relativeDir = "com/oaalto/agent/acp/transcript/render",
            forbiddenPrefixes = listOf(viewPrefix),
            forbiddenExact = listOf("javax.swing"),
            rule = "render must not import view or javax.swing",
        ) { true }
    }

    @Test
    fun `plan forbids view imports`() {
        assertNoForbiddenImports(
            relativeDir = "com/oaalto/agent/acp/plan",
            forbiddenPrefixes = listOf(viewPrefix),
            forbiddenExact = emptyList(),
            rule = "plan must not import view",
        ) { true }
    }

    private fun assertNoForbiddenImports(
        relativeDir: String,
        forbiddenPrefixes: List<String>,
        forbiddenExact: List<String>,
        rule: String,
        fileFilter: (Path) -> Boolean,
    ) {
        val violations =
            collectImportViolations(
                relativeDir = relativeDir,
                forbiddenPrefixes = forbiddenPrefixes,
                forbiddenExact = forbiddenExact,
                fileFilter = fileFilter,
            )
        assertTrue(
            violations.isEmpty(),
            "$rule:\n${violations.joinToString("\n")}",
        )
    }

    private fun collectImportViolations(
        relativeDir: String,
        forbiddenPrefixes: List<String>,
        forbiddenExact: List<String>,
        fileFilter: (Path) -> Boolean,
    ): List<String> {
        val dir = mainRoot.resolve(relativeDir)
        val violations = mutableListOf<String>()

        Files.walk(dir).use { paths ->
            paths
                .filter { it.isRegularFile() && it.name.endsWith(".kt") }
                .filter(fileFilter)
                .forEach { file ->
                    file.readText().lineSequence().forEachIndexed { index, line ->
                        val trimmed = line.trim()
                        if (!trimmed.startsWith("import ")) return@forEachIndexed
                        val importPath = trimmed.removePrefix("import ").substringBeforeLast('.')
                        if (isForbiddenImport(importPath, forbiddenPrefixes, forbiddenExact)) {
                            violations += "${file.relativeTo(repoRoot)}:${index + 1}: $trimmed"
                        }
                    }
                }
        }

        return violations
    }

    private fun isForbiddenImport(
        importPath: String,
        forbiddenPrefixes: List<String>,
        forbiddenExact: List<String>,
    ): Boolean =
        forbiddenExact.any { importPath == it || importPath.startsWith("$it.") } ||
            forbiddenPrefixes.any { importPath == it || importPath.startsWith("$it.") }
}
