package com.oaalto.agent.acp

private const val FENCE_MARKER = "```"

// Longest-first so `javascript` wins over `java`.
private val FENCE_LANGUAGE_PREFIXES: List<String> =
    listOf(
        "dockerfile",
        "javascript",
        "typescript",
        "markdown",
        "kotlin",
        "python",
        "shell",
        "bash",
        "yaml",
        "java",
        "rust",
        "ruby",
        "swift",
        "scala",
        "groovy",
        "csharp",
        "docker",
        "html",
        "css",
        "json",
        "xml",
        "sql",
        "cpp",
        "php",
        "sh",
        "py",
        "js",
        "ts",
        "kt",
        "rb",
        "rs",
        "go",
        "c",
    ).sortedByDescending { it.length }

/**
 * Agents often emit closing fences mid-line (`code```Example`) or with trailing prose on the
 * same line (` ```Example`). Opening fences may omit the newline after the language tag
 * (` ```kotlinfun main()`). CommonMark requires fences on their own lines.
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
    if (inFence) {
        result.add(FENCE_MARKER)
    }
    return result.joinToString("\n")
}

private fun appendOutsideFenceLine(
    line: String,
    result: MutableList<String>,
): Boolean {
    val fenceIdx = line.indexOf(FENCE_MARKER)
    if (fenceIdx < 0) {
        result.add(line)
        return false
    }
    if (fenceIdx > 0) {
        result.add(line.substring(0, fenceIdx))
        return appendOutsideFenceLine(line.substring(fenceIdx), result)
    }
    return appendOpeningFenceLine(line, result)
}

private fun appendOpeningFenceLine(
    line: String,
    result: MutableList<String>,
): Boolean {
    val trimmedStart = line.trimStart()
    val indent = line.substring(0, line.length - trimmedStart.length)
    val afterFence = trimmedStart.removePrefix(FENCE_MARKER)
    return when {
        afterFence.isEmpty() || afterFence.matches(Regex("""[\w+#.-]+""")) -> {
            result.add("$indent$FENCE_MARKER$afterFence")
            true
        }
        else -> {
            val split = splitOpeningFenceInfo(afterFence)
            if (split == null) {
                result.add(line)
                false
            } else {
                val (lang, codeLine) = split
                result.add("$indent$FENCE_MARKER$lang")
                if (codeLine.contains(FENCE_MARKER)) {
                    appendFenceCloseLine("$indent$codeLine", result)
                } else {
                    result.add("$indent$codeLine")
                    true
                }
            }
        }
    }
}

private fun splitOpeningFenceInfo(info: String): Pair<String, String>? {
    val lower = info.lowercase()
    for (lang in FENCE_LANGUAGE_PREFIXES) {
        if (lower.startsWith(lang) && info.length > lang.length) {
            return lang to info.substring(lang.length)
        }
    }
    return null
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
