package com.oaalto.agent.acp

private const val FENCE_MARKER = "```"

/** Cursor citation fences: ` ```3:10:path/File.ktclass Foo(` — path extension is the language. */
private val CITATION_KNOWN_EXTENSIONS =
    listOf(
        "kotlin",
        "dockerfile",
        "javascript",
        "typescript",
        "markdown",
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
        "json",
        "kts",
        "cpp",
        "php",
        "css",
        "xml",
        "sql",
        "kt",
        "sh",
        "py",
        "js",
        "ts",
        "rb",
        "rs",
        "go",
        "md",
        "yml",
        "c",
    ).sortedByDescending { it.length }

private val CITATION_FENCE_PREFIX =
    Regex(
        """^\d+:\d+:.+\.(${CITATION_KNOWN_EXTENSIONS.joinToString("|") { Regex.escape(it) }})(.*)$""",
        RegexOption.IGNORE_CASE,
    )

private val CITATION_EXTENSION_TO_LANG =
    mapOf(
        "kt" to "kotlin",
        "kts" to "kotlin",
        "java" to "java",
        "py" to "python",
        "js" to "javascript",
        "ts" to "typescript",
        "go" to "go",
        "rs" to "rust",
        "rb" to "ruby",
        "sh" to "shell",
        "md" to "markdown",
        "yml" to "yaml",
        "yaml" to "yaml",
    )

// Longest-first so `javascript` wins over `java`.
private val VALID_FENCE_INFO = Regex("""[\w+#.-]+""")

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
 * Repairs malformed fenced-code markers in agent (and agent-like tool) markdown before parse or
 * plain-text streaming display.
 *
 * **Invariants**
 * 1. Idempotent on already-valid GFM fences — `normalize(normalize(x)) == normalize(x)`.
 * 2. Preserves non-fence prose verbatim except required line splits around fence markers.
 * 3. Safe on partial streaming input — never throws; auto-closes a trailing unclosed fence.
 *
 * **Streaming contract:** callers re-normalize the full accumulated buffer on each bind
 * (`AgentTextRowAdapter`); cursor glyph is appended outside this function.
 *
 * Accepts `\r\n` input; output uses `\n` line endings.
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
        afterFence.isEmpty() || afterFence.matches(VALID_FENCE_INFO) -> {
            result.add("$indent$FENCE_MARKER$afterFence")
            true
        }
        else -> {
            val split = splitCitationFenceInfo(afterFence) ?: splitOpeningFenceInfo(afterFence)
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

private fun splitCitationFenceInfo(info: String): Pair<String, String>? {
    val match = CITATION_FENCE_PREFIX.find(info) ?: return null
    val ext = match.groupValues[1].lowercase()
    val lang = CITATION_EXTENSION_TO_LANG[ext] ?: ext
    return lang to match.groupValues[2]
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
