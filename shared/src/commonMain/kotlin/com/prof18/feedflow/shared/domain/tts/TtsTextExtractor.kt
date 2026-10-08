package com.prof18.feedflow.shared.domain.tts

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Element
import com.fleeksoft.ksoup.nodes.Node
import com.fleeksoft.ksoup.nodes.TextNode
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

internal object TtsTextExtractor {
    private val blockTags = setOf(
        "p", "div", "section", "article", "li", "h1", "h2", "h3", "h4", "h5", "h6",
        "blockquote", "pre", "figure", "tr",
    )
    private val skippedTags = setOf(
        "script", "style", "iframe", "svg", "img", "picture", "video", "audio", "object", "embed",
        "canvas", "figcaption",
    )
    private val headingTags = setOf("h1", "h2")

    suspend fun extract(title: String?, content: String): List<String> {
        val context = currentCoroutineContext()
        context.ensureActive()
        val document = Ksoup.parse(content, "")
        context.ensureActive()
        val headingInfo = headingInfo(document, title, context)
        val bodyText = extractBodyText(document, headingInfo, context)
        val bodySegments = splitBlocks(bodyText, context)
        val titleText = title?.let { normalize(it, context) }.orEmpty()
        val result = mutableListOf<String>()
        if (titleText.isNotEmpty()) result.addAll(splitBlock(titleText, context))
        result.addAll(bodySegments)
        return result
    }

    private suspend fun headingInfo(
        root: Node,
        title: String?,
        context: kotlin.coroutines.CoroutineContext,
    ): HeadingInfo {
        if (title != null) return HeadingInfo(reference = title, firstHeading = null)
        val firstHeading = firstElementWithTag(root, "h1", context)
        val reference = firstHeading?.let { textContent(it, context) }
        return HeadingInfo(reference = reference, firstHeading = firstHeading)
    }

    private suspend fun firstElementWithTag(
        root: Node,
        tag: String,
        context: kotlin.coroutines.CoroutineContext,
    ): Element? {
        val stack = ArrayDeque<Node>()
        stack.addLast(root)
        var visits = 0
        var found: Element? = null
        while (stack.isNotEmpty() && found == null) {
            if (++visits % CHECK_INTERVAL == 0) context.ensureActive()
            val node = stack.removeLast()
            if (node is Element && node.normalName() == tag) {
                found = node
            } else {
                node.childNodes().asReversed().forEach(stack::addLast)
            }
        }
        return found
    }

    private suspend fun extractBodyText(
        root: Node,
        headingInfo: HeadingInfo,
        context: kotlin.coroutines.CoroutineContext,
    ): String {
        val output = StringBuilder()
        val stack = ArrayDeque<TraversalNode>()
        root.childNodes().asReversed().forEach { stack.addLast(TraversalNode(it)) }
        var visits = 0
        while (stack.isNotEmpty()) {
            if (++visits % CHECK_INTERVAL == 0) context.ensureActive()
            val traversalNode = stack.removeLast()
            if (traversalNode.isClosing) {
                output.append(SEGMENT_BREAK)
            } else {
                appendNode(traversalNode, stack, output, headingInfo, context)
            }
        }
        return output.toString()
    }

    private suspend fun appendNode(
        traversalNode: TraversalNode,
        stack: ArrayDeque<TraversalNode>,
        output: StringBuilder,
        headingInfo: HeadingInfo,
        context: kotlin.coroutines.CoroutineContext,
    ) {
        when (val node = traversalNode.node) {
            is TextNode -> if (traversalNode.isVisible) output.append(node.getWholeText())
            is Element -> appendElement(node, traversalNode.isVisible, stack, output, headingInfo, context)
        }
    }

    private suspend fun appendElement(
        element: Element,
        inheritedVisibility: Boolean,
        stack: ArrayDeque<TraversalNode>,
        output: StringBuilder,
        headingInfo: HeadingInfo,
        context: kotlin.coroutines.CoroutineContext,
    ) {
        val tag = element.normalName()
        if (isSkipped(element, tag, headingInfo, context)) return
        val inlineStyle = InlineSpeechStyle.parse(element.attr("style"))
        if (inlineStyle.display == "none") return
        val isVisible = when (inlineStyle.visibility) {
            "hidden", "collapse" -> false
            "visible", "initial" -> true
            else -> inheritedVisibility
        }
        if (tag == "br" || tag == "hr") {
            if (isVisible) output.append(SEGMENT_BREAK)
        } else {
            if (isVisible && (tag == "td" || tag == "th")) output.append(' ')
            val isBlock = tag in blockTags
            if (isBlock) output.append(SEGMENT_BREAK)
            if (isBlock) stack.addLast(TraversalNode(element, isClosing = true))
            element.childNodes().asReversed().forEach { stack.addLast(TraversalNode(it, isVisible = isVisible)) }
        }
    }

    private suspend fun isSkipped(
        element: Element,
        tag: String,
        headingInfo: HeadingInfo,
        context: kotlin.coroutines.CoroutineContext,
    ): Boolean {
        if (tag == "head" || tag in skippedTags) return true
        if (element.hasAttr("hidden")) return true
        if (element.attr("aria-hidden").equals("true", ignoreCase = true)) return true
        return isDuplicateHeading(element, tag, headingInfo, context)
    }

    private suspend fun isDuplicateHeading(
        element: Element,
        tag: String,
        headingInfo: HeadingInfo,
        context: kotlin.coroutines.CoroutineContext,
    ): Boolean {
        if (tag !in headingTags || element === headingInfo.firstHeading) return false
        val reference = headingInfo.reference ?: return false
        return reference.trim().lowercase() == textContent(element, context).trim().lowercase()
    }

    private suspend fun textContent(root: Node, context: kotlin.coroutines.CoroutineContext): String {
        val result = StringBuilder()
        val stack = ArrayDeque<Node>()
        stack.addLast(root)
        var visits = 0
        while (stack.isNotEmpty()) {
            if (++visits % CHECK_INTERVAL == 0) context.ensureActive()
            val node = stack.removeLast()
            if (node is TextNode) {
                result.append(node.getWholeText())
            } else {
                node.childNodes().asReversed().forEach(stack::addLast)
            }
        }
        return result.toString()
    }

    private suspend fun splitBlocks(source: String, context: kotlin.coroutines.CoroutineContext): List<String> {
        val result = mutableListOf<String>()
        for (part in source.split(SEGMENT_BREAK)) {
            context.ensureActive()
            val normalized = normalize(part, context)
            if (normalized.isNotEmpty()) result.addAll(splitBlock(normalized, context))
        }
        return result
    }

    private suspend fun splitBlock(text: String, context: kotlin.coroutines.CoroutineContext): List<String> {
        val chunks = mutableListOf<String>()
        var start = 0
        while (start < text.length) {
            context.ensureActive()
            val remaining = text.length - start
            if (remaining <= TtsLimits.MAX_SEGMENT_LENGTH) {
                chunks.add(text.substring(start).trim())
                break
            }
            val limit = start + TtsLimits.MAX_SEGMENT_LENGTH
            val boundary = findBoundary(text, start, limit)
            val safeBoundary = avoidSurrogateSplit(text, boundary)
            val chunk = text.substring(start, safeBoundary).trim()
            if (chunk.isNotEmpty()) chunks.add(chunk)
            start = skipWhitespace(text, safeBoundary)
        }
        return chunks
    }

    private fun findBoundary(text: String, start: Int, limit: Int): Int {
        val sentence = sentenceBoundary(text, start, limit)
        if (sentence != start) return sentence
        val whitespace = whitespaceBoundary(text, start, limit)
        return if (whitespace != start) whitespace else limit
    }

    private fun sentenceBoundary(text: String, start: Int, limit: Int): Int {
        var candidate = start
        for (index in start until limit) {
            if (isSentenceEnd(text, index)) candidate = index + 1
        }
        return candidate
    }

    private fun isSentenceEnd(text: String, index: Int): Boolean {
        val char = text[index]
        if (isCjkSentenceEnd(char)) return true
        if (!isAsciiSentenceEnd(char)) return false
        return index + 1 == text.length || text[index + 1].isWhitespace()
    }

    private fun isCjkSentenceEnd(char: Char): Boolean = char == '。' || char == '！' || char == '？'

    private fun isAsciiSentenceEnd(char: Char): Boolean = char == '.' || char == '!' || char == '?'

    private fun whitespaceBoundary(text: String, start: Int, limit: Int): Int {
        for (index in limit - 1 downTo start) {
            if (text[index].isWhitespace()) return index
        }
        return start
    }

    private fun avoidSurrogateSplit(text: String, boundary: Int): Int {
        val splitsSurrogatePair = boundary > 0 && boundary < text.length &&
            text[boundary - 1].isHighSurrogate() && text[boundary].isLowSurrogate()
        return if (splitsSurrogatePair) boundary - 1 else boundary
    }

    private fun skipWhitespace(text: String, start: Int): Int {
        var index = start
        while (index < text.length && text[index].isWhitespace()) index++
        return index
    }

    private suspend fun normalize(text: String, context: kotlin.coroutines.CoroutineContext): String {
        val result = StringBuilder(text.length)
        var pendingSpace = false
        for (index in text.indices) {
            if (index % CHECK_INTERVAL == 0) context.ensureActive()
            val char = text[index]
            if (char == '\u00a0' || char.isWhitespace()) {
                pendingSpace = result.isNotEmpty()
            } else {
                if (pendingSpace) result.append(' ')
                result.append(char)
                pendingSpace = false
            }
        }
        return result.toString()
    }

    private data class HeadingInfo(val reference: String?, val firstHeading: Element?)

    private data class TraversalNode(val node: Node, val isClosing: Boolean = false, val isVisible: Boolean = true)

    private const val CHECK_INTERVAL = 128
    private const val SEGMENT_BREAK = '\u0000'
}

private data class InlineSpeechStyle(val display: String?, val visibility: String?) {
    companion object {
        private val importantSuffix = Regex("\\s*!\\s*important\\s*$", RegexOption.IGNORE_CASE)
        private val displayValues = setOf(
            "none", "block", "inline", "inline-block", "flow-root", "list-item", "flex", "inline-flex",
            "grid", "inline-grid", "table", "inline-table", "table-caption", "table-row", "table-cell",
            "table-row-group", "table-header-group", "table-footer-group", "table-column", "table-column-group",
            "ruby", "ruby-base", "ruby-text", "contents", "initial", "inherit", "unset", "revert", "revert-layer",
        )
        private val outerDisplayValues = setOf("block", "inline")
        private val innerDisplayValues = setOf("flow", "flow-root", "flex", "grid", "table", "ruby")
        private val visibilityValues = setOf(
            "visible",
            "hidden",
            "collapse",
            "initial",
            "inherit",
            "unset",
            "revert",
            "revert-layer",
        )

        fun parse(style: String): InlineSpeechStyle {
            var display: Declaration? = null
            var visibility: Declaration? = null
            for (part in declarations(style)) {
                val (property, declaration) = readDeclaration(part) ?: continue
                when (property) {
                    "display" -> if (isDisplayValue(declaration.value) &&
                        (display?.important != true || declaration.important)
                    ) {
                        display = declaration
                    }
                    "visibility" -> if (declaration.value in visibilityValues &&
                        (visibility?.important != true || declaration.important)
                    ) {
                        visibility = declaration
                    }
                }
            }
            return InlineSpeechStyle(display?.value, visibility?.value)
        }

        private fun readDeclaration(part: String): Pair<String, Declaration>? {
            val colon = part.indexOf(':')
            if (colon < 0) return null
            val property = part.substring(0, colon).trim().lowercase()
            val value = part.substring(colon + 1).trim()
            val important = importantSuffix.containsMatchIn(value)
            return property to Declaration(value.replace(importantSuffix, "").trim().lowercase(), important)
        }

        private fun isDisplayValue(value: String): Boolean {
            if (value in displayValues) return true
            val tokens = value.split(Regex("\\s+"))
            return tokens.size == 2 && tokens[0] in outerDisplayValues && tokens[1] in innerDisplayValues
        }

        private fun declarations(style: String): List<String> {
            val scanner = DeclarationScanner()
            var index = 0
            while (index < style.length) {
                index = scanner.consume(style, index)
            }
            return scanner.finish()
        }
    }

    private data class Declaration(val value: String, val important: Boolean)

    private class DeclarationScanner {
        private val result = mutableListOf<String>()
        private val part = StringBuilder()
        private var quote: Char? = null
        private var depth = 0
        private var inComment = false

        fun consume(style: String, index: Int): Int = when {
            inComment -> consumeComment(style, index)
            quote != null -> consumeQuoted(style, index)
            else -> consumePlain(style, index)
        }

        fun finish(): List<String> {
            result.add(part.toString())
            return result
        }

        private fun consumeComment(style: String, index: Int): Int {
            if (style[index] == '*' && style.getOrNull(index + 1) == '/') {
                inComment = false
                return index + 2
            }
            return index + 1
        }

        private fun consumeQuoted(style: String, index: Int): Int {
            val char = style[index]
            part.append(char)
            if (char == '\\' && index + 1 < style.length) {
                part.append(style[index + 1])
                return index + 2
            }
            if (char == quote) quote = null
            return index + 1
        }

        private fun consumePlain(style: String, index: Int): Int {
            val char = style[index]
            if (char == '/' && style.getOrNull(index + 1) == '*') {
                inComment = true
                return index + 2
            }
            when (char) {
                '"', '\'' -> quote = char
                '(' -> depth++
                ')' -> depth = (depth - 1).coerceAtLeast(0)
                ';' -> if (depth == 0) {
                    result.add(part.toString())
                    part.clear()
                    return index + 1
                }
            }
            part.append(char)
            return index + 1
        }
    }
}
