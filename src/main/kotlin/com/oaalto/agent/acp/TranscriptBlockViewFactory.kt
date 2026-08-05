package com.oaalto.agent.acp

import com.intellij.openapi.components.serviceOrNull
import com.intellij.openapi.project.Project
import com.oaalto.agent.AgentCliLog
import com.oaalto.agent.AgentCliSessionContext
import com.oaalto.agent.acp.transcript.model.TranscriptBlock
import com.oaalto.agent.acp.transcript.theme.DefaultTranscriptColorProvider
import com.oaalto.agent.acp.transcript.theme.TranscriptColorProvider
import javax.swing.JPanel

/** Lazily accessed color provider for theme-aware colors */
private val colorProvider: TranscriptColorProvider
    get() = serviceOrNull<TranscriptColorProvider>() ?: DefaultTranscriptColorProvider()

/** Maps [TranscriptBlock] snapshots to Swing row components via adapter dispatch. */
internal class TranscriptBlockViewFactory(
    codeBlockViewFactory: TranscriptCodeBlockViewFactory,
    logContextProvider: () -> AgentCliSessionContext? = { null },
    colorProviderArg: TranscriptColorProvider = colorProvider,
    columnWidth: Int = 600,
    project: Project? = null,
) {
    private val log = AgentCliLog.getInstance(TranscriptBlockViewFactory::class.java)
    private val context =
        RowContext(
            project = project,
            columnWidth = columnWidth,
            codeBlockViewFactory = codeBlockViewFactory,
            colorProvider = colorProviderArg,
            logContextProvider = logContextProvider,
        )

    /**
     * Registered adapters in priority order.
     * More specific adapters first: Tool → Plan → Agent text → Simple text.
     */
    private val adapters =
        listOf<TranscriptBlockRowAdapter>(
            ToolCallRowAdapter(),
            PlanRowAdapter(),
            AgentTextRowAdapter(),
            SimpleTextRowAdapter(),
        )

    fun create(
        block: TranscriptBlock,
        onToolToggle: (toolCallId: String) -> Unit,
    ): JPanel {
        for (adapter in adapters) {
            if (adapter.matches(block)) {
                return adapter.create(context, block, onToolToggle)
            }
        }
        log.warn(
            "No adapter matched block: ${block::class.simpleName}",
            context = context.logContextProvider(),
        )
        // Unknown block: return empty panel to avoid crashes.
        // With current TranscriptBlock types, all variants are covered by registered adapters.
        return JPanel()
    }

    fun update(
        component: JPanel,
        block: TranscriptBlock,
    ) {
        for (adapter in adapters) {
            if (adapter.update(context, component, block)) return
        }
        log.warn(
            "Transcript block/component type mismatch: " +
                "component=${component::class.simpleName}, block=${block::class.simpleName}",
            context = context.logContextProvider(),
        )
    }

    fun disposeRow(component: JPanel) {
        val adapter = adapters.find { it.matches(component) } ?: return
        adapter.dispose(context, component)
    }

    private fun TranscriptBlockRowAdapter.matches(component: JPanel): Boolean =
        when (this) {
            is ToolCallRowAdapter -> isToolCallRow(component)
            is PlanRowAdapter -> isPlanRow(component)
            is AgentTextRowAdapter -> isAgentTextRow(component)
            is SimpleTextRowAdapter -> isSimpleTextRow(component)
            else -> false
        }

    companion object {
        fun escapeHtml(text: String): String = TranscriptRenderHelpers.escapeHtml(text)

        fun tryOpenUrl(url: String) = TranscriptBodyPartWidgetMapper.tryOpenUrl(url)
    }
}
