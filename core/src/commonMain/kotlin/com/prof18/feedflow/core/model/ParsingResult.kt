package com.prof18.feedflow.core.model

/**
 * Result of feed item content parsing operation.
 *
 * Note: FeedFlow does NOT store parsed metadata in the database.
 * We trust the RSS feed metadata. Only the HTML content is cached to files.
 */
sealed class ParsingResult {
    /**
     * Parsing succeeded.
     *
     * @property htmlContent Cleaned HTML content (THIS is what we cache)
     * @property title Article title (used for UI display)
     * @property siteName Source domain (kept for potential future use)
     */
    data class Success(
        val htmlContent: String?,
        val title: String?,
        val siteName: String?,
    ) : ParsingResult()

    /**
     * Parsing failed due to network, parsing, or other error.
     */
    data object Error : ParsingResult()
}
