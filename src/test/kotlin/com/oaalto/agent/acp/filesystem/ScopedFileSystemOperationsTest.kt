package com.oaalto.agent.acp.filesystem

import kotlin.io.path.createTempDirectory
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ScopedFileSystemOperationsTest {
    @Test
    fun `allows relative in-scope read paths`() {
        val root = createTempDirectory("acp-scope-read")
        val file = root.resolve("src/main.kt")
        file.parent.toFile().mkdirs()
        file.writeText("hello")

        val scoped = ScopedFileSystemOperations(root)
        val result = scoped.resolveForRead("src/main.kt")

        assertIs<ScopedFileSystemOperations.ScopeResult.InScope>(result)
        assertEquals(file.normalize().toAbsolutePath(), result.resolved)
    }

    @Test
    fun `rejects paths outside scope`() {
        val root = createTempDirectory("acp-scope-out")
        val scoped = ScopedFileSystemOperations(root)

        val result = scoped.resolveForRead("../outside.txt")

        assertIs<ScopedFileSystemOperations.ScopeResult.OutOfScope>(result)
        assertTrue(result.message.contains("outside"))
    }

    @Test
    fun `allows absolute paths inside scope`() {
        val root = createTempDirectory("acp-scope-abs")
        val file = root.resolve("notes.txt")
        file.writeText("content")
        val scoped = ScopedFileSystemOperations(root)

        val result = scoped.resolveForRead(file.toAbsolutePath().toString())

        assertIs<ScopedFileSystemOperations.ScopeResult.InScope>(result)
    }

    @Test
    fun `resolveForWrite accepts new in-scope file`() {
        val root = createTempDirectory("acp-scope-write")
        val scoped = ScopedFileSystemOperations(root)

        val result = scoped.resolveForWrite("new-file.txt")

        assertIs<ScopedFileSystemOperations.ScopeResult.InScope>(result)
        assertEquals(root.resolve("new-file.txt").normalize().toAbsolutePath(), result.resolved)
    }
}
