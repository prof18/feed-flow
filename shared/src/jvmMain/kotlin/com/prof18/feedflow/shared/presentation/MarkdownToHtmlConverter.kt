package com.prof18.feedflow.shared.presentation

import com.prof18.feedflow.core.utils.DispatcherProvider
import com.vladsch.flexmark.html2md.converter.FlexmarkHtmlConverter
import kotlinx.coroutines.withContext

internal class MarkdownToHtmlConverter(
    private val converter: FlexmarkHtmlConverter,
    private val dispatcherProvider: DispatcherProvider,
) {

    // Matches linked images: [![alt](img-url)](link-url)
    private val linkedImagePattern = Regex("""\[!\[[^\]]*]\([^)]+\)]\([^)]+\)""")

    // Matches standalone images: ![alt](url)
    private val standaloneImagePattern = Regex("""!\[[^\]]*]\([^)]+\)""")

    suspend fun convertToMarkdown(html: String): String = withContext(dispatcherProvider.io) {
        val markdown = converter.convert(html)
        return@withContext ensureImagesAreBlockLevel(markdown)
    }

    private fun ensureImagesAreBlockLevel(markdown: String): String {
        // First handle linked images as a unit
        var result = linkedImagePattern.replace(markdown) { match ->
            "\n\n${match.value}\n\n"
        }
        // Then handle standalone images (that aren't part of a link)
        // We need to check that the image isn't preceded by '[' which would indicate it's inside a link
        result = standaloneImagePattern.replace(result) { match ->
            val startIndex = match.range.first
            val charBefore = if (startIndex > 0) result[startIndex - 1] else ' '
            if (charBefore == '[') {
                // This image is inside a link, don't modify
                match.value
            } else {
                "\n\n${match.value}\n\n"
            }
        }
        return result.replace(Regex("\n{3,}"), "\n\n")
    }
}
