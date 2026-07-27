package com.prof18.feedflow.core.model

data class FeedItemUrlInfo(
    val id: String,
    val url: String,
    val title: String?,
    val openOnlyOnBrowser: Boolean = false,
    val isBookmarked: Boolean,
    val articleOpenMode: ArticleOpenMode,
    val commentsUrl: String?,
    val imageUrl: String? = null,
    val feedSourceTitle: String? = null,
    val feedSourceBaseUrl: String? = null,
)

// Whether the item's web page can be parsed for reader mode. URL-less items are not eligible here,
// but they can still be shown in the reader from their feed content: see [hasNoUrl].
fun FeedItemUrlInfo.canOpenWebReaderMode(): Boolean =
    !openOnlyOnBrowser && ReaderModeEligibility.canOpenReaderMode(url)

// URL-less items can only be shown from their feed-provided content.
fun FeedItemUrlInfo.hasNoUrl(): Boolean = url.isBlank()

/**
 * The mode this item actually opens with: the per-feed value falls back to [globalDefault], and a
 * reader mode falls back to the favourite browser when the page cannot be parsed. Never returns
 * [ArticleOpenMode.DEFAULT].
 */
fun FeedItemUrlInfo.resolveArticleOpenMode(globalDefault: ArticleOpenMode): ArticleOpenMode =
    ArticleOpenModeResolver.resolve(this, globalDefault)

// Object wrapper so iOS can call the same resolution: top-level functions are awkward to reach
// from Swift, while objects are exported as `ArticleOpenModeResolver.shared`.
object ArticleOpenModeResolver {
    fun resolve(urlInfo: FeedItemUrlInfo, globalDefault: ArticleOpenMode): ArticleOpenMode {
        if (urlInfo.hasNoUrl()) return ArticleOpenMode.FEED_CONTENT
        val resolved = urlInfo.articleOpenMode.resolveWith(globalDefault)
        return when {
            !resolved.isReaderMode() -> resolved
            urlInfo.canOpenWebReaderMode() -> resolved
            else -> ArticleOpenMode.PREFERRED_BROWSER
        }
    }
}
