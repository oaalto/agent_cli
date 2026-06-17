package com.oaalto.agent.acp.filesystem

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Computable
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.vcsUtil.VcsUtil
import git4idea.repo.GitRepositoryManager
import java.nio.charset.StandardCharsets
import java.nio.file.Path

class IdeScopedFileSystemAccess(
    private val project: Project,
) {
    sealed class AccessResult {
        data class Success(
            val content: String = "",
        ) : AccessResult()

        data class Failure(
            val message: String,
        ) : AccessResult()
    }

    fun readText(resolved: Path): AccessResult {
        val virtualFile =
            findVirtualFile(resolved)
                ?: return AccessResult.Failure("File not found in IDE VFS: $resolved")
        return runRead {
            val document = FileDocumentManager.getInstance().getDocument(virtualFile)
            val text = document?.text ?: String(virtualFile.contentsToByteArray(), StandardCharsets.UTF_8)
            AccessResult.Success(text)
        }
    }

    fun writeText(
        resolved: Path,
        content: String,
    ): AccessResult {
        val virtualFile = findOrCreateVirtualFile(resolved)
        val blockedReason = virtualFile?.let { file -> writeBlockReason(file, resolved) }
        return when {
            virtualFile == null -> AccessResult.Failure("Could not resolve file in IDE VFS: $resolved")
            blockedReason != null -> AccessResult.Failure(blockedReason)
            else ->
                runWrite {
                    val document = FileDocumentManager.getInstance().getDocument(virtualFile)
                    if (document != null) {
                        document.setText(content)
                        FileDocumentManager.getInstance().saveDocument(document)
                    } else {
                        virtualFile.setBinaryContent(content.toByteArray(StandardCharsets.UTF_8))
                    }
                    AccessResult.Success()
                }
        }
    }

    fun isBlockedForWrite(resolved: Path): String? {
        val virtualFile = findVirtualFile(resolved) ?: findOrCreateVirtualFile(resolved)
        return virtualFile?.let { file -> writeBlockReason(file, resolved) }
            ?: "Could not resolve file in IDE VFS: $resolved"
    }

    private fun findVirtualFile(resolved: Path): VirtualFile? {
        val path = resolved.normalize().toAbsolutePath().toString()
        return LocalFileSystem.getInstance().findFileByPath(path)
            ?: VfsUtil.findFileByIoFile(resolved.toFile(), true)
    }

    private fun writeBlockReason(
        virtualFile: VirtualFile,
        resolved: Path,
    ): String? =
        when {
            !virtualFile.isWritable -> "File is read-only in the IDE: $resolved"
            isGitIgnored(virtualFile) -> "File is ignored by version control: $resolved"
            else -> null
        }

    private fun findOrCreateVirtualFile(resolved: Path): VirtualFile? =
        findVirtualFile(resolved) ?: createVirtualFileUnderParent(resolved)

    private fun createVirtualFileUnderParent(resolved: Path): VirtualFile? {
        val parent = resolved.parent
        val parentVirtual = parent?.let(::findVirtualFile)
        return if (parent != null && parentVirtual != null) {
            runWrite {
                parentVirtual.createChildData(this, resolved.fileName.toString())
            }
        } else {
            null
        }
    }

    private fun isGitIgnored(virtualFile: VirtualFile): Boolean {
        if (project.isDisposed) {
            return false
        }
        val repository = GitRepositoryManager.getInstance(project).getRepositoryForFileQuick(virtualFile)
        return repository != null &&
            repository.ignoredFilesHolder.containsFile(VcsUtil.getFilePath(virtualFile))
    }

    private fun <T> runRead(action: () -> T): T = ApplicationManager.getApplication().runReadAction(Computable(action))

    private fun <T> runWrite(action: () -> T): T = WriteAction.compute<T, RuntimeException>(action)
}
