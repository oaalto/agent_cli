package com.oaalto.agent.acp.transcript.view.rows

import com.oaalto.agent.AgentCliLog
import com.oaalto.agent.acp.transcript.model.TranscriptBlock
import com.oaalto.agent.acp.transcript.view.CollapsibleToolPanel
import com.oaalto.agent.acp.transcript.view.RowContext
import com.oaalto.agent.acp.transcript.view.TranscriptBlockRowAdapter
import javax.swing.JPanel

/** Client property marker for panels created by ToolCallRowAdapter. */
internal const val TOOL_CALL_ROW_MARKER = "transcript.toolCallRow"

/** Check if a panel was created by ToolCallRowAdapter. */
internal fun isToolCallRow(panel: JPanel): Boolean = panel.getClientProperty(TOOL_CALL_ROW_MARKER) == true

/**
 * Adapter for [TranscriptBlock.ToolCallBlock].
 *
 * Wraps [CollapsibleToolPanel] and delegates presentation to it.
 * `onToolToggle` is passed at construction time, not via [RowContext].
 */
internal class ToolCallRowAdapter : TranscriptBlockRowAdapter {
    private val log = AgentCliLog.getInstance(ToolCallRowAdapter::class.java)

    override fun matches(block: TranscriptBlock): Boolean = block is TranscriptBlock.ToolCallBlock

    override fun create(
        context: RowContext,
        block: TranscriptBlock,
        onToolToggle: (String) -> Unit,
    ): JPanel {
        require(block is TranscriptBlock.ToolCallBlock) { "Expected ToolCallBlock, got ${block::class.simpleName}" }
        return CollapsibleToolPanel(onToolToggle, context.codeBlockViewFactory).apply {
            putClientProperty(TOOL_CALL_ROW_MARKER, true)
            bind(block)
        }
    }

    override fun update(
        context: RowContext,
        row: JPanel,
        block: TranscriptBlock,
    ): Boolean {
        val toolBlock = block as? TranscriptBlock.ToolCallBlock ?: return false
        if (row is CollapsibleToolPanel) {
            row.bind(toolBlock)
            return true
        }
        log.warn(
            "ToolCallRowAdapter update mismatch: " +
                "component=${row::class.simpleName} is not CollapsibleToolPanel, block=ToolCallBlock",
            context = context.logContextProvider(),
        )
        return false
    }

    override fun dispose(
        context: RowContext,
        row: JPanel,
    ) {
        if (row is CollapsibleToolPanel) {
            row.disposeCodeComponents()
        }
    }
}
