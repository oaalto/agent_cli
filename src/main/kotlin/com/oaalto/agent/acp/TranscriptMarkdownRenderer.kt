package com.oaalto.agent.acp

import org.intellij.markdown.IElementType
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.ast.getTextInNode
import org.intellij.markdown.flavours.gfm.GFMElementTypes
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.flavours.gfm.GFMTokenTypes
import org.intellij.markdown.parser.MarkdownParser

/**
 * One rendered block in a Markdown-parsed agent text or tool card body.
 */
internal sealed class RenderedBlock {
    data class InlineText(
        val text: String,
        val runs: List<StyledRun>,
        val headingLevel: Int = 0,
        val listMarker: String? = null,
    ) : RenderedBlock()

    data class CodeBlock(
        val languageId: String?,
        val code: String,
    ) : RenderedBlock()

    data class Table(
        val headers: List<String>,
        val rows: List<List<String>>,
        val alignments: List<TableAlignment>,
    ) : RenderedBlock()

    data class Image(
        val altText: String,
        val url: String,
    ) : RenderedBlock()

    data object ThematicBreak : RenderedBlock()

    data class BlockQuote(
        val blocks: List<RenderedBlock>,
    ) : RenderedBlock()

    data class CustomHtml(
        val html: String,
    ) : RenderedBlock()
}

internal enum class TableAlignment { LEFT, CENTER, RIGHT }

internal enum class TextStyle {
    BOLD,
    ITALIC,
    BOLD_ITALIC,
    CODE,
    LINK,
    STRIKETHROUGH,
}

internal data class StyledRun(
    val start: Int,
    val end: Int,
    val style: TextStyle,
    val url: String? = null,
)

/**
 * Pure-Kotlin Markdown AST-to-[RenderedBlock] mapper.
 *
 * Uses [org.intellij.markdown] with GFM flavour for tables, task lists,
 * strikethrough, and fenced code blocks.
 */
internal object TranscriptMarkdownRenderer {
    private val flavour = GFMFlavourDescriptor()
    private val parser = MarkdownParser(flavour)

    fun parseToBlocks(input: String): List<RenderedBlock> {
        if (input.isBlank()) return emptyList()
        return try {
            val normalized = normalizeAgentFences(input)
            val ast = parser.buildMarkdownTreeFromString(normalized)
            val blocks = mutableListOf<RenderedBlock>()
            walkChildren(ast, normalized, blocks)
            if (blocks.isEmpty() && normalized.isNotBlank()) {
                listOf(RenderedBlock.InlineText(input, emptyList()))
            } else {
                blocks
            }
        } catch (_: Exception) {
            listOf(RenderedBlock.InlineText(input, emptyList()))
        }
    }

    private val markdownHints = setOf('*', '_', '`', '#', '[', '-', '>', '|')

    fun likelyContainsMarkdown(input: String): Boolean {
        if (input.length < MARKDOWN_HINT_MIN_LENGTH) return false
        // Check for inline Markdown characters and block patterns
        return input.any { it in markdownHints } ||
            input.contains("```") ||
            input.contains("---") ||
            // List items: - item or 1. item at line start
            input.contains(Regex("""(?m)^\s*[-*+]\s+""")) ||
            input.contains(Regex("""(?m)^\s*\d+\.\s+""")) ||
            // Blockquote: > text at line start
            input.contains(Regex("""(?m)^\s*>\s+""")) ||
            // Table: | col | col |
            input.contains(Regex("""(?m)^\s*\|[^|]+\|"""))
    }

    private const val MARKDOWN_HINT_MIN_LENGTH = 3

    // -----------------------------------------------------------------------
    // Block walker
    // -----------------------------------------------------------------------

    private fun walkChildren(
        node: ASTNode,
        source: String,
        out: MutableList<RenderedBlock>,
    ) {
        for (child in node.children) {
            BlockHandlers.dispatch(child, source, out)
        }
    }

    private fun walkListItems(
        listNode: ASTNode,
        source: String,
        out: MutableList<RenderedBlock>,
        ordered: Boolean,
    ) {
        var itemIndex = 1
        for (child in listNode.children) {
            if (child.type !== MarkdownElementTypes.LIST_ITEM) continue
            val marker = if (ordered) "$itemIndex. " else "\u2022 "
            itemIndex++
            val itemBlocks = mutableListOf<RenderedBlock>()
            walkChildren(child, source, itemBlocks)
            addListItemToOutput(marker, itemBlocks, out)
        }
    }

    private fun addListItemToOutput(
        marker: String,
        itemBlocks: List<RenderedBlock>,
        out: MutableList<RenderedBlock>,
    ) {
        if (itemBlocks.isEmpty()) {
            out.add(RenderedBlock.InlineText(marker, emptyList()))
            return
        }
        val first = itemBlocks.first()
        if (first is RenderedBlock.InlineText) {
            out.add(first.copy(listMarker = marker))
        } else {
            out.add(first)
        }
        for (extra in itemBlocks.drop(1)) {
            out.add(extra)
        }
    }

    // -----------------------------------------------------------------------
    // Table extraction (GFM)
    // -----------------------------------------------------------------------

    private fun isTableNode(node: ASTNode): Boolean =
        node.type === GFMElementTypes.TABLE ||
            node.type === GFMElementTypes.HEADER ||
            node.type === GFMElementTypes.ROW

    private fun extractTable(
        tableNode: ASTNode,
        source: String,
    ): RenderedBlock.Table? {
        var headerCells: List<String>? = null
        val bodyRows = mutableListOf<List<String>>()
        var alignments = emptyList<TableAlignment>()

        for (child in tableNode.children) {
            when {
                child.type === GFMElementTypes.HEADER ->
                    headerCells = BlockHandlers.extractRowCells(child, source)
                child.type === GFMElementTypes.ROW ->
                    bodyRows.add(BlockHandlers.extractRowCells(child, source))
                child.type === GFMTokenTypes.TABLE_SEPARATOR ->
                    alignments = BlockHandlers.extractAlignmentsFromDelimiter(child, source)
            }
        }

        val headers = headerCells ?: return null
        if (headers.isEmpty() && bodyRows.isEmpty()) return null
        return RenderedBlock.Table(headers, bodyRows, alignments)
    }

    // -----------------------------------------------------------------------
    // Inline entry points
    // -----------------------------------------------------------------------

    private data class InlineResult(
        val text: String,
        val runs: List<StyledRun>,
    )

    private fun tryExtractImageFromParagraph(
        paraNode: ASTNode,
        source: String,
    ): RenderedBlock.Image? {
        for (child in paraNode.children) {
            if (child.type === MarkdownElementTypes.IMAGE) {
                return TextExtractor.imageFromNode(child, source)
            }
        }
        return null
    }

    private fun extractInlineContent(
        node: ASTNode,
        source: String,
    ): InlineResult = InlineEngine.extract(node, source)

    // =========================================================================
    // Block handlers
    // =========================================================================

    private object BlockHandlers {
        private val atxLevelMap: Map<IElementType, Int> =
            mapOf(
                MarkdownElementTypes.ATX_1 to 1,
                MarkdownElementTypes.ATX_2 to 2,
                MarkdownElementTypes.ATX_3 to 3,
                MarkdownElementTypes.ATX_4 to 4,
                MarkdownElementTypes.ATX_5 to 5,
                MarkdownElementTypes.ATX_6 to 6,
            )

        private val blockLevelTypes: Set<IElementType> =
            setOf(
                MarkdownElementTypes.PARAGRAPH,
                GFMElementTypes.HEADER,
                MarkdownElementTypes.CODE_FENCE,
                MarkdownElementTypes.CODE_BLOCK,
                MarkdownElementTypes.ORDERED_LIST,
                MarkdownElementTypes.UNORDERED_LIST,
                GFMElementTypes.TABLE,
                MarkdownElementTypes.BLOCK_QUOTE,
                MarkdownTokenTypes.HORIZONTAL_RULE,
                MarkdownElementTypes.HTML_BLOCK,
                MarkdownElementTypes.IMAGE,
            )

        fun dispatch(
            child: ASTNode,
            source: String,
            out: MutableList<RenderedBlock>,
        ) {
            when (child.type) {
                MarkdownElementTypes.PARAGRAPH -> handleParagraph(child, source, out)
                MarkdownElementTypes.ATX_1,
                MarkdownElementTypes.ATX_2,
                MarkdownElementTypes.ATX_3,
                MarkdownElementTypes.ATX_4,
                MarkdownElementTypes.ATX_5,
                MarkdownElementTypes.ATX_6,
                -> handleHeading(child, source, out)
                MarkdownElementTypes.CODE_FENCE,
                MarkdownElementTypes.CODE_BLOCK,
                -> handleCodeBlock(child, source, out)
                MarkdownElementTypes.ORDERED_LIST,
                MarkdownElementTypes.UNORDERED_LIST,
                -> handleList(child, source, out)
                MarkdownElementTypes.BLOCK_QUOTE -> handleBlockQuote(child, source, out)
                MarkdownElementTypes.IMAGE -> handleImage(child, source, out)
                else -> handleElse(child, source, out)
            }
        }

        private fun handleParagraph(
            child: ASTNode,
            source: String,
            out: MutableList<RenderedBlock>,
        ) {
            val imageBlock = tryExtractImageFromParagraph(child, source)
            if (imageBlock != null) {
                out.add(imageBlock)
            } else {
                val (text, runs) = extractInlineContent(child, source)
                out.add(RenderedBlock.InlineText(text, runs))
            }
        }

        private fun handleHeading(
            child: ASTNode,
            source: String,
            out: MutableList<RenderedBlock>,
        ) {
            val level = atxLevelMap[child.type] ?: 1
            val (text, runs) = extractInlineContent(child, source)
            out.add(RenderedBlock.InlineText(text, runs, headingLevel = level))
        }

        private fun handleCodeBlock(
            child: ASTNode,
            source: String,
            out: MutableList<RenderedBlock>,
        ) {
            val codeText = TextExtractor.extractCodeText(child, source)
            val lang =
                if (child.type === MarkdownElementTypes.CODE_FENCE) {
                    TextExtractor.resolveLanguage(child, source)
                } else {
                    null
                }
            out.add(RenderedBlock.CodeBlock(lang, codeText))
        }

        private fun handleList(
            child: ASTNode,
            source: String,
            out: MutableList<RenderedBlock>,
        ) {
            val ordered = child.type === MarkdownElementTypes.ORDERED_LIST
            walkListItems(child, source, out, ordered)
        }

        private fun handleBlockQuote(
            child: ASTNode,
            source: String,
            out: MutableList<RenderedBlock>,
        ) {
            val inner = mutableListOf<RenderedBlock>()
            walkChildren(child, source, inner)
            if (inner.isNotEmpty()) {
                out.add(RenderedBlock.BlockQuote(inner))
            }
        }

        private fun handleImage(
            child: ASTNode,
            source: String,
            out: MutableList<RenderedBlock>,
        ) {
            out.add(TextExtractor.imageFromNode(child, source))
        }

        private fun handleElse(
            child: ASTNode,
            source: String,
            out: MutableList<RenderedBlock>,
        ) {
            if (child.type === MarkdownTokenTypes.HORIZONTAL_RULE) {
                out.add(RenderedBlock.ThematicBreak)
            } else if (isTableNode(child)) {
                val table = extractTable(child, source)
                if (table != null) out.add(table)
            } else if (child.type in blockLevelTypes) {
                val text = child.getTextInNode(source).toString().trim()
                if (text.isNotEmpty()) {
                    out.add(RenderedBlock.InlineText(text, emptyList()))
                }
            }
        }

        fun extractRowCells(
            rowNode: ASTNode,
            source: String,
        ): List<String> =
            rowNode.children
                .filter { it.type === GFMTokenTypes.CELL }
                .map { it.getTextInNode(source).toString().trim() }

        fun extractAlignmentsFromDelimiter(
            delimNode: ASTNode,
            source: String,
        ): List<TableAlignment> =
            delimNode.children
                .filter { it.type === GFMTokenTypes.CELL }
                .map { cellNode ->
                    val text = cellNode.getTextInNode(source).toString().trim()
                    when {
                        text.startsWith(":") && text.endsWith(":") -> TableAlignment.CENTER
                        text.endsWith(":") -> TableAlignment.RIGHT
                        text.startsWith(":") -> TableAlignment.LEFT
                        else -> TableAlignment.LEFT
                    }
                }
    }

    // =========================================================================
    // Inline engine
    // =========================================================================

    private data class InlineCtx(
        val sb: StringBuilder,
        val outRuns: MutableList<StyledRun>,
    )

    private val styleContainerTypes: Map<IElementType, TextStyle> =
        mapOf(
            MarkdownElementTypes.STRONG to TextStyle.BOLD,
            MarkdownElementTypes.EMPH to TextStyle.ITALIC,
            GFMElementTypes.STRIKETHROUGH to TextStyle.STRIKETHROUGH,
        )

    private object InlineEngine {
        fun extract(
            node: ASTNode,
            source: String,
        ): InlineResult {
            val ctx = InlineCtx(StringBuilder(), mutableListOf())
            processAll(node, source, ctx, emptyList())
            return InlineResult(ctx.sb.toString(), ctx.outRuns)
        }

        private fun processAll(
            node: ASTNode,
            source: String,
            ctx: InlineCtx,
            activeStyles: List<Pair<TextStyle, String?>>,
        ) {
            for (child in node.children) {
                dispatch(child, source, ctx, activeStyles)
            }
        }

        private fun dispatch(
            child: ASTNode,
            source: String,
            ctx: InlineCtx,
            activeStyles: List<Pair<TextStyle, String?>>,
        ) {
            when (child.type) {
                MarkdownTokenTypes.TEXT,
                MarkdownTokenTypes.EOL,
                MarkdownTokenTypes.WHITE_SPACE,
                -> handleTextual(child, source, ctx, activeStyles)
                MarkdownElementTypes.CODE_SPAN -> handleCodeSpan(child, source, ctx)
                MarkdownElementTypes.IMAGE -> handleInlineImage(child, source, ctx)
                MarkdownElementTypes.INLINE_LINK -> handleInlineLink(child, source, ctx)
                MarkdownElementTypes.AUTOLINK -> {
                    val url = child.getTextInNode(source).toString()
                    val start = ctx.sb.length
                    ctx.sb.append(url)
                    ctx.outRuns.add(StyledRun(start, ctx.sb.length, TextStyle.LINK, url))
                }
                // Skip delimiter / syntax tokens
                MarkdownTokenTypes.EMPH,
                MarkdownTokenTypes.BACKTICK,
                MarkdownTokenTypes.ATX_HEADER,
                GFMTokenTypes.TILDE,
                -> {}
                else -> handleFallsThrough(child, source, ctx, activeStyles)
            }
        }

        private fun handleFallsThrough(
            child: ASTNode,
            source: String,
            ctx: InlineCtx,
            activeStyles: List<Pair<TextStyle, String?>>,
        ) {
            val style = styleContainerTypes[child.type]
            if (style != null) {
                processStyle(child, source, ctx, activeStyles, style)
            } else {
                handleUnknown(child, source, ctx, activeStyles)
            }
        }

        private fun handleTextual(
            child: ASTNode,
            source: String,
            ctx: InlineCtx,
            activeStyles: List<Pair<TextStyle, String?>>,
        ) {
            val text = child.getTextInNode(source).toString()
            val start = ctx.sb.length
            ctx.sb.append(text)
            InlineEngine.addStyledRun(ctx, activeStyles, start)
        }

        private fun processStyle(
            child: ASTNode,
            source: String,
            ctx: InlineCtx,
            activeStyles: List<Pair<TextStyle, String?>>,
            style: TextStyle,
        ) {
            val nested = activeStyles + (style to null)
            processAll(child, source, ctx, nested)
        }

        private fun handleCodeSpan(
            child: ASTNode,
            source: String,
            ctx: InlineCtx,
        ) {
            val code = TextExtractor.extractCodeSpanText(child, source)
            val start = ctx.sb.length
            ctx.sb.append(code)
            ctx.outRuns.add(StyledRun(start, ctx.sb.length, TextStyle.CODE))
        }

        private fun handleInlineImage(
            child: ASTNode,
            source: String,
            ctx: InlineCtx,
        ) {
            val alt = TextExtractor.extractImageAlt(child, source)
            val url = TextExtractor.extractImageUrl(child, source)
            val placeholder = "[image: $alt]"
            val start = ctx.sb.length
            ctx.sb.append(placeholder)
            ctx.outRuns.add(StyledRun(start, ctx.sb.length, TextStyle.LINK, url))
        }

        private fun handleInlineLink(
            child: ASTNode,
            source: String,
            ctx: InlineCtx,
        ) {
            val linkText = TextExtractor.extractLinkText(child, source)
            val url = TextExtractor.resolveLinkUrl(child, source)
            val start = ctx.sb.length
            ctx.sb.append(linkText)
            ctx.outRuns.add(StyledRun(start, ctx.sb.length, TextStyle.LINK, url))
        }

        private fun handleUnknown(
            child: ASTNode,
            source: String,
            ctx: InlineCtx,
            activeStyles: List<Pair<TextStyle, String?>>,
        ) {
            val text = child.getTextInNode(source).toString()
            if (text.isNotBlank()) {
                val start = ctx.sb.length
                ctx.sb.append(text)
                InlineEngine.addStyledRun(ctx, activeStyles, start)
            }
        }
    }

    private fun InlineEngine.addStyledRun(
        ctx: InlineCtx,
        activeStyles: List<Pair<TextStyle, String?>>,
        start: Int,
    ) {
        if (activeStyles.isNotEmpty()) {
            val combined = combineStyles(activeStyles)
            ctx.outRuns.add(StyledRun(start, ctx.sb.length, combined.first, combined.second))
        }
    }

    private fun combineStyles(activeStyles: List<Pair<TextStyle, String?>>): Pair<TextStyle, String?> {
        val styleSet = activeStyles.map { it.first }.toSet()
        val url = activeStyles.firstOrNull { it.first == TextStyle.LINK }?.second
        val style =
            when {
                styleSet.contains(TextStyle.BOLD) && styleSet.contains(TextStyle.ITALIC) ->
                    TextStyle.BOLD_ITALIC
                styleSet.contains(TextStyle.BOLD) -> TextStyle.BOLD
                styleSet.contains(TextStyle.ITALIC) -> TextStyle.ITALIC
                styleSet.contains(TextStyle.STRIKETHROUGH) -> TextStyle.STRIKETHROUGH
                styleSet.contains(TextStyle.CODE) -> TextStyle.CODE
                styleSet.contains(TextStyle.LINK) -> TextStyle.LINK
                else -> TextStyle.ITALIC // Fallback for unexpected cases - italic is less intrusive than bold
            }
        return style to url
    }

    // =========================================================================
    // Text extraction helpers
    // =========================================================================

    private object TextExtractor {
        fun extractLinkText(
            linkNode: ASTNode,
            source: String,
        ): String {
            for (child in linkNode.children) {
                if (child.type === MarkdownElementTypes.LINK_TEXT) {
                    val text = extractFromLinkText(child, source)
                    if (text.isNotEmpty()) return text
                }
            }
            return extractFallbackText(linkNode, source)
        }

        private fun extractFromLinkText(
            linkTextNode: ASTNode,
            source: String,
        ): String {
            val sb = StringBuilder()
            for (inner in linkTextNode.children) {
                if (inner.type === MarkdownTokenTypes.TEXT) {
                    sb.append(inner.getTextInNode(source))
                }
            }
            if (sb.isNotEmpty()) return sb.toString()
            val full = linkTextNode.getTextInNode(source).toString()
            return full.removeSurrounding("[", "]")
        }

        private fun extractFallbackText(
            linkNode: ASTNode,
            source: String,
        ): String {
            val sb = StringBuilder()
            for (child in linkNode.children) {
                if (child.type === MarkdownTokenTypes.TEXT) {
                    sb.append(child.getTextInNode(source))
                }
            }
            return sb.toString().trim()
        }

        fun resolveLinkUrl(
            linkNode: ASTNode,
            source: String,
        ): String? {
            for (child in linkNode.children) {
                if (child.type === MarkdownElementTypes.LINK_DESTINATION) {
                    return child.getTextInNode(source).toString().trim()
                }
            }
            return null
        }

        fun extractImageAlt(
            imageNode: ASTNode,
            source: String,
        ): String {
            for (child in imageNode.children) {
                if (child.type === MarkdownElementTypes.INLINE_LINK) {
                    return extractLinkText(child, source)
                }
            }
            return "image"
        }

        fun extractImageUrl(
            imageNode: ASTNode,
            source: String,
        ): String {
            for (child in imageNode.children) {
                if (child.type === MarkdownElementTypes.INLINE_LINK) {
                    return resolveLinkUrl(child, source).orEmpty()
                }
            }
            return ""
        }

        fun imageFromNode(
            imageNode: ASTNode,
            source: String,
        ): RenderedBlock.Image =
            RenderedBlock.Image(
                altText = extractImageAlt(imageNode, source),
                url = extractImageUrl(imageNode, source),
            )

        fun extractCodeSpanText(
            spanNode: ASTNode,
            source: String,
        ): String {
            val sb = StringBuilder()
            for (child in spanNode.children) {
                if (child.type === MarkdownTokenTypes.TEXT) {
                    sb.append(child.getTextInNode(source))
                }
            }
            return sb.toString()
        }

        fun extractCodeText(
            codeNode: ASTNode,
            source: String,
        ): String {
            val textParts = mutableListOf<String>()
            for (child in codeNode.children) {
                if (child.type === MarkdownTokenTypes.CODE_FENCE_CONTENT ||
                    child.type === MarkdownTokenTypes.CODE_LINE
                ) {
                    textParts.add(child.getTextInNode(source).toString())
                }
            }
            return joinCodeFenceParts(textParts)
        }

        fun joinCodeFenceParts(parts: List<String>): String {
            if (parts.isEmpty()) return ""
            val joined = parts.joinToString("")
            if (joined.contains('\n')) return joined.trimEnd('\n')
            return parts.joinToString("\n").trimEnd('\n')
        }

        fun resolveLanguage(
            codeNode: ASTNode,
            source: String,
        ): String? {
            for (child in codeNode.children) {
                if (child.type === MarkdownTokenTypes.FENCE_LANG) {
                    val lang = child.getTextInNode(source).toString().trim()
                    return lang.ifEmpty { null }
                }
            }
            return null
        }
    }
}
