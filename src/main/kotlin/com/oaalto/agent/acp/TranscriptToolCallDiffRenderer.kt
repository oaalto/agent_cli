package com.oaalto.agent.acp

import com.agentclientprotocol.model.ToolCallContent

internal object TranscriptToolCallDiffRenderer {
    private const val DIFF_PATH_STYLE = "color:#aaaaaa"
    private const val DIFF_REMOVED_STYLE = "color:#c43c3c"
    private const val DIFF_ADDED_STYLE = "color:#2d8a4e"

    fun render(
        diff: ToolCallContent.Diff,
        preStyle: String,
    ): String {
        val escapedPath = TranscriptRenderHelpers.escapeHtml(diff.path)
        val body =
            buildString {
                append("<span style=\"$DIFF_PATH_STYLE\">$escapedPath</span>\n")
                append(
                    renderLineDiff(
                        textLines(diff.oldText),
                        textLines(diff.newText),
                    ),
                )
            }
        return "<pre style=\"$preStyle\">$body</pre>"
    }

    private fun textLines(text: String?): List<String> =
        if (text.isNullOrEmpty()) {
            emptyList()
        } else {
            text.lines()
        }

    private fun renderLineDiff(
        oldLines: List<String>,
        newLines: List<String>,
    ): String =
        buildString {
            val matches = longestCommonSubsequenceMatches(oldLines, newLines)
            var oldIndex = 0
            var newIndex = 0
            for ((matchedOldIndex, matchedNewIndex) in matches) {
                oldIndex = appendRemovedLinesUntil(this, oldLines, oldIndex, matchedOldIndex)
                newIndex = appendAddedLinesUntil(this, newLines, newIndex, matchedNewIndex)
                oldIndex = matchedOldIndex + 1
                newIndex = matchedNewIndex + 1
            }
            appendRemovedLinesUntil(this, oldLines, oldIndex, oldLines.size)
            appendAddedLinesUntil(this, newLines, newIndex, newLines.size)
        }

    private fun appendRemovedLinesUntil(
        builder: StringBuilder,
        oldLines: List<String>,
        fromIndex: Int,
        untilIndex: Int,
    ): Int {
        var index = fromIndex
        while (index < untilIndex) {
            builder.appendDiffLine(DIFF_REMOVED_STYLE, "-", oldLines[index])
            index++
        }
        return index
    }

    private fun appendAddedLinesUntil(
        builder: StringBuilder,
        newLines: List<String>,
        fromIndex: Int,
        untilIndex: Int,
    ): Int {
        var index = fromIndex
        while (index < untilIndex) {
            builder.appendDiffLine(DIFF_ADDED_STYLE, "+", newLines[index])
            index++
        }
        return index
    }

    private fun longestCommonSubsequenceMatches(
        oldLines: List<String>,
        newLines: List<String>,
    ): List<Pair<Int, Int>> {
        val lengths = lcsLengths(oldLines, newLines)
        return backtrackLcsMatches(oldLines, newLines, lengths)
    }

    private fun lcsLengths(
        oldLines: List<String>,
        newLines: List<String>,
    ): Array<IntArray> {
        val table = Array(oldLines.size + 1) { IntArray(newLines.size + 1) }
        for (oldIndex in 1..oldLines.size) {
            for (newIndex in 1..newLines.size) {
                table[oldIndex][newIndex] =
                    if (oldLines[oldIndex - 1] == newLines[newIndex - 1]) {
                        table[oldIndex - 1][newIndex - 1] + 1
                    } else {
                        maxOf(
                            table[oldIndex - 1][newIndex],
                            table[oldIndex][newIndex - 1],
                        )
                    }
            }
        }
        return table
    }

    private fun backtrackLcsMatches(
        oldLines: List<String>,
        newLines: List<String>,
        lengths: Array<IntArray>,
    ): List<Pair<Int, Int>> {
        val matches = ArrayDeque<Pair<Int, Int>>()
        var oldIndex = oldLines.size
        var newIndex = newLines.size
        while (oldIndex > 0 && newIndex > 0) {
            when {
                oldLines[oldIndex - 1] == newLines[newIndex - 1] -> {
                    matches.addFirst(oldIndex - 1 to newIndex - 1)
                    oldIndex--
                    newIndex--
                }
                lengths[oldIndex - 1][newIndex] >= lengths[oldIndex][newIndex - 1] -> {
                    oldIndex--
                }
                else -> {
                    newIndex--
                }
            }
        }
        return matches.toList()
    }

    private fun StringBuilder.appendDiffLine(
        colorStyle: String,
        prefix: String,
        line: String,
    ) {
        append("<span style=\"$colorStyle\">$prefix ${TranscriptRenderHelpers.escapeHtml(line)}</span>\n")
    }
}
