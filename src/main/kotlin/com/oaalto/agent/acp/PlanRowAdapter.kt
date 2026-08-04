package com.oaalto.agent.acp

import com.oaalto.agent.AgentCliLog
import com.oaalto.agent.acp.plan.PlanPanel
import javax.swing.JPanel

/** Client property marker for panels created by PlanRowAdapter. */
internal const val PLAN_ROW_MARKER = "transcript.planRow"

/** Check if a panel was created by PlanRowAdapter. */
internal fun isPlanRow(panel: JPanel): Boolean = panel.getClientProperty(PLAN_ROW_MARKER) == true

/**
 * Adapter for [TranscriptBlock.PlanBlock].
 *
 * Thin wrapper around [PlanPanel]; delegates all presentation to the existing panel.
 * Plan layout constants remain colocated with PlanPanel, not here.
 */
internal class PlanRowAdapter : TranscriptBlockRowAdapter {
    private val log = AgentCliLog.getInstance(PlanRowAdapter::class.java)

    override fun matches(block: TranscriptBlock): Boolean = block is TranscriptBlock.PlanBlock

    override fun create(
        context: RowContext,
        block: TranscriptBlock,
        onToolToggle: (String) -> Unit,
    ): JPanel {
        require(block is TranscriptBlock.PlanBlock) { "Expected PlanBlock, got ${block::class.simpleName}" }
        return PlanPanel().apply {
            putClientProperty(PLAN_ROW_MARKER, true)
            bind(block)
        }
    }

    override fun update(
        context: RowContext,
        row: JPanel,
        block: TranscriptBlock,
    ): Boolean {
        val planBlock = block as? TranscriptBlock.PlanBlock ?: return false
        if (row is PlanPanel) {
            row.bind(planBlock)
            return true
        }
        log.warn(
            "PlanRowAdapter update mismatch: " +
                "component=${row::class.simpleName} is not PlanPanel, block=PlanBlock",
            context = context.logContextProvider(),
        )
        return false
    }

    override fun dispose(
        context: RowContext,
        row: JPanel,
    ) {
        if (row is PlanPanel) {
            row.dispose()
        }
    }
}
