package com.oaalto.agent.acp

private const val FENCE_MARKER = "```"

/**
 * Agents often emit closing fences mid-line (`code```Example`) or with trailing prose on the
 * same line (` ```Example`). CommonMark requires the closing fence on its own line.
 */
internal fun normalizeAgentFences(input: String): String {
    if (!input.contains(FENCE_MARKER)) return input
    val lines = input.lines()
    val result = ArrayList<String>(lines.size + 2)
    var inFence = false
    for (line in lines) {
        inFence =
            if (inFence) {
                appendFenceCloseLine(line, result)
            } else {
                appendOutsideFenceLine(line, result)
            }
    }
    return result.joinToString("\n")
}

private fun appendOutsideFenceLine(
    line: String,
    result: MutableList<String>,
): Boolean {
    result.add(line)
    return line.trimStart().startsWith(FENCE_MARKER)
}

private fun appendFenceCloseLine(
    line: String,
    result: MutableList<String>,
): Boolean {
    val closeIdx = line.indexOf(FENCE_MARKER)
    if (closeIdx < 0) {
        result.add(line)
        return true
    }
    val before = line.substring(0, closeIdx)
    val after = line.substring(closeIdx + FENCE_MARKER.length)
    if (before.isNotEmpty()) {
        result.add(before)
    }
    result.add(FENCE_MARKER)
    if (after.isNotEmpty()) {
        result.add(after)
    }
    return false
}
