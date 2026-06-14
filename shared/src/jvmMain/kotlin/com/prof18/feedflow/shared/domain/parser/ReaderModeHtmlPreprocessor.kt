package com.prof18.feedflow.shared.domain.parser

internal object ReaderModeHtmlPreprocessor {

    fun prepare(html: String): PreparedHtml {
        val strippedHtml = BLOCK_TAGS.fold(html) { currentHtml, tagRule ->
            removeElementBlocks(currentHtml, tagRule)
        }
        val cappedHtml = if (strippedHtml.length > MAX_READER_HTML_CHARS) {
            strippedHtml.take(MAX_READER_HTML_CHARS)
        } else {
            strippedHtml
        }

        return PreparedHtml(
            html = cappedHtml,
            removedChars = html.length - strippedHtml.length,
            truncated = cappedHtml.length != strippedHtml.length,
        )
    }

    private fun removeElementBlocks(html: String, tagRule: BlockTagRule): String {
        var searchIndex = 0
        var copyFrom = 0
        var result: StringBuilder? = null

        while (searchIndex < html.length) {
            val removal = findRemovalRange(html, tagRule, searchIndex)
            if (removal == null) {
                searchIndex = html.length
            } else {
                if (result == null) {
                    result = StringBuilder(html.length)
                }
                result.append(html, copyFrom, removal.start)
                copyFrom = removal.end
                searchIndex = removal.end
            }
        }

        return result?.apply {
            append(html, copyFrom, html.length)
        }?.toString() ?: html
    }

    private fun findRemovalRange(html: String, tagRule: BlockTagRule, searchIndex: Int): RemovalRange? {
        val start = findOpeningTag(html, tagRule.name, searchIndex) ?: return null
        val openEnd = html.indexOf('>', start)
        if (openEnd == -1) return null
        if (!tagRule.shouldRemove(html.substring(start, openEnd + 1))) {
            return RemovalRange(start = openEnd + 1, end = openEnd + 1)
        }

        val closeStart = html.indexOf("</${tagRule.name}", startIndex = openEnd + 1, ignoreCase = true)
        val end = if (closeStart >= 0) {
            val closeEnd = html.indexOf('>', closeStart)
            if (closeEnd >= 0) closeEnd + 1 else html.length
        } else {
            openEnd + 1
        }
        return RemovalRange(start, end)
    }

    private fun findOpeningTag(html: String, tag: String, startIndex: Int): Int? {
        var index = html.indexOf("<$tag", startIndex = startIndex, ignoreCase = true)
        while (index >= 0) {
            val boundaryIndex = index + tag.length + TAG_START_LENGTH
            if (boundaryIndex >= html.length || html[boundaryIndex].isTagBoundary()) {
                return index
            }
            index = html.indexOf("<$tag", startIndex = index + 1, ignoreCase = true)
        }
        return null
    }

    private fun Char.isTagBoundary(): Boolean =
        this == '>' || this == '/' || isWhitespace()

    data class PreparedHtml(
        val html: String,
        val removedChars: Int,
        val truncated: Boolean,
    )

    private data class RemovalRange(
        val start: Int,
        val end: Int,
    )

    private data class BlockTagRule(
        val name: String,
        val shouldRemove: (openingTag: String) -> Boolean = { true },
    )

    private fun shouldRemoveScript(openingTag: String): Boolean {
        val type = TYPE_ATTRIBUTE_REGEX.find(openingTag)
            ?.groupValues
            ?.get(TYPE_ATTRIBUTE_VALUE_GROUP)
            ?.substringBefore(';')
            ?.lowercase()
            ?: return true

        return type != JSON_LD_SCRIPT_TYPE && !type.startsWith(MATH_SCRIPT_TYPE_PREFIX)
    }

    private const val TAG_START_LENGTH = 1
    private const val MAX_READER_HTML_CHARS = 5 * 1024 * 1024
    private const val TYPE_ATTRIBUTE_VALUE_GROUP = 1
    private const val JSON_LD_SCRIPT_TYPE = "application/ld+json"
    private const val MATH_SCRIPT_TYPE_PREFIX = "math/"

    private val BLOCK_TAGS = listOf(
        BlockTagRule("script", ::shouldRemoveScript),
        BlockTagRule("style"),
        BlockTagRule("frame"),
        BlockTagRule("frameset"),
        BlockTagRule("noscript"),
        BlockTagRule("canvas"),
        BlockTagRule("object"),
        BlockTagRule("embed"),
        BlockTagRule("applet"),
        BlockTagRule("base"),
    )

    private val TYPE_ATTRIBUTE_REGEX = Regex("""\btype\s*=\s*["']?([^"'\s>]+)""", RegexOption.IGNORE_CASE)
}
