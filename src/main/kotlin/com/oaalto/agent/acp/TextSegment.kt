package com.oaalto.agent.acp

/** Prose or fenced-code span produced by [segmentFencedCodeBlocks]. */
internal sealed class TextSegment {
    data class Prose(
        val text: String,
    ) : TextSegment()

    data class Code(
        val text: String,
        val languageId: String?,
    ) : TextSegment()
}

private const val FENCE_MARKER = "```"
private const val FENCE_MARKER_LENGTH = FENCE_MARKER.length

/**
 * Splits [input] into prose and GitHub-style fenced code regions.
 *
 * An unclosed opening fence treats the remainder as prose (streaming-safe).
 */
internal fun segmentFencedCodeBlocks(input: String): List<TextSegment> {
    if (input.isEmpty()) {
        return emptyList()
    }
    val segments = mutableListOf<TextSegment>()
    var index = 0
    while (index < input.length) {
        val fenceStart = input.indexOf(FENCE_MARKER, index)
        if (fenceStart < 0) {
            appendProseSegment(segments, input.substring(index))
            return segments
        }
        appendProseSegment(segments, input.substring(index, fenceStart))
        val nextIndex = parseClosedFence(input, fenceStart, segments) ?: return segments
        index = nextIndex
    }
    return segments
}

private fun parseClosedFence(
    input: String,
    fenceStart: Int,
    segments: MutableList<TextSegment>,
): Int? {
    val openerEnd = input.indexOf('\n', fenceStart + FENCE_MARKER_LENGTH).let { if (it < 0) input.length else it }
    val languageId =
        input
            .substring(fenceStart + FENCE_MARKER_LENGTH, openerEnd)
            .trim()
            .takeIf { it.isNotEmpty() }
    val contentStart = if (openerEnd < input.length) openerEnd + 1 else input.length
    val closeFence = input.indexOf(FENCE_MARKER, contentStart)
    if (closeFence < 0) {
        appendProseSegment(segments, input.substring(fenceStart))
        return null
    }
    val rawCode = input.substring(contentStart, closeFence)
    val codeText = rawCode.removeSuffix("\n").removeSuffix("\r\n")
    segments.add(TextSegment.Code(codeText, languageId))
    var nextIndex = closeFence + FENCE_MARKER_LENGTH
    if (nextIndex < input.length && input[nextIndex] == '\n') {
        nextIndex += 1
    }
    return nextIndex
}

private fun appendProseSegment(
    segments: MutableList<TextSegment>,
    text: String,
) {
    if (text.isNotEmpty()) {
        segments.add(TextSegment.Prose(text))
    }
}

internal fun List<TextSegment>.containsCode(): Boolean = any { it is TextSegment.Code }
