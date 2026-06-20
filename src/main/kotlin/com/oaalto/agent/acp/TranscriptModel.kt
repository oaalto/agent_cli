package com.oaalto.agent.acp

/**
 * Ordered transcript blocks with tool deduplication and per-card expansion state.
 */
internal class TranscriptModel {
    private val blocks = mutableListOf<TranscriptBlock>()
    private val toolBlockIndexById = mutableMapOf<String, Int>()
    private var nextBlockId = 0

    fun blocks(): List<TranscriptBlock> = blocks.toList()

    fun apply(update: StructuredUpdate) {
        when (update) {
            is StructuredUpdate.AppendAgentText -> appendAgentText(update.text)
            StructuredUpdate.FinalizeAgentStream -> finalizeAgentStream()
            is StructuredUpdate.AppendUserEcho -> appendBlock(TranscriptBlock.UserEcho(allocBlockId(), update.text))
            is StructuredUpdate.AppendThought -> appendBlock(TranscriptBlock.Thought(allocBlockId(), update.text))
            is StructuredUpdate.AppendPlainLine ->
                appendBlock(
                    TranscriptBlock.PlainLine(
                        blockId = allocBlockId(),
                        text = update.line,
                        isUserPrompt = update.isUserPrompt,
                    ),
                )
            is StructuredUpdate.AppendError ->
                appendBlock(TranscriptBlock.ErrorLine(allocBlockId(), update.message))
            is StructuredUpdate.AppendAuthFailure ->
                appendBlock(TranscriptBlock.AuthFailureLine(allocBlockId(), update.message))
            is StructuredUpdate.StartOrUpdateToolCall -> startOrUpdateToolCall(update)
        }
    }

    fun toggleToolExpansion(toolCallId: String) {
        val index = toolBlockIndexById[toolCallId] ?: return
        val block = blocks.getOrNull(index) as? TranscriptBlock.ToolCallBlock ?: return
        if (!block.hasBodyContent) return
        blocks[index] = block.copy(expanded = !block.expanded)
    }

    private fun appendAgentText(text: String) {
        if (text.isBlank()) return
        val last = blocks.lastOrNull()
        if (last is TranscriptBlock.StreamingAgentText) {
            blocks[blocks.lastIndex] = last.copy(text = last.text + text)
            return
        }
        appendBlock(TranscriptBlock.StreamingAgentText(allocBlockId(), text))
    }

    private fun finalizeAgentStream() {
        val lastIndex = blocks.lastIndex
        if (lastIndex < 0) return
        val last = blocks[lastIndex]
        if (last !is TranscriptBlock.StreamingAgentText) return
        blocks[lastIndex] =
            TranscriptBlock.FinalAgentText(
                blockId = last.blockId,
                text = last.text,
            )
    }

    private fun startOrUpdateToolCall(update: StructuredUpdate.StartOrUpdateToolCall) {
        val existingIndex = toolBlockIndexById[update.toolCallId]
        if (existingIndex != null) {
            val existing = blocks.getOrNull(existingIndex) as? TranscriptBlock.ToolCallBlock ?: return
            val mergedFragments =
                if (update.contentFragments.isEmpty()) {
                    existing.contentFragments
                } else {
                    existing.contentFragments + update.contentFragments
                }
            blocks[existingIndex] =
                existing.copy(
                    title = update.title,
                    kind = update.kind ?: existing.kind,
                    status = update.status ?: existing.status,
                    contentFragments = mergedFragments,
                )
            return
        }
        val block =
            TranscriptBlock.ToolCallBlock(
                blockId = allocBlockId(),
                toolCallId = update.toolCallId,
                title = update.title,
                kind = update.kind,
                status = update.status,
                contentFragments = update.contentFragments,
            )
        toolBlockIndexById[update.toolCallId] = blocks.size
        appendBlock(block)
    }

    private fun appendBlock(block: TranscriptBlock) {
        blocks.add(block)
    }

    private fun allocBlockId(): String {
        nextBlockId += 1
        return "block-$nextBlockId"
    }
}
