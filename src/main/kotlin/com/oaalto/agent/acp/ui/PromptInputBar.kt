package com.oaalto.agent.acp.ui

import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.PopupChooserBuilder
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.JBUI
import com.oaalto.agent.acp.SlashCommand
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.Point
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import javax.swing.DefaultListCellRenderer
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class PromptInputBar(
    private val onSubmit: (String) -> Unit,
) {
    private val inputField =
        JBTextField().apply {
            addKeyListener(
                object : KeyAdapter() {
                    override fun keyPressed(event: KeyEvent) {
                        when {
                            event.keyCode == KeyEvent.VK_ENTER && !event.isShiftDown -> {
                                event.consume()
                                submitCurrentText()
                            }
                            event.keyCode == KeyEvent.VK_TAB && !event.isShiftDown -> {
                                if (completeSlashCommand()) {
                                    event.consume()
                                }
                            }
                            event.keyCode == KeyEvent.VK_DOWN -> moveHighlightedCommand(1, event)
                            event.keyCode == KeyEvent.VK_UP -> moveHighlightedCommand(-1, event)
                            event.keyCode == KeyEvent.VK_ESCAPE -> {
                                if (commandPopup?.isDisposed == false) {
                                    dismissCommandPopup(resetSelection = false)
                                    event.consume()
                                }
                            }
                        }
                    }
                },
            )
            document.addDocumentListener(
                object : DocumentListener {
                    override fun insertUpdate(event: DocumentEvent) = refreshSlashCommandPopup()

                    override fun removeUpdate(event: DocumentEvent) = refreshSlashCommandPopup()

                    override fun changedUpdate(event: DocumentEvent) = refreshSlashCommandPopup()
                },
            )
        }
    private val panel =
        JPanel(BorderLayout()).apply {
            border = JBUI.Borders.empty(8, 12, 8, 12)
            add(inputField, BorderLayout.CENTER)
        }
    private var availableCommands: List<SlashCommand> = emptyList()
    private var commandPopup: JBPopup? = null
    private var commandList: JBList<SlashCommand>? = null
    private var highlightedCommandIndex = 0

    val component: JComponent
        get() = panel

    fun requestFocus() {
        inputField.requestFocusInWindow()
    }

    fun setEnabled(enabled: Boolean) {
        inputField.isEnabled = enabled
    }

    fun setAvailableCommands(commands: List<SlashCommand>) {
        availableCommands = commands
        updatePlaceholder()
        refreshSlashCommandPopup()
    }

    private fun submitCurrentText() {
        if (!inputField.isEnabled) return
        val text = inputField.text.trim()
        if (text.isEmpty()) return
        dismissCommandPopup()
        inputField.text = ""
        onSubmit(text)
    }

    private fun updatePlaceholder() {
        inputField.emptyText.text =
            when {
                availableCommands.isEmpty() -> "Send a prompt (Enter to submit)"
                else -> {
                    val preview =
                        availableCommands
                            .take(PLACEHOLDER_PREVIEW_COUNT)
                            .joinToString(", ") { "/${it.name}" }
                    "Send a prompt or $preview (Tab to complete)"
                }
            }
    }

    private fun refreshSlashCommandPopup() {
        if (!inputField.isEnabled) {
            dismissCommandPopup()
            return
        }
        val matches = SlashCommandMatcher.filter(availableCommands, inputField.text)
        if (matches.isEmpty()) {
            dismissCommandPopup()
            return
        }
        highlightedCommandIndex = highlightedCommandIndex.coerceIn(0, matches.lastIndex)
        if (commandPopup?.isDisposed != false) {
            showCommandPopup(matches)
        } else {
            updateCommandPopup(matches)
        }
    }

    private fun moveHighlightedCommand(
        delta: Int,
        event: KeyEvent,
    ) {
        if (commandPopup?.isDisposed != false) return
        val matches = SlashCommandMatcher.filter(availableCommands, inputField.text)
        if (matches.isEmpty()) return
        highlightedCommandIndex = (highlightedCommandIndex + delta).coerceIn(0, matches.lastIndex)
        updateCommandPopup(matches)
        event.consume()
    }

    private fun showCommandPopup(matches: List<SlashCommand>) {
        dismissCommandPopup(resetSelection = false)
        val popupWidth = inputField.width.coerceAtLeast(inputField.preferredSize.width)
        val list =
            JBList(matches).apply {
                isFocusable = false
                selectedIndex = highlightedCommandIndex
                visibleRowCount = matches.size.coerceAtMost(MAX_VISIBLE_COMMANDS)
                fixedCellWidth = popupWidth
                cellRenderer =
                    object : DefaultListCellRenderer() {
                        override fun getListCellRendererComponent(
                            list: javax.swing.JList<*>,
                            value: Any?,
                            index: Int,
                            isSelected: Boolean,
                            cellHasFocus: Boolean,
                        ) = super.getListCellRendererComponent(list, value, index, isSelected, false).apply {
                            val command = value as SlashCommand
                            text = "/${command.name} — ${command.description}"
                        }
                    }
            }
        commandList = list
        commandPopup =
            PopupChooserBuilder(list)
                .setRequestFocus(false)
                .setMinSize(Dimension(popupWidth, 0))
                .setItemChosenCallback { chosen ->
                    applySlashCommand(chosen as SlashCommand)
                }.setCancelCallback {
                    commandPopup = null
                    true
                }.createPopup()
                .also { popup ->
                    popup.pack(true, true)
                    val popupHeight = popup.content.preferredSize.height
                    popup.show(RelativePoint(inputField, Point(0, -popupHeight)))
                    inputField.requestFocusInWindow()
                }
    }

    private fun updateCommandPopup(matches: List<SlashCommand>) {
        val list = commandList ?: return
        list.setListData(matches.toTypedArray())
        list.selectedIndex = highlightedCommandIndex
        list.ensureIndexIsVisible(highlightedCommandIndex)
        commandPopup?.pack(true, true)
    }

    private fun dismissCommandPopup(resetSelection: Boolean = true) {
        commandPopup?.cancel()
        commandPopup = null
        commandList = null
        if (resetSelection) {
            highlightedCommandIndex = 0
        }
    }

    private fun completeSlashCommand(): Boolean {
        val matches = SlashCommandMatcher.filter(availableCommands, inputField.text)
        if (matches.isEmpty()) return false
        val command = matches[highlightedCommandIndex.coerceIn(0, matches.lastIndex)]
        applySlashCommand(command)
        return true
    }

    private fun applySlashCommand(command: SlashCommand) {
        val suffix = if (command.inputHint.isNullOrBlank()) "" else " "
        inputField.text = "/${command.name}$suffix"
        inputField.caretPosition = inputField.text.length
        dismissCommandPopup()
        inputField.requestFocusInWindow()
    }

    companion object {
        private const val PLACEHOLDER_PREVIEW_COUNT = 3
        private const val MAX_VISIBLE_COMMANDS = 5
    }
}
