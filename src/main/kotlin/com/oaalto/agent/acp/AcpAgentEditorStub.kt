package com.oaalto.agent.acp

import com.intellij.codeHighlighting.BackgroundEditorHighlighter
import com.intellij.ide.structureView.StructureViewBuilder
import com.intellij.openapi.Disposable
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorLocation
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import com.oaalto.agent.AgentVirtualFile
import java.awt.BorderLayout
import java.beans.PropertyChangeListener
import java.beans.PropertyChangeSupport
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.SwingConstants

class AcpAgentEditorStub(
    private val project: Project,
    private val file: AgentVirtualFile,
) : FileEditor,
    Disposable {
    private val propertyChangeSupport = PropertyChangeSupport(this)
    private val userData = UserDataHolderBase()
    private val rootPanel =
        JPanel(BorderLayout()).apply {
            border = JBUI.Borders.empty(24)
            background = JBColor.PanelBackground
            add(
                JBLabel(
                    "<html><center>ACP Client launch mode is recognized but not yet implemented.<br>" +
                        "Full ACP client support ships in PRD 02.</center></html>",
                    SwingConstants.CENTER,
                ).apply {
                    foreground = JBColor.foreground()
                },
                BorderLayout.CENTER,
            )
        }

    override fun getComponent(): JComponent = rootPanel

    override fun getPreferredFocusedComponent(): JComponent = rootPanel

    override fun getName(): String = "Agent CLI"

    override fun getFile(): VirtualFile = file

    override fun setState(state: FileEditorState) = Unit

    override fun isModified(): Boolean = false

    override fun isValid(): Boolean = file.isValid && !project.isDisposed

    override fun selectNotify() = Unit

    override fun deselectNotify() = Unit

    override fun addPropertyChangeListener(listener: PropertyChangeListener) {
        propertyChangeSupport.addPropertyChangeListener(listener)
    }

    override fun removePropertyChangeListener(listener: PropertyChangeListener) {
        propertyChangeSupport.removePropertyChangeListener(listener)
    }

    override fun getBackgroundHighlighter(): BackgroundEditorHighlighter? = null

    override fun getCurrentLocation(): FileEditorLocation? = null

    override fun getStructureViewBuilder(): StructureViewBuilder? = null

    override fun <T : Any?> getUserData(key: Key<T>): T? = userData.getUserData(key)

    override fun <T : Any?> putUserData(
        key: Key<T>,
        value: T?,
    ) {
        userData.putUserData(key, value)
    }

    override fun dispose() = Unit
}
