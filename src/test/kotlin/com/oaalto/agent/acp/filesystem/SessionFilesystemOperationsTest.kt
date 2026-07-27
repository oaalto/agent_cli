package com.oaalto.agent.acp.filesystem

import com.agentclientprotocol.model.PermissionOptionId
import com.agentclientprotocol.model.RequestPermissionOutcome
import com.oaalto.agent.acp.permission.PermissionCoordinator
import com.oaalto.agent.acp.permission.PermissionMemoryStore
import com.oaalto.agent.acp.permission.PermissionPromptUi
import kotlinx.coroutines.runBlocking
import kotlin.io.path.createTempDirectory
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class SessionFilesystemOperationsTest {
    private fun createScoped(scopeRoot: java.nio.file.Path): ScopedFileSystemOperations =
        ScopedFileSystemOperations.create(scopeRoot)

    private fun createPermissionCoordinator(allowWrite: Boolean = true): PermissionCoordinator {
        val settings =
            com.oaalto.agent.settings
                .AgentSettingsState()
        val memory = PermissionMemoryStore(settings)
        return PermissionCoordinator(
            configurationId = "test-config",
            memoryStore = memory,
            promptUi =
                PermissionPromptUi { _, _ ->
                    if (allowWrite) {
                        RequestPermissionOutcome.Selected(
                            PermissionOptionId("allow_once"),
                        )
                    } else {
                        RequestPermissionOutcome.Selected(
                            PermissionOptionId("reject_once"),
                        )
                    }
                },
        )
    }

    @Test
    fun `readText succeeds for in-scope file`() =
        runBlocking {
            val root = createTempDirectory("acp-deep-read")
            val file = root.resolve("src/main.kt")
            file.parent.toFile().mkdirs()
            file.writeText("hello world")

            val scoped = createScoped(root)
            val vfs = InMemoryScopedFileSystemAccess()
            vfs.writeText(file.toAbsolutePath(), "hello world")
            val perm = createPermissionCoordinator()
            val deep = SessionFilesystemOperationsImpl(scoped, vfs, perm)

            val result = deep.readText("src/main.kt", null, null)

            assertIs<SessionFilesystemResult.Success>(result)
            assertEquals("hello world", result.content)
        }

    @Test
    fun `readText rejects out-of-scope path`() =
        runBlocking {
            val root = createTempDirectory("acp-deep-out")
            val scoped = createScoped(root)
            val vfs = InMemoryScopedFileSystemAccess()
            val perm = createPermissionCoordinator()
            val deep = SessionFilesystemOperationsImpl(scoped, vfs, perm)

            val result = deep.readText("../outside.txt", null, null)

            assertIs<SessionFilesystemResult.Failure>(result)
            assertEquals(SessionFilesystemResult.FailureReason.OUT_OF_SCOPE, result.reason)
            assertTrue(result.message.contains("outside"))
        }

    @Test
    fun `writeText is blocked when permission denied`() =
        runBlocking {
            val root = createTempDirectory("acp-deep-perm")
            val file = root.resolve("new.txt")
            file.parent.toFile().mkdirs()

            val scoped = createScoped(root)
            val vfs = InMemoryScopedFileSystemAccess()
            val perm = createPermissionCoordinator(allowWrite = false)
            val deep = SessionFilesystemOperationsImpl(scoped, vfs, perm)

            val result = deep.writeText("new.txt", "content")

            assertIs<SessionFilesystemResult.Failure>(result)
            assertEquals(SessionFilesystemResult.FailureReason.PERMISSION_DENIED, result.reason)
        }

    @Test
    fun `writeText is blocked when VFS reports read-only`() =
        runBlocking {
            val root = createTempDirectory("acp-deep-readonly")
            val file = root.resolve("readonly.txt")
            file.writeText("existing")

            val scoped = createScoped(root)
            val vfs =
                InMemoryScopedFileSystemAccess(
                    readOnlyPaths = setOf(file.toAbsolutePath()),
                )
            val perm = createPermissionCoordinator()
            val deep = SessionFilesystemOperationsImpl(scoped, vfs, perm)

            val result = deep.writeText("readonly.txt", "new content")

            assertIs<SessionFilesystemResult.Failure>(result)
            assertEquals(SessionFilesystemResult.FailureReason.VFS_ERROR, result.reason)
            assertTrue(result.message.contains("read-only"))
        }

    @Test
    fun `writeText is blocked when VFS reports ignored`() =
        runBlocking {
            val root = createTempDirectory("acp-deep-ignored")
            val file = root.resolve("ignored.txt")
            file.writeText("existing")

            val scoped = createScoped(root)
            val vfs =
                InMemoryScopedFileSystemAccess(
                    ignoredPaths = setOf(file.toAbsolutePath()),
                )
            val perm = createPermissionCoordinator()
            val deep = SessionFilesystemOperationsImpl(scoped, vfs, perm)

            val result = deep.writeText("ignored.txt", "new content")

            assertIs<SessionFilesystemResult.Failure>(result)
            assertEquals(SessionFilesystemResult.FailureReason.VFS_ERROR, result.reason)
            assertTrue(result.message.contains("ignored"))
        }

    @Test
    fun `readText slices by line and limit`() =
        runBlocking {
            val root = createTempDirectory("acp-deep-slice")
            val file = root.resolve("multi.txt")
            file.parent.toFile().mkdirs()
            file.writeText("line1\nline2\nline3\nline4\nline5")

            val scoped = createScoped(root)
            val vfs = InMemoryScopedFileSystemAccess()
            vfs.writeText(file.toAbsolutePath(), "line1\nline2\nline3\nline4\nline5")
            val perm = createPermissionCoordinator()
            val deep = SessionFilesystemOperationsImpl(scoped, vfs, perm)

            val result = deep.readText("multi.txt", line = 1u, limit = 2u)

            assertIs<SessionFilesystemResult.Success>(result)
            assertEquals("line2\nline3", result.content)
        }

    @Test
    fun `readText with only line parameter returns from line to end`() =
        runBlocking {
            val root = createTempDirectory("acp-deep-slice-line")
            val file = root.resolve("lines.txt")
            file.parent.toFile().mkdirs()
            file.writeText("a\nb\nc\nd")

            val scoped = createScoped(root)
            val vfs = InMemoryScopedFileSystemAccess()
            vfs.writeText(file.toAbsolutePath(), "a\nb\nc\nd")
            val perm = createPermissionCoordinator()
            val deep = SessionFilesystemOperationsImpl(scoped, vfs, perm)

            val result = deep.readText("lines.txt", line = 2u, limit = null)

            assertIs<SessionFilesystemResult.Success>(result)
            assertEquals("c\nd", result.content)
        }

    @Test
    fun `readText with no line or limit returns full content`() =
        runBlocking {
            val root = createTempDirectory("acp-deep-full")
            val file = root.resolve("full.txt")
            file.parent.toFile().mkdirs()
            file.writeText("full content here")

            val scoped = createScoped(root)
            val vfs = InMemoryScopedFileSystemAccess()
            vfs.writeText(file.toAbsolutePath(), "full content here")
            val perm = createPermissionCoordinator()
            val deep = SessionFilesystemOperationsImpl(scoped, vfs, perm)

            val result = deep.readText("full.txt", line = null, limit = null)

            assertIs<SessionFilesystemResult.Success>(result)
            assertEquals("full content here", result.content)
        }

    @Test
    fun `writeText succeeds when in-scope and permitted`() =
        runBlocking {
            val root = createTempDirectory("acp-deep-write-ok")
            val file = root.resolve("new.txt")
            file.parent.toFile().mkdirs()

            val scoped = createScoped(root)
            val vfs = InMemoryScopedFileSystemAccess()
            val perm = createPermissionCoordinator()
            val deep = SessionFilesystemOperationsImpl(scoped, vfs, perm)

            val result = deep.writeText("new.txt", "new content")

            assertIs<SessionFilesystemResult.Success>(result)
            assertEquals("new content", result.content)
            assertEquals(
                "new content",
                vfs.readText(file.toAbsolutePath()).let {
                    if (it is ScopedFileSystemAccess.AccessResult.Success) it.content else ""
                },
            )
        }
}
