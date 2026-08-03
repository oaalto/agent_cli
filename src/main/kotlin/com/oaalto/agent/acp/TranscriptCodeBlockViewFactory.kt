package com.oaalto.agent.acp

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.project.Project
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTextArea

private const val CODE_BLOCK_LEFT_INSET = 20
private const val MONO_FONT_SIZE = 12
internal const val TRANSCRIPT_CODE_BLOCK_MARKER = "transcript.codeBlock"
internal const val TRANSCRIPT_CODE_BLOCK_EDITOR_KEY = "transcript.codeBlock.editor"

internal fun isTranscriptCodeBlock(component: JComponent): Boolean =
    component.getClientProperty(TRANSCRIPT_CODE_BLOCK_MARKER) == true

/** Reflows embedded Editor or JTextArea code blocks after transcript column resize. */
internal fun applyTranscriptCodeBlockWidth(
    component: JComponent,
    width: Int,
): Boolean {
    if (!isTranscriptCodeBlock(component)) {
        return false
    }
    val editor =
        component.getClientProperty(TRANSCRIPT_CODE_BLOCK_EDITOR_KEY) as? Editor
    if (editor != null) {
        val editorEx = editor as EditorEx
        editorEx.settings.isUseSoftWraps = true
        val editorComponent = editor.component
        editorComponent.setSize(width, Int.MAX_VALUE)
        val height = editorComponent.preferredSize.height
        component.setSize(width, height)
        component.preferredSize = Dimension(width, height)
        component.maximumSize = Dimension(Int.MAX_VALUE, height)
        return true
    }
    if (component is JTextArea) {
        component.setSize(width, Int.MAX_VALUE)
        val height = component.preferredSize.height
        component.preferredSize = Dimension(width, height)
        component.maximumSize = Dimension(Int.MAX_VALUE, height)
        return true
    }
    return false
}

/** Creates read-only highlighted code blocks for transcript rows and tool card bodies. */
internal interface TranscriptCodeBlockViewFactory {
    fun createReadOnlyCodeBlock(
        languageId: String?,
        code: String,
    ): JComponent

    fun dispose(component: JComponent)
}

internal class EditorFactoryTranscriptCodeBlockViewFactory(
    private val project: Project,
) : TranscriptCodeBlockViewFactory {
    override fun createReadOnlyCodeBlock(
        languageId: String?,
        code: String,
    ): JComponent {
        val fileType = TranscriptFenceLanguageResolver.resolveFileType(languageId)
        val document = EditorFactory.getInstance().createDocument(code)
        val editor =
            EditorFactory.getInstance().createEditor(document, project, fileType, false).also { created ->
                val editorEx = created as EditorEx
                editorEx.isViewer = true
                editorEx.settings.isWheelFontChangeEnabled = false
                editorEx.settings.additionalLinesCount = 0
                editorEx.settings.additionalColumnsCount = 0
                editorEx.settings.isUseSoftWraps = true
                editorEx.component.isFocusable = false
            }
        return JPanel(BorderLayout()).apply {
            isOpaque = false
            border = JBUI.Borders.emptyLeft(CODE_BLOCK_LEFT_INSET)
            putClientProperty(TRANSCRIPT_CODE_BLOCK_MARKER, true)
            putClientProperty(TRANSCRIPT_CODE_BLOCK_EDITOR_KEY, editor)
            add(editor.component, BorderLayout.CENTER)
            preferredSize = editor.component.preferredSize
            maximumSize = Dimension(Int.MAX_VALUE, preferredSize.height)
        }
    }

    override fun dispose(component: JComponent) {
        val editor = component.getClientProperty(TRANSCRIPT_CODE_BLOCK_EDITOR_KEY) as? Editor ?: return
        EditorFactory.getInstance().releaseEditor(editor)
        component.putClientProperty(TRANSCRIPT_CODE_BLOCK_EDITOR_KEY, null)
    }
}

/** Headless-safe fallback used in unit tests and when editor creation is unavailable. */
internal object PlainMonospaceTranscriptCodeBlockViewFactory : TranscriptCodeBlockViewFactory {
    override fun createReadOnlyCodeBlock(
        languageId: String?,
        code: String,
    ): JComponent =
        JTextArea(code).apply {
            isEditable = false
            isOpaque = false
            font = java.awt.Font("Monospaced", java.awt.Font.PLAIN, MONO_FONT_SIZE)
            border = JBUI.Borders.emptyLeft(CODE_BLOCK_LEFT_INSET)
            lineWrap = true
            wrapStyleWord = true
            putClientProperty(TRANSCRIPT_CODE_BLOCK_MARKER, true)
        }

    override fun dispose(component: JComponent) = Unit
}
