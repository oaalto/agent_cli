package com.oaalto.agent.acp

import com.agentclientprotocol.model.SessionUpdate
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBScrollPane
import java.awt.Adjustable
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JScrollBar
import javax.swing.JScrollPane
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Mounted-panel integration seam for ACP transcript UI regressions.
 *
 * Default path: [StructuredUpdate] → [TranscriptModel] → [TranscriptPanel.sync].
 * Use [syncBlocks] for pure view-layer regressions with pre-built block lists.
 * Use [applyIngest] when finalize-before-non-chunk ingestion policy must be exercised.
 */
internal class TranscriptPanelTestHarness(
    project: Project = fakeTranscriptProject(),
    columnWidth: Int = 400,
    viewportHeight: Int = 300,
) {
    private val viewController: TranscriptViewController
    val panel: TranscriptPanel
    val scrollPane: JBScrollPane

    init {
        lateinit var controller: TranscriptViewController
        lateinit var mountedPanel: TranscriptPanel
        runOnEdtSync {
            controller =
                TranscriptViewController(
                    project = project,
                    runOnEdt = { action -> runOnEdtSync(action) },
                    codeBlockViewFactory = PlainMonospaceTranscriptCodeBlockViewFactory,
                )
            mountedPanel = controller.panelForTest()
            val pane = mountedPanel.component as JBScrollPane
            pane.setSize(columnWidth, viewportHeight)
            pane.doLayout()
        }
        viewController = controller
        panel = mountedPanel
        scrollPane = panel.component as JBScrollPane
        pumpEdt()
    }

    fun apply(vararg updates: StructuredUpdate) = apply(updates.toList())

    fun apply(updates: List<StructuredUpdate>) {
        updates.forEach(viewController::apply)
        pumpEdt()
    }

    /** SessionUpdate → ingestion finalize policy → ViewController apply. */
    fun applyIngest(update: SessionUpdate) {
        TranscriptEventIngestion.ingest(update).forEach(viewController::apply)
        pumpEdt()
    }

    fun syncBlocks(blocks: List<TranscriptBlock>) {
        runOnEdtSync { panel.sync(blocks) }
        pumpEdt()
    }

    fun setColumnWidth(width: Int) {
        runOnEdtSync {
            scrollPane.setSize(width, scrollPane.height)
            scrollPane.doLayout()
        }
        pumpEdt()
    }

    fun pumpEdt() = pumpTranscriptEdt()

    fun blocks(): List<TranscriptBlock> = viewController.blocksForTest()

    fun scrollViewportToBottom() {
        runOnEdtSync {
            val bar = scrollPane.verticalScrollBar
            bar.value = bar.maximum
        }
        pumpEdt()
    }

    fun scrollViewportToTop() {
        runOnEdtSync {
            scrollPane.verticalScrollBar.value = 0
        }
        pumpEdt()
    }

    fun verticalScrollBarValue(): Int {
        var value = 0
        runOnEdtSync { value = scrollPane.verticalScrollBar.value }
        return value
    }

    fun assertRowCount(expected: Int) {
        runOnEdtSync {
            assertEquals(expected, transcriptColumn().componentCount, "transcript row count")
        }
    }

    fun assertNoHorizontalScrollbar() {
        runOnEdtSync {
            val horizontalBars =
                collectTranscriptDescendants(panel, JScrollBar::class.java)
                    .filter { it.orientation == Adjustable.HORIZONTAL }
            assertTrue(
                horizontalBars.none { it.isVisible && it.width > 0 && it.height > 0 },
                "visible horizontal scrollbars in transcript tree: ${horizontalBars.size}",
            )
            collectTranscriptDescendants(panel, JScrollPane::class.java).forEach { pane ->
                assertEquals(
                    JScrollPane.HORIZONTAL_SCROLLBAR_NEVER,
                    pane.horizontalScrollBarPolicy,
                    "scroll pane ${pane.name} must not enable horizontal scrolling",
                )
            }
        }
    }

    fun assertViewportShowsBottom(tolerance: Int = 4) {
        runOnEdtSync {
            val bar = scrollPane.verticalScrollBar
            assertTrue(
                bar.maximum <= bar.visibleAmount ||
                    bar.value + bar.visibleAmount >= bar.maximum - tolerance,
                "viewport not at bottom: value=${bar.value} visible=${bar.visibleAmount} max=${bar.maximum}",
            )
        }
    }

    fun assertSingleTranscriptScrollPane() {
        runOnEdtSync {
            val verticalOwners =
                collectTranscriptDescendants(panel, JScrollPane::class.java)
                    .filter { it.verticalScrollBarPolicy != JScrollPane.VERTICAL_SCROLLBAR_NEVER }
            assertEquals(
                1,
                verticalOwners.size,
                "expected exactly one scroll pane owning vertical scrolling",
            )
            assertSame(
                scrollPane,
                verticalOwners.single(),
                "transcript root scroll pane must own vertical scrolling",
            )
        }
    }

    fun codeBlockPreferredHeights(): List<Int> {
        val holder = mutableListOf<Int>()
        runOnEdtSync {
            holder.addAll(codeBlockRowsOnEdt().map { it.preferredSize.height })
        }
        return holder
    }

    fun rowPreferredHeights(): List<Int> {
        val holder = mutableListOf<Int>()
        runOnEdtSync {
            holder.addAll(transcriptColumn().components.map { it.preferredSize.height })
        }
        return holder
    }

    fun dispose() {
        viewController.dispose()
    }

    private fun transcriptColumn(): JPanel {
        val viewport = scrollPane.viewport
        return viewport.view as JPanel
    }

    private fun codeBlockRowsOnEdt(): List<JComponent> =
        collectTranscriptDescendants(transcriptColumn(), JComponent::class.java)
            .filter(::isTranscriptCodeBlock)
}
