package com.oaalto.agent.acp

import com.intellij.openapi.project.Project
import com.oaalto.agent.AgentCliSessionContext
import com.oaalto.agent.acp.transcript.theme.TranscriptColorProvider
import javax.swing.JPanel

/**
 * Session-wide dependencies needed by row adapters.
 *
 * [onToolToggle] is intentionally NOT on RowContext; callers pass it separately to [create].
 */
internal data class RowContext(
    val project: Project? = null,
    val columnWidth: Int = 600,
    val codeBlockViewFactory: TranscriptCodeBlockViewFactory,
    val colorProvider: TranscriptColorProvider,
    val logContextProvider: () -> AgentCliSessionContext?,
)

/**
 * Adapter responsible for one family of transcript block types.
 *
 * The coordinator iterates adapters in registration order and uses the first adapter whose
 * [matches] returns true for a given block.
 */
internal interface TranscriptBlockRowAdapter {
    /** Returns true when this adapter can handle the given block type. */
    fun matches(block: TranscriptBlock): Boolean

    /** Create a new row UI for [block]. */
    fun create(
        context: RowContext,
        block: TranscriptBlock,
        onToolToggle: (String) -> Unit,
    ): JPanel

    /**
     * Update an existing row in place.
     *
     * Returns true when the update was accepted (same block family), false on a type mismatch.
     * On mismatch the adapter should log and skip, preserving the current panel state.
     */
    fun update(
        context: RowContext,
        row: JPanel,
        block: TranscriptBlock,
    ): Boolean

    /** Dispose any disposable resources held by [row]. */
    fun dispose(
        context: RowContext,
        row: JPanel,
    )
}
